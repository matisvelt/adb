package com.antlab.systemthinker.intel.sim;

public final class CostWeights {
    private final double falsePositiveCost;
    private final double falseNegativeCost;
    private final double investigationCost;
    private final double actCost;
    private final double urgencyPenaltyMultiplier;

    public CostWeights(double falsePositiveCost,
                       double falseNegativeCost,
                       double investigationCost,
                       double actCost,
                       double urgencyPenaltyMultiplier) {
        this.falsePositiveCost = falsePositiveCost;
        this.falseNegativeCost = falseNegativeCost;
        this.investigationCost = investigationCost;
        this.actCost = actCost;
        this.urgencyPenaltyMultiplier = urgencyPenaltyMultiplier;
    }

    public double getFalsePositiveCost() {
        return falsePositiveCost;
    }

    public double getFalseNegativeCost() {
        return falseNegativeCost;
    }

    public double getInvestigationCost() {
        return investigationCost;
    }

    public double getActCost() {
        return actCost;
    }

    public double getUrgencyPenaltyMultiplier() {
        return urgencyPenaltyMultiplier;
    }

    public String toJson() {
        return String.format("{\"falsePositiveCost\":%.5f,\"falseNegativeCost\":%.5f,\"investigationCost\":%.5f,\"actCost\":%.5f,\"urgencyPenaltyMultiplier\":%.5f}",
                falsePositiveCost, falseNegativeCost, investigationCost, actCost, urgencyPenaltyMultiplier);
    }
}
