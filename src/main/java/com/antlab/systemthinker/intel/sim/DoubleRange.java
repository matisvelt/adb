package com.antlab.systemthinker.intel.sim;

import java.util.Random;

public final class DoubleRange {
    private final double min;
    private final double max;

    public DoubleRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double sample(Random rng) {
        return min + rng.nextDouble() * (max - min);
    }

    public String toJson() {
        return String.format("{\"min\":%.5f,\"max\":%.5f}", min, max);
    }
}
