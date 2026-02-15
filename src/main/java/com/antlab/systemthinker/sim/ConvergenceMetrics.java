package com.antlab.systemthinker.sim;

public final class ConvergenceMetrics {
    public final boolean converged;
    public final int stabilizedAtTrial;
    public final double epsilon;
    public final int checkInterval;
    public final double lastDelta;

    public ConvergenceMetrics(boolean converged, int stabilizedAtTrial, double epsilon, int checkInterval, double lastDelta) {
        this.converged = converged;
        this.stabilizedAtTrial = stabilizedAtTrial;
        this.epsilon = epsilon;
        this.checkInterval = checkInterval;
        this.lastDelta = lastDelta;
    }
}
