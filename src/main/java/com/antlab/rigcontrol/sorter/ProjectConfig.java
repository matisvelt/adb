package com.antlab.rigcontrol.sorter;

import java.util.ArrayList;
import java.util.List;

public class ProjectConfig {
    private String name = "RigSort Project";
    private String sourceRoot;
    private String destinationRoot;
    private boolean strictHash = false;
    private double confidenceThreshold = 0.70;
    private int batchSize = 16;
    private int maxInFlight = 32;
    private boolean appendDateFolders = true;
    private PreviewPolicy previewPolicy = new PreviewPolicy();
    private List<Rule> rules = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceRoot() {
        return sourceRoot;
    }

    public void setSourceRoot(String sourceRoot) {
        this.sourceRoot = sourceRoot;
    }

    public String getDestinationRoot() {
        return destinationRoot;
    }

    public void setDestinationRoot(String destinationRoot) {
        this.destinationRoot = destinationRoot;
    }

    public boolean isStrictHash() {
        return strictHash;
    }

    public void setStrictHash(boolean strictHash) {
        this.strictHash = strictHash;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        if (confidenceThreshold < 0) {
            this.confidenceThreshold = 0.0;
        } else if (confidenceThreshold > 1.0) {
            this.confidenceThreshold = 1.0;
        } else {
            this.confidenceThreshold = confidenceThreshold;
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxInFlight() {
        return maxInFlight;
    }

    public void setMaxInFlight(int maxInFlight) {
        this.maxInFlight = maxInFlight;
    }

    public boolean isAppendDateFolders() {
        return appendDateFolders;
    }

    public void setAppendDateFolders(boolean appendDateFolders) {
        this.appendDateFolders = appendDateFolders;
    }

    public PreviewPolicy getPreviewPolicy() {
        return previewPolicy;
    }

    public void setPreviewPolicy(PreviewPolicy previewPolicy) {
        this.previewPolicy = previewPolicy == null ? new PreviewPolicy() : previewPolicy;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules == null ? new ArrayList<>() : rules;
    }
}
