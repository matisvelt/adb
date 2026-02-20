package com.antlab.rigcontrol.worker;

public class WorkerResult {
    private String fileId;
    private String topLabel;
    private double confidence;
    private WorkerFeatures features;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getTopLabel() {
        return topLabel;
    }

    public void setTopLabel(String topLabel) {
        this.topLabel = topLabel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public WorkerFeatures getFeatures() {
        return features;
    }

    public void setFeatures(WorkerFeatures features) {
        this.features = features;
    }
}
