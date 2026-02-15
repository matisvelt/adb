package com.antlab.systemthinker.intel.sim;

public final class SourceConfig {
    private final int sourceId;
    private final String name;
    private final double sensitivity;
    private final double specificity;
    private final double bias;
    private final double dropout;
    private final double weight;
    private final double delayMean;
    private final int maxDelay;

    public SourceConfig(int sourceId,
                        String name,
                        double sensitivity,
                        double specificity,
                        double bias,
                        double dropout,
                        double weight,
                        double delayMean,
                        int maxDelay) {
        this.sourceId = sourceId;
        this.name = name;
        this.sensitivity = sensitivity;
        this.specificity = specificity;
        this.bias = bias;
        this.dropout = dropout;
        this.weight = weight;
        this.delayMean = delayMean;
        this.maxDelay = maxDelay;
    }

    public int getSourceId() {
        return sourceId;
    }

    public String getName() {
        return name;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public double getSpecificity() {
        return specificity;
    }

    public double getBias() {
        return bias;
    }

    public double getDropout() {
        return dropout;
    }

    public double getWeight() {
        return weight;
    }

    public double getDelayMean() {
        return delayMean;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public SourceConfig withSensitivity(double value) {
        return new SourceConfig(sourceId, name, value, specificity, bias, dropout, weight, delayMean, maxDelay);
    }

    public SourceConfig withSpecificity(double value) {
        return new SourceConfig(sourceId, name, sensitivity, value, bias, dropout, weight, delayMean, maxDelay);
    }

    public SourceConfig withBias(double value) {
        return new SourceConfig(sourceId, name, sensitivity, specificity, value, dropout, weight, delayMean, maxDelay);
    }

    public SourceConfig withWeight(double value) {
        return new SourceConfig(sourceId, name, sensitivity, specificity, bias, dropout, value, delayMean, maxDelay);
    }

    public SourceConfig withDelayMean(double value) {
        return new SourceConfig(sourceId, name, sensitivity, specificity, bias, dropout, weight, value, maxDelay);
    }

    public SourceConfig withMaxDelay(int value) {
        return new SourceConfig(sourceId, name, sensitivity, specificity, bias, dropout, weight, delayMean, value);
    }

    public SourceConfig withDropout(double value) {
        return new SourceConfig(sourceId, name, sensitivity, specificity, bias, value, weight, delayMean, maxDelay);
    }

    public String toJson() {
        return String.format("{\"id\":%d,\"name\":\"%s\",\"sensitivity\":%.5f,\"specificity\":%.5f,\"bias\":%.5f,\"dropout\":%.5f,\"weight\":%.5f,\"delayMean\":%.5f,\"maxDelay\":%d}",
                sourceId, name.replace("\"", "'"), sensitivity, specificity, bias, dropout, weight, delayMean, maxDelay);
    }
}
