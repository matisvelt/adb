package com.antlab.systemthinker.intel.sim;

public final class IntelTrialResult {
    private final long seed;
    private final double totalCost;
    private final double overreactionRate;
    private final double missedThreatRate;
    private final double meanDecisionDelay;
    private final double decisionAccuracy;
    private final int actCount;
    private final int actDuringNoThreat;
    private final int activeSteps;
    private final int missedActiveSteps;
    private final int investigationCount;
    private final int activeEpisodes;

    public IntelTrialResult(long seed,
                            double totalCost,
                            double overreactionRate,
                            double missedThreatRate,
                            double meanDecisionDelay,
                            double decisionAccuracy,
                            int actCount,
                            int actDuringNoThreat,
                            int activeSteps,
                            int missedActiveSteps,
                            int investigationCount,
                            int activeEpisodes) {
        this.seed = seed;
        this.totalCost = totalCost;
        this.overreactionRate = overreactionRate;
        this.missedThreatRate = missedThreatRate;
        this.meanDecisionDelay = meanDecisionDelay;
        this.decisionAccuracy = decisionAccuracy;
        this.actCount = actCount;
        this.actDuringNoThreat = actDuringNoThreat;
        this.activeSteps = activeSteps;
        this.missedActiveSteps = missedActiveSteps;
        this.investigationCount = investigationCount;
        this.activeEpisodes = activeEpisodes;
    }

    public long getSeed() {
        return seed;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getOverreactionRate() {
        return overreactionRate;
    }

    public double getMissedThreatRate() {
        return missedThreatRate;
    }

    public double getMeanDecisionDelay() {
        return meanDecisionDelay;
    }

    public double getDecisionAccuracy() {
        return decisionAccuracy;
    }

    public int getActCount() {
        return actCount;
    }

    public int getActDuringNoThreat() {
        return actDuringNoThreat;
    }

    public int getActiveSteps() {
        return activeSteps;
    }

    public int getMissedActiveSteps() {
        return missedActiveSteps;
    }

    public int getInvestigationCount() {
        return investigationCount;
    }

    public int getActiveEpisodes() {
        return activeEpisodes;
    }

    public String toJson() {
        return String.format("{\"seed\":%d,\"totalCost\":%.5f,\"overreactionRate\":%.5f,\"missedThreatRate\":%.5f,\"meanDecisionDelay\":%.5f,\"decisionAccuracy\":%.5f,\"actCount\":%d,\"actDuringNoThreat\":%d,\"activeSteps\":%d,\"missedActiveSteps\":%d,\"investigationCount\":%d,\"activeEpisodes\":%d}",
                seed, totalCost, overreactionRate, missedThreatRate, meanDecisionDelay, decisionAccuracy,
                actCount, actDuringNoThreat, activeSteps, missedActiveSteps, investigationCount, activeEpisodes);
    }
}
