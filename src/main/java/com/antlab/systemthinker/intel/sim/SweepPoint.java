package com.antlab.systemthinker.intel.sim;

public record SweepPoint(double xValue, double meanTotalCost, double meanOverreaction, double meanMissed) {
    public String toCsv() {
        return String.format("%.6f,%.6f,%.6f,%.6f", xValue, meanTotalCost, meanOverreaction, meanMissed);
    }
}
