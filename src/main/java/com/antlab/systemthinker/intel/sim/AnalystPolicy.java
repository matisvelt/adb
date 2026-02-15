package com.antlab.systemthinker.intel.sim;

public final class AnalystPolicy {
    private final double baselineBelief;
    private final double decayFactor;
    private final double actThresholdBase;
    private final double investigateLow;
    private final double investigateHigh;
    private final double urgencyActShift;
    private final double urgencyInvestigateShift;
    private final int investigationBoostSteps;
    private final double investigationSensitivityBoost;
    private final double investigationSpecificityBoost;
    private final double investigationDropoutMultiplier;
    private final double investigationDelayMultiplier;

    public AnalystPolicy(double baselineBelief,
                         double decayFactor,
                         double actThresholdBase,
                         double investigateLow,
                         double investigateHigh,
                         double urgencyActShift,
                         double urgencyInvestigateShift,
                         int investigationBoostSteps,
                         double investigationSensitivityBoost,
                         double investigationSpecificityBoost,
                         double investigationDropoutMultiplier,
                         double investigationDelayMultiplier) {
        this.baselineBelief = baselineBelief;
        this.decayFactor = decayFactor;
        this.actThresholdBase = actThresholdBase;
        this.investigateLow = investigateLow;
        this.investigateHigh = investigateHigh;
        this.urgencyActShift = urgencyActShift;
        this.urgencyInvestigateShift = urgencyInvestigateShift;
        this.investigationBoostSteps = investigationBoostSteps;
        this.investigationSensitivityBoost = investigationSensitivityBoost;
        this.investigationSpecificityBoost = investigationSpecificityBoost;
        this.investigationDropoutMultiplier = investigationDropoutMultiplier;
        this.investigationDelayMultiplier = investigationDelayMultiplier;
    }

    public double getBaselineBelief() {
        return baselineBelief;
    }

    public double getDecayFactor() {
        return decayFactor;
    }

    public double getActThresholdBase() {
        return actThresholdBase;
    }

    public double getInvestigateLow() {
        return investigateLow;
    }

    public double getInvestigateHigh() {
        return investigateHigh;
    }

    public double getUrgencyActShift() {
        return urgencyActShift;
    }

    public double getUrgencyInvestigateShift() {
        return urgencyInvestigateShift;
    }

    public int getInvestigationBoostSteps() {
        return investigationBoostSteps;
    }

    public double getInvestigationSensitivityBoost() {
        return investigationSensitivityBoost;
    }

    public double getInvestigationSpecificityBoost() {
        return investigationSpecificityBoost;
    }

    public double getInvestigationDropoutMultiplier() {
        return investigationDropoutMultiplier;
    }

    public double getInvestigationDelayMultiplier() {
        return investigationDelayMultiplier;
    }

    public double actThreshold(double urgency) {
        return clamp01(actThresholdBase - urgencyActShift * urgency);
    }

    public double investigateLow(double urgency) {
        return clamp01(investigateLow - urgencyInvestigateShift * urgency);
    }

    public double investigateHigh(double urgency) {
        return clamp01(investigateHigh - urgencyInvestigateShift * urgency);
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    public String toJson() {
        return String.format("{\"baselineBelief\":%.5f,\"decayFactor\":%.5f,\"actThresholdBase\":%.5f,\"investigateLow\":%.5f,\"investigateHigh\":%.5f,\"urgencyActShift\":%.5f,\"urgencyInvestigateShift\":%.5f,\"investigationBoostSteps\":%d,\"investigationSensitivityBoost\":%.5f,\"investigationSpecificityBoost\":%.5f,\"investigationDropoutMultiplier\":%.5f,\"investigationDelayMultiplier\":%.5f}",
                baselineBelief, decayFactor, actThresholdBase, investigateLow, investigateHigh,
                urgencyActShift, urgencyInvestigateShift, investigationBoostSteps,
                investigationSensitivityBoost, investigationSpecificityBoost,
                investigationDropoutMultiplier, investigationDelayMultiplier);
    }
}
