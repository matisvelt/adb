package com.antlab.rigcontrol;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceManager {
    private final Settings settings;
    private final ADBService adbService;
    private final Logger logger;

    private final ObservableList<DeviceInfo> devices = FXCollections.observableArrayList();
    private final Map<String, DeviceInfo> registry = new ConcurrentHashMap<>();
    private final Map<String, Instant> pingBackoffUntil = new ConcurrentHashMap<>();
    private final Map<String, Integer> pingBackoffMultiplier = new ConcurrentHashMap<>();
    private final Set<String> expectedSerials = ConcurrentHashMap.newKeySet();

    private final StringProperty adbHealth = new SimpleStringProperty("ADB OK");
    private final StringProperty lastScanDuration = new SimpleStringProperty("-");
    private final StringProperty lastError = new SimpleStringProperty("");
    private final StringProperty pingBatchDuration = new SimpleStringProperty("-");
    private final StringProperty pingBatchCount = new SimpleStringProperty("0");
    private final StringProperty expectedMissing = new SimpleStringProperty("0");
    private final AtomicLong totalPolls = new AtomicLong(0);
    private final AtomicLong pingOk = new AtomicLong(0);
    private final AtomicLong pingFail = new AtomicLong(0);

    private final AtomicInteger adbTokens = new AtomicInteger(0);
    private volatile long adbTokenWindowStart = System.currentTimeMillis();

    private ScheduledExecutorService scheduler;
    private ExecutorService adbPool;
    private Semaphore pingSemaphore;
    private volatile boolean paused = false;

    public DeviceManager(Settings settings, ADBService adbService, Logger logger) {
        this.settings = settings;
        this.adbService = adbService;
        this.logger = logger;
    }

    public ObservableList<DeviceInfo> getDevices() {
        return devices;
    }

    public StringProperty adbHealthProperty() {
        return adbHealth;
    }

    public StringProperty lastScanDurationProperty() {
        return lastScanDuration;
    }

    public StringProperty lastErrorProperty() {
        return lastError;
    }

    public StringProperty pingBatchDurationProperty() {
        return pingBatchDuration;
    }

    public StringProperty pingBatchCountProperty() {
        return pingBatchCount;
    }

    public long getTotalPolls() {
        return totalPolls.get();
    }

    public long getPingOk() {
        return pingOk.get();
    }

    public long getPingFail() {
        return pingFail.get();
    }

    public StringProperty expectedMissingProperty() {
        return expectedMissing;
    }

    public void setExpectedSerials(Set<String> serials) {
        expectedSerials.clear();
        if (serials != null) {
            expectedSerials.addAll(serials);
        }
        updateExpectedFlags();
    }

    public Set<String> getExpectedSerials() {
        return new HashSet<>(expectedSerials);
    }

    public void setPaused(boolean value) {
        paused = value;
    }

    public boolean isPaused() {
        return paused;
    }

    public void restartAdb() {
        adbPool.submit(() -> {
            adbService.runAdb(List.of("kill-server"), Duration.ofSeconds(settings.getAdbTimeoutSeconds()));
            adbService.runAdb(List.of("start-server"), Duration.ofSeconds(settings.getAdbTimeoutSeconds()));
            Platform.runLater(() -> lastError.set(""));
        });
    }

    public void start() {
        int poolSize = Math.max(4, settings.getPingMaxConcurrency());
        adbPool = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "adb-worker");
            t.setDaemon(true);
            return t;
        });
        pingSemaphore = new Semaphore(settings.getPingMaxConcurrency());
        resetAdbTokens();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "device-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduleTasks();
    }

    public void applySettings() {
        reschedule();
    }

    public void rescanNow() {
        if (scheduler != null) {
            scheduler.execute(this::pollDevices);
        }
    }

    public void pingAll() {
        adbPool.submit(() -> {
            List<DeviceInfo> toPing = new ArrayList<>();
            for (DeviceInfo device : new ArrayList<>(devices)) {
                if ("device".equalsIgnoreCase(device.getAdbState())) {
                    toPing.add(device);
                }
            }
            if (toPing.isEmpty()) {
                return;
            }
            long start = System.nanoTime();
            AtomicInteger remaining = new AtomicInteger(toPing.size());
            Platform.runLater(() -> pingBatchCount.set(String.valueOf(toPing.size())));
            for (DeviceInfo device : toPing) {
                adbPool.submit(() -> pingDevice(device, remaining, start));
            }
        });
    }

    private void scheduleTasks() {
        scheduler.scheduleWithFixedDelay(wrapSafe(this::pollDevices, "pollDevices"), 0,
                settings.getPollIntervalSeconds(), TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(wrapSafe(this::autoPing, "autoPing"), 2,
                settings.getPingIntervalSeconds(), TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(wrapSafe(this::resetAdbTokens, "resetAdbTokens"),
                settings.getAdbRateIntervalSeconds(), settings.getAdbRateIntervalSeconds(), TimeUnit.SECONDS);
    }

    private Runnable wrapSafe(Runnable task, String name) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                logger.log(Level.WARNING, name + " crashed", t);
                Platform.runLater(() -> lastError.set(name + " crashed: " + t.getMessage()));
            }
        };
    }

    private void reschedule() {
        scheduler.shutdownNow();
        pingSemaphore = new Semaphore(settings.getPingMaxConcurrency());
        resetAdbTokens();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "device-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduleTasks();
    }

    private synchronized void resetAdbTokens() {
        adbTokenWindowStart = System.currentTimeMillis();
        adbTokens.set(settings.getAdbRateLimitPerInterval());
    }

    private boolean tryConsumeAdbToken() {
        long now = System.currentTimeMillis();
        if (now - adbTokenWindowStart > settings.getAdbRateIntervalSeconds() * 1000L) {
            resetAdbTokens();
        }
        if (adbTokens.get() <= 0) {
            return false;
        }
        adbTokens.decrementAndGet();
        return true;
    }
    private void pollDevices() {
        long start = System.nanoTime();
        try {
            if (paused) {
                return;
            }
            totalPolls.incrementAndGet();
            String output;
            if (!tryConsumeAdbToken()) {
                Platform.runLater(() -> lastError.set("adb rate limit hit"));
                return;
            }
            ADBService.ExecResult res = adbService.runAdb(List.of("devices", "-l"), Duration.ofSeconds(settings.getAdbTimeoutSeconds()));
            if (res.timedOut || res.exitCode != 0) {
                logger.warning("adb devices -l failed: " + res.stderr);
                Platform.runLater(() -> adbHealth.set("ADB ERROR"));
                Platform.runLater(() -> lastError.set("adb devices -l failed: " + (res.stderr == null ? "" : res.stderr.trim())));
                markAllDisconnected();
                return;
            }
            output = res.stdout;
            Platform.runLater(() -> adbHealth.set("ADB OK"));
            Platform.runLater(() -> lastError.set(""));
            Map<String, ParsedDevice> parsed = parseDevices(output);
            Instant now = Instant.now();

            for (ParsedDevice dev : parsed.values()) {
                DeviceInfo info = registry.computeIfAbsent(dev.serial, DeviceInfo::new);
                updateDevice(info, dev, now);
            }

            for (String serial : new HashSet<>(registry.keySet())) {
                if (!parsed.containsKey(serial)) {
                    DeviceInfo info = registry.get(serial);
                    if (info != null) {
                        Platform.runLater(() -> info.setAdbState(DeviceStatus.DISCONNECTED.getAdbLabel()));
                    }
                }
            }
            updateExpectedFlags();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Polling error", e);
            Platform.runLater(() -> adbHealth.set("ADB ERROR"));
            Platform.runLater(() -> lastError.set("polling error: " + e.getMessage()));
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            Platform.runLater(() -> lastScanDuration.set(durationMs + " ms"));
        }
    }

    private void updateDevice(DeviceInfo info, ParsedDevice dev, Instant now) {
        Platform.runLater(() -> {
            if (!devices.contains(info)) {
                devices.add(info);
            }
            info.setAdbState(dev.state);
            if (dev.model != null && !dev.model.isBlank()) {
                info.setModel(dev.model);
            }
            info.markLastSeen(now);
        });

        if ("device".equalsIgnoreCase(dev.state)) {
            if (info.modelProperty().get().isBlank() || info.androidVersionProperty().get().isBlank()) {
                adbPool.submit(() -> refreshProps(info));
            }
        }
    }

    private void refreshProps(DeviceInfo info) {
        if (!tryConsumeAdbToken()) {
            return;
        }
        String serial = info.getSerial();
        Duration timeout = Duration.ofSeconds(settings.getAdbTimeoutSeconds());

        String model = null;
        String version = null;

        if (!tryConsumeAdbToken()) {
            return;
        }
        ADBService.ExecResult modelRes = adbService.runAdb(List.of("-s", serial, "shell", "getprop", "ro.product.model"), timeout);
        if (!modelRes.timedOut && modelRes.exitCode == 0) {
            model = modelRes.stdout.trim();
        }

        if (!tryConsumeAdbToken()) {
            return;
        }
        ADBService.ExecResult verRes = adbService.runAdb(List.of("-s", serial, "shell", "getprop", "ro.build.version.release"), timeout);
        if (!verRes.timedOut && verRes.exitCode == 0) {
            version = verRes.stdout.trim();
        }

        String finalModel = model;
        String finalVersion = version;
        Platform.runLater(() -> {
            if (finalModel != null && !finalModel.isBlank()) {
                info.setModel(finalModel);
            }
            if (finalVersion != null && !finalVersion.isBlank()) {
                info.setAndroidVersion(finalVersion);
            }
        });
    }

    private void autoPing() {
        if (paused) {
            return;
        }
        Instant now = Instant.now();
        List<DeviceInfo> toPing = new ArrayList<>();
        for (DeviceInfo device : new ArrayList<>(devices)) {
            if (!"device".equalsIgnoreCase(device.getAdbState())) {
                continue;
            }
            Instant backoffUntil = pingBackoffUntil.get(device.getSerial());
            if (backoffUntil != null && backoffUntil.isAfter(now)) {
                continue;
            }
            toPing.add(device);
        }
        if (toPing.isEmpty()) {
            return;
        }

        long start = System.nanoTime();
        AtomicInteger remaining = new AtomicInteger(toPing.size());
        Platform.runLater(() -> pingBatchCount.set(String.valueOf(toPing.size())));
        for (DeviceInfo device : toPing) {
            adbPool.submit(() -> pingDevice(device, remaining, start));
        }
    }

    private void pingDevice(DeviceInfo device, AtomicInteger remaining, long batchStart) {
        if (!pingSemaphore.tryAcquire()) {
            if (remaining.decrementAndGet() == 0) {
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - batchStart);
                Platform.runLater(() -> pingBatchDuration.set(ms + " ms"));
            }
            return;
        }
        String serial = device.getSerial();
        Duration timeout = Duration.ofSeconds(settings.getAdbTimeoutSeconds());
        try {
            if (!tryConsumeAdbToken()) {
                pingFail.incrementAndGet();
                return;
            }
            ADBService.ExecResult res = adbService.runAdb(List.of("-s", serial, "shell", "echo", "PING"), timeout);
            Instant now = Instant.now();

            boolean ok = !res.timedOut && res.exitCode == 0 && res.stdout != null && res.stdout.contains("PING");
            if (!ok) {
                int current = pingBackoffMultiplier.getOrDefault(serial, 1);
                int next = Math.min(current + 1, Math.max(1, settings.getPingBackoffMaxSeconds() / settings.getPingBackoffStepSeconds()));
                pingBackoffMultiplier.put(serial, next);
                long delay = Math.min(settings.getPingBackoffMaxSeconds(), (long) settings.getPingBackoffStepSeconds() * next);
                pingBackoffUntil.put(serial, now.plusSeconds(delay));
                pingFail.incrementAndGet();
            } else {
                pingBackoffUntil.remove(serial);
                pingBackoffMultiplier.remove(serial);
                pingOk.incrementAndGet();
            }
            Platform.runLater(() -> {
                device.setPingStatus(ok ? "OK" : "FAIL");
                device.markLastPing(now);
            });
        } finally {
            pingSemaphore.release();
            if (remaining.decrementAndGet() == 0) {
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - batchStart);
                Platform.runLater(() -> pingBatchDuration.set(ms + " ms"));
            }
        }
    }

    private void markAllDisconnected() {
        Platform.runLater(() -> {
            for (DeviceInfo device : devices) {
                device.setAdbState(DeviceStatus.DISCONNECTED.getAdbLabel());
            }
        });
    }

    private void updateExpectedFlags() {
        Platform.runLater(() -> {
            int missing = 0;
            for (DeviceInfo device : devices) {
                boolean isExpected = expectedSerials.contains(device.getSerial());
                device.setExpected(isExpected);
            }
            for (String serial : expectedSerials) {
                DeviceInfo info = registry.get(serial);
                if (info == null || "disconnected".equalsIgnoreCase(info.getAdbState())) {
                    missing++;
                }
            }
            expectedMissing.set(String.valueOf(missing));
        });
    }

    private Map<String, ParsedDevice> parseDevices(String output) {
        Map<String, ParsedDevice> result = new HashMap<>();
        if (output == null || output.isBlank()) {
            return result;
        }
        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("List of devices")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            String serial = parts[0];
            String state = parts[1];
            String model = null;
            for (int i = 2; i < parts.length; i++) {
                String token = parts[i];
                if (token.startsWith("model:")) {
                    model = token.substring("model:".length());
                }
            }
            ParsedDevice dev = new ParsedDevice(serial, state, model);
            result.put(serial, dev);
        }
        return result;
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (adbPool != null) {
            adbPool.shutdownNow();
        }
        adbService.shutdown();
    }

    private static class ParsedDevice {
        final String serial;
        final String state;
        final String model;

        ParsedDevice(String serial, String state, String model) {
            this.serial = serial;
            this.state = DeviceStatus.fromAdbState(state).getAdbLabel();
            this.model = model;
        }
    }
}
