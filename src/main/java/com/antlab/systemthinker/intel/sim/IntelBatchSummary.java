package com.antlab.systemthinker.intel.sim;

public final class IntelBatchSummary {
    private final int trials;
    private final long baseSeed;
    private final String seedPolicy;
    private final StatsUtil.SummaryStats totalCostStats;
    private final StatsUtil.SummaryStats overreactionStats;
    private final StatsUtil.SummaryStats missedStats;
    private final StatsUtil.SummaryStats decisionDelayStats;
    private final StatsUtil.SummaryStats decisionAccuracyStats;
    private final BinomialCI.Interval overreactionCI;
    private final BinomialCI.Interval missedCI;
    private final IntelConvergenceMetrics convergenceMetrics;

    public IntelBatchSummary(int trials,
                             long baseSeed,
                             String seedPolicy,
                             StatsUtil.SummaryStats totalCostStats,
                             StatsUtil.SummaryStats overreactionStats,
                             StatsUtil.SummaryStats missedStats,
                             StatsUtil.SummaryStats decisionDelayStats,
                             StatsUtil.SummaryStats decisionAccuracyStats,
                             BinomialCI.Interval overreactionCI,
                             BinomialCI.Interval missedCI,
                             IntelConvergenceMetrics convergenceMetrics) {
        this.trials = trials;
        this.baseSeed = baseSeed;
        this.seedPolicy = seedPolicy;
        this.totalCostStats = totalCostStats;
        this.overreactionStats = overreactionStats;
        this.missedStats = missedStats;
        this.decisionDelayStats = decisionDelayStats;
        this.decisionAccuracyStats = decisionAccuracyStats;
        this.overreactionCI = overreactionCI;
        this.missedCI = missedCI;
        this.convergenceMetrics = convergenceMetrics;
    }

    public int getTrials() {
        return trials;
    }

    public long getBaseSeed() {
        return baseSeed;
    }

    public String getSeedPolicy() {
        return seedPolicy;
    }

    public StatsUtil.SummaryStats getTotalCostStats() {
        return totalCostStats;
    }

    public StatsUtil.SummaryStats getOverreactionStats() {
        return overreactionStats;
    }

    public StatsUtil.SummaryStats getMissedStats() {
        return missedStats;
    }

    public StatsUtil.SummaryStats getDecisionDelayStats() {
        return decisionDelayStats;
    }

    public StatsUtil.SummaryStats getDecisionAccuracyStats() {
        return decisionAccuracyStats;
    }

    public BinomialCI.Interval getOverreactionCI() {
        return overreactionCI;
    }

    public BinomialCI.Interval getMissedCI() {
        return missedCI;
    }

    public IntelConvergenceMetrics getConvergenceMetrics() {
        return convergenceMetrics;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"trials\":").append(trials);
        sb.append(",\"baseSeed\":").append(baseSeed);
        sb.append(",\"seedPolicy\":\"").append(seedPolicy).append("\"");
        sb.append(",\"totalCost\":").append(totalCostStats.toJson());
        sb.append(",\"overreactionRate\":").append(overreactionStats.toJson());
        sb.append(",\"missedThreatRate\":").append(missedStats.toJson());
        sb.append(",\"decisionDelay\":").append(decisionDelayStats.toJson());
        sb.append(",\"decisionAccuracy\":").append(decisionAccuracyStats.toJson());
        sb.append(",\"overreactionCI\":").append(overreactionCI.toJson());
        sb.append(",\"missedCI\":").append(missedCI.toJson());
        sb.append(",\"convergence\":").append(convergenceMetrics.toJson());
        sb.append("}");
        return sb.toString();
    }
}
