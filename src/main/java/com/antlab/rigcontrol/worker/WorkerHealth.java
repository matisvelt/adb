package com.antlab.rigcontrol.worker;

public class WorkerHealth {
    private boolean ok;
    private String version;
    private String modelVersion;
    private int queueDepth;
    private long uptimeSeconds;
    private long memoryMb;
    private String error;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public int getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(int queueDepth) {
        this.queueDepth = queueDepth;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public long getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(long memoryMb) {
        this.memoryMb = memoryMb;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
