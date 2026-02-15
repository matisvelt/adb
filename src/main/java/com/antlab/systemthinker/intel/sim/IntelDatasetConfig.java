package com.antlab.systemthinker.intel.sim;

public final class IntelDatasetConfig {
    private final int scenarios;
    private final int trialsPerScenario;
    private final long baseSeed;
    private final DoubleRange sensitivityRange;
    private final DoubleRange specificityRange;
    private final DoubleRange dropoutRange;
    private final DoubleRange urgencyRange;
    private final DoubleRange falsePositiveCostRange;
    private final DoubleRange falseNegativeCostRange;
    private final DoubleRange actThresholdRange;

    public IntelDatasetConfig(int scenarios,
                              int trialsPerScenario,
                              long baseSeed,
                              DoubleRange sensitivityRange,
                              DoubleRange specificityRange,
                              DoubleRange dropoutRange,
                              DoubleRange urgencyRange,
                              DoubleRange falsePositiveCostRange,
                              DoubleRange falseNegativeCostRange,
                              DoubleRange actThresholdRange) {
        this.scenarios = scenarios;
        this.trialsPerScenario = trialsPerScenario;
        this.baseSeed = baseSeed;
        this.sensitivityRange = sensitivityRange;
        this.specificityRange = specificityRange;
        this.dropoutRange = dropoutRange;
        this.urgencyRange = urgencyRange;
        this.falsePositiveCostRange = falsePositiveCostRange;
        this.falseNegativeCostRange = falseNegativeCostRange;
        this.actThresholdRange = actThresholdRange;
    }

    public int getScenarios() {
        return scenarios;
    }

    public int getTrialsPerScenario() {
        return trialsPerScenario;
    }

    public long getBaseSeed() {
        return baseSeed;
    }

    public DoubleRange getSensitivityRange() {
        return sensitivityRange;
    }

    public DoubleRange getSpecificityRange() {
        return specificityRange;
    }

    public DoubleRange getDropoutRange() {
        return dropoutRange;
    }

    public DoubleRange getUrgencyRange() {
        return urgencyRange;
    }

    public DoubleRange getFalsePositiveCostRange() {
        return falsePositiveCostRange;
    }

    public DoubleRange getFalseNegativeCostRange() {
        return falseNegativeCostRange;
    }

    public DoubleRange getActThresholdRange() {
        return actThresholdRange;
    }

    public String toJson() {
        return String.format("{\"scenarios\":%d,\"trialsPerScenario\":%d,\"baseSeed\":%d,\"sensitivityRange\":%s,\"specificityRange\":%s,\"dropoutRange\":%s,\"urgencyRange\":%s,\"falsePositiveCostRange\":%s,\"falseNegativeCostRange\":%s,\"actThresholdRange\":%s}",
                scenarios, trialsPerScenario, baseSeed,
                sensitivityRange.toJson(), specificityRange.toJson(), dropoutRange.toJson(),
                urgencyRange.toJson(), falsePositiveCostRange.toJson(), falseNegativeCostRange.toJson(),
                actThresholdRange.toJson());
    }
}
