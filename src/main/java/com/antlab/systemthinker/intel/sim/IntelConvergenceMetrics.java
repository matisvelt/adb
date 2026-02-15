package com.antlab.systemthinker.intel.sim;

public final class IntelConvergenceMetrics {
    private final boolean converged;
    private final int trialsAtConvergence;
    private final double epsilon;
    private final int checkInterval;
    private final int stableChecks;
    private final double lastWidthOverreaction;
    private final double lastWidthMissed;

    public IntelConvergenceMetrics(boolean converged,
                                   int trialsAtConvergence,
                                   double epsilon,
                                   int checkInterval,
                                   int stableChecks,
                                   double lastWidthOverreaction,
                                   double lastWidthMissed) {
        this.converged = converged;
        this.trialsAtConvergence = trialsAtConvergence;
        this.epsilon = epsilon;
        this.checkInterval = checkInterval;
        this.stableChecks = stableChecks;
        this.lastWidthOverreaction = lastWidthOverreaction;
        this.lastWidthMissed = lastWidthMissed;
    }

    public boolean isConverged() {
        return converged;
    }

    public int getTrialsAtConvergence() {
        return trialsAtConvergence;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public int getStableChecks() {
        return stableChecks;
    }

    public double getLastWidthOverreaction() {
        return lastWidthOverreaction;
    }

    public double getLastWidthMissed() {
        return lastWidthMissed;
    }

    public String toJson() {
        return String.format("{\"converged\":%s,\"trialsAtConvergence\":%d,\"epsilon\":%.6f,\"checkInterval\":%d,\"stableChecks\":%d,\"lastWidthOverreaction\":%.6f,\"lastWidthMissed\":%.6f}",
                converged ? "true" : "false", trialsAtConvergence, epsilon, checkInterval, stableChecks,
                lastWidthOverreaction, lastWidthMissed);
    }
}
