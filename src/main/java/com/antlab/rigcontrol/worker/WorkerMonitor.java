package com.antlab.rigcontrol.worker;

import com.antlab.rigcontrol.DeviceInfo;
import com.antlab.rigcontrol.DeviceManager;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkerMonitor {
    private final DeviceManager deviceManager;
    private final WorkerClient workerClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "worker-monitor");
        t.setDaemon(true);
        return t;
    });

    public WorkerMonitor(DeviceManager deviceManager, WorkerClient workerClient) {
        this.deviceManager = deviceManager;
        this.workerClient = workerClient;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::refreshAll, 2, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public void refreshAll() {
        for (DeviceInfo device : new ArrayList<>(deviceManager.getDevices())) {
            if (!"device".equalsIgnoreCase(device.getAdbState())) {
                javafx.application.Platform.runLater(() -> device.setWorkerStatus("-"));
                continue;
            }
            WorkerHealth health = workerClient.checkHealth(device);
            javafx.application.Platform.runLater(() -> {
                if (health.isOk()) {
                    device.setWorkerStatus("OK");
                } else {
                    device.setWorkerStatus("FAIL");
                }
                device.setWorkerVersion(health.getVersion());
                device.setWorkerQueueDepth(health.getQueueDepth());
                device.setWorkerUptime(health.getUptimeSeconds());
                device.setWorkerError(health.getError());
            });
        }
    }
}
