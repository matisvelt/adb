package com.antlab.rigcontrol.sorter;

import com.antlab.rigcontrol.DeviceInfo;
import com.antlab.rigcontrol.DeviceManager;
import com.antlab.rigcontrol.worker.WorkerClient;
import com.antlab.rigcontrol.worker.WorkerFeatures;
import com.antlab.rigcontrol.worker.WorkerResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Dispatcher {
    private final DeviceManager deviceManager;
    private final ProjectManager projectManager;
    private final PreviewGenerator previewGenerator;
    private final RulesEngine rulesEngine;
    private final FileMover fileMover;
    private final WorkerClient workerClient;
    private final JobQueue queue = new JobQueue();
    private final Logger logger;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "dispatch-scheduler");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService workerPool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "dispatch-worker");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, Long> deviceBackoff = new ConcurrentHashMap<>();
    private final Set<String> busyDevices = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.atomic.AtomicInteger inFlight = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile boolean running = false;
    private volatile boolean scheduled = false;

    public Dispatcher(DeviceManager deviceManager,
                      ProjectManager projectManager,
                      PreviewGenerator previewGenerator,
                      RulesEngine rulesEngine,
                      FileMover fileMover,
                      WorkerClient workerClient,
                      Logger logger) {
        this.deviceManager = deviceManager;
        this.projectManager = projectManager;
        this.previewGenerator = previewGenerator;
        this.rulesEngine = rulesEngine;
        this.fileMover = fileMover;
        this.workerClient = workerClient;
        this.logger = logger;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        refreshQueue();
        if (!scheduled) {
            scheduler.scheduleWithFixedDelay(this::dispatchLoop, 0, 1, TimeUnit.SECONDS);
            scheduled = true;
        }
    }

    public void stop() {
        running = false;
    }

    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
        workerPool.shutdownNow();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void refreshQueue() {
        queue.clear();
        for (FileRecord record : projectManager.getRecords()) {
            if (record.getStatus() == FileStatus.NEW || record.getStatus() == FileStatus.ERROR) {
                record.setStatus(FileStatus.QUEUED);
                try {
                    projectManager.upsertRecord(record);
                } catch (Exception ex) {
                    logger.warning("Failed to queue record: " + ex.getMessage());
                }
                queue.enqueue(record.getFileId());
            }
        }
    }

    private void dispatchLoop() {
        if (!running || projectManager.getConfig() == null) {
            return;
        }
        rulesEngine.setRules(projectManager.getConfig().getRules());
        for (DeviceInfo device : new ArrayList<>(deviceManager.getDevices())) {
            if (!"device".equalsIgnoreCase(device.getAdbState())) {
                continue;
            }
            if (busyDevices.contains(device.getSerial())) {
                continue;
            }
            if (isBackoff(device.getSerial())) {
                continue;
            }
            if (inFlight.get() >= projectManager.getConfig().getMaxInFlight()) {
                break;
            }
            List<String> batchIds = queue.pollBatch(projectManager.getConfig().getBatchSize());
            if (batchIds.isEmpty()) {
                break;
            }
            busyDevices.add(device.getSerial());
            workerPool.submit(() -> runBatch(device, batchIds));
        }
    }

    private void runBatch(DeviceInfo device, List<String> fileIds) {
        inFlight.addAndGet(fileIds.size());
        try {
            List<FileRecord> records = new ArrayList<>();
            for (String id : fileIds) {
                FileRecord record = projectManager.getRecord(id);
                if (record == null || record.getStatus() == FileStatus.MOVED || record.getStatus() == FileStatus.REVIEW) {
                    continue;
                }
                records.add(record);
            }
            if (records.isEmpty()) {
                return;
            }
            List<WorkerClient.PreviewPayload> payloads = new ArrayList<>();
            for (FileRecord record : records) {
                if (!ensurePreview(record)) {
                    continue;
                }
                record.setStatus(FileStatus.DISPATCHED);
                try {
                    projectManager.upsertRecord(record);
                } catch (Exception ex) {
                    logger.warning("Dispatch persist failed: " + ex.getMessage());
                }
                Path previewPath = Path.of(record.getPreviewPath());
                payloads.add(WorkerClient.PreviewPayload.fromPath(record.getFileId(), previewPath));
            }
            if (payloads.isEmpty()) {
                return;
            }
            List<WorkerResult> results = workerClient.classifyBatch(device, payloads);
            java.util.Set<String> returned = new java.util.HashSet<>();
            for (WorkerResult result : results) {
                returned.add(result.getFileId());
                FileRecord record = projectManager.getRecord(result.getFileId());
                if (record == null) {
                    continue;
                }
                applyResult(record, result);
            }
            for (FileRecord record : records) {
                if (!returned.contains(record.getFileId())) {
                    record.setStatus(FileStatus.ERROR);
                    record.setError("Missing worker result");
                    try {
                        projectManager.upsertRecord(record);
                    } catch (Exception ex) {
                        logger.warning("Missing result persist failed: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            logger.warning("Dispatch failed: " + ex.getMessage());
            backoff(device.getSerial());
            requeue(fileIds);
        } finally {
            inFlight.addAndGet(-fileIds.size());
            busyDevices.remove(device.getSerial());
        }
    }

    private boolean ensurePreview(FileRecord record) {
        if (record.getPreviewPath() != null && Files.exists(Path.of(record.getPreviewPath()))) {
            record.setPreviewStatus(PreviewStatus.READY);
            return true;
        }
        PreviewGenerator.PreviewResult result = previewGenerator.generate(record, projectManager.getPreviewDir(),
                projectManager.getConfig().getPreviewPolicy());
        if (!result.isOk()) {
            record.setPreviewStatus(PreviewStatus.ERROR);
            record.setStatus(FileStatus.ERROR);
            record.setError(result.getError());
            try {
                projectManager.upsertRecord(record);
            } catch (Exception ex) {
                logger.warning("Preview error persist failed: " + ex.getMessage());
            }
            return false;
        }
        record.setPreviewStatus(PreviewStatus.READY);
        record.setPreviewPath(result.getPreviewPath());
        record.setStatus(FileStatus.PREVIEWED);
        try {
            projectManager.upsertRecord(record);
        } catch (Exception ex) {
            logger.warning("Preview persist failed: " + ex.getMessage());
        }
        return true;
    }

    private void applyResult(FileRecord record, WorkerResult result) {
        record.setLabel(result.getTopLabel());
        record.setConfidence(result.getConfidence());
        WorkerFeatures features = result.getFeatures();
        if (features != null) {
            record.setFacesCount(features.getFacesCount());
            record.setHasTextLikelihood(features.getHasTextLikelihood());
            record.setIsDocumentLikelihood(features.getIsDocumentLikelihood());
            record.setScreenshotLikelihood(features.getScreenshotLikelihood());
            record.setNotes(features.getNotes());
        }
        record.setStatus(FileStatus.INFERRED);

        ProjectConfig config = projectManager.getConfig();
        if (record.getConfidence() < config.getConfidenceThreshold() || "UNKNOWN".equalsIgnoreCase(record.getLabel())) {
            record.setStatus(FileStatus.REVIEW);
            record.setMoveStatus("REVIEW");
        } else {
            RulesEngine.RuleMatch match = rulesEngine.evaluate(record);
            if (match == null) {
                record.setStatus(FileStatus.REVIEW);
                record.setMoveStatus("REVIEW");
            } else {
                record.setRuleId(match.getRule().getId());
                record.setRuleName(match.getRule().getNotes());
                FileMover.MoveResult move = fileMover.move(record, config, match.getDestination());
                if (move.isOk()) {
                    String originalPath = record.getSourcePath();
                    record.setDestinationPath(move.getDestination());
                    record.setMoveStatus("MOVED");
                    record.setStatus(FileStatus.MOVED);
                    writeAudit(record, move.getDestination(), originalPath);
                    record.setSourcePath(move.getDestination());
                    cleanupPreview(record);
                } else {
                    record.setStatus(FileStatus.ERROR);
                    record.setMoveStatus("ERROR");
                    record.setError(move.getError());
                }
            }
        }
        try {
            projectManager.upsertRecord(record);
        } catch (Exception ex) {
            logger.warning("Persist result failed: " + ex.getMessage());
        }
    }

    private void writeAudit(FileRecord record, String destination, String originalPath) {
        AuditRecord audit = new AuditRecord();
        audit.setFileId(record.getFileId());
        audit.setFromPath(originalPath);
        audit.setToPath(destination);
        audit.setTimestamp(System.currentTimeMillis());
        audit.setRuleId(record.getRuleId());
        audit.setRuleName(record.getRuleName());
        audit.setLabel(record.getLabel());
        audit.setConfidence(record.getConfidence());
        audit.setStatus("MOVED");
        try {
            projectManager.appendAudit(audit);
        } catch (Exception ex) {
            logger.warning("Audit write failed: " + ex.getMessage());
        }
    }

    private void cleanupPreview(FileRecord record) {
        if (projectManager.getConfig().getPreviewPolicy().isKeepPreviews()) {
            return;
        }
        try {
            if (record.getPreviewPath() != null) {
                Files.deleteIfExists(Path.of(record.getPreviewPath()));
            }
        } catch (Exception ignored) {
        }
    }

    private void backoff(String serial) {
        deviceBackoff.put(serial, Instant.now().plusSeconds(30).toEpochMilli());
    }

    private boolean isBackoff(String serial) {
        Long until = deviceBackoff.get(serial);
        return until != null && until > System.currentTimeMillis();
    }

    private void requeue(List<String> fileIds) {
        for (String id : fileIds) {
            queue.enqueue(id);
            FileRecord record = projectManager.getRecord(id);
            if (record != null) {
                record.setStatus(FileStatus.QUEUED);
                try {
                    projectManager.upsertRecord(record);
                } catch (Exception ex) {
                    logger.warning("Requeue persist failed: " + ex.getMessage());
                }
            }
        }
    }
}
