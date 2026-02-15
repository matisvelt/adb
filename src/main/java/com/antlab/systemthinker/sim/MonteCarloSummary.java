package com.antlab.systemthinker.sim;

public final class MonteCarloSummary {
    public final int trialsRequested;
    public final int trialsRun;
    public final long baseSeed;
    public final double winProbabilityA;
    public final double winProbabilityB;
    public final double winProbabilityDraw;
    public final double meanTimeToDecision;
    public final double standardDeviationTime;
    public final double meanCasualtyRatio;
    public final double confidenceLow;
    public final double confidenceHigh;
    public final ConvergenceMetrics convergenceMetrics;

    public MonteCarloSummary(
            int trialsRequested,
            int trialsRun,
            long baseSeed,
            double winProbabilityA,
            double winProbabilityB,
            double winProbabilityDraw,
            double meanTimeToDecision,
            double standardDeviationTime,
            double meanCasualtyRatio,
            double confidenceLow,
            double confidenceHigh,
            ConvergenceMetrics convergenceMetrics
    ) {
        this.trialsRequested = trialsRequested;
        this.trialsRun = trialsRun;
        this.baseSeed = baseSeed;
        this.winProbabilityA = winProbabilityA;
        this.winProbabilityB = winProbabilityB;
        this.winProbabilityDraw = winProbabilityDraw;
        this.meanTimeToDecision = meanTimeToDecision;
        this.standardDeviationTime = standardDeviationTime;
        this.meanCasualtyRatio = meanCasualtyRatio;
        this.confidenceLow = confidenceLow;
        this.confidenceHigh = confidenceHigh;
        this.convergenceMetrics = convergenceMetrics;
    }

    public String toJson() {
        return "{" +
                "\"trialsRequested\":" + trialsRequested + "," +
                "\"trialsRun\":" + trialsRun + "," +
                "\"baseSeed\":" + baseSeed + "," +
                "\"winProbabilityA\":" + winProbabilityA + "," +
                "\"winProbabilityB\":" + winProbabilityB + "," +
                "\"winProbabilityDraw\":" + winProbabilityDraw + "," +
                "\"meanTimeToDecision\":" + meanTimeToDecision + "," +
                "\"standardDeviationTime\":" + standardDeviationTime + "," +
                "\"meanCasualtyRatio\":" + meanCasualtyRatio + "," +
                "\"confidenceLow\":" + confidenceLow + "," +
                "\"confidenceHigh\":" + confidenceHigh + "," +
                "\"converged\":" + convergenceMetrics.converged + "," +
                "\"stabilizedAtTrial\":" + convergenceMetrics.stabilizedAtTrial +
                "}";
    }
}
