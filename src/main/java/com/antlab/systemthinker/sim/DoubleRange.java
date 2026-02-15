package com.antlab.systemthinker.sim;

public final class DoubleRange {
    public final double min;
    public final double max;

    public DoubleRange(double min, double max) {
        if (max < min) {
            throw new IllegalArgumentException("max must be >= min");
        }
        this.min = min;
        this.max = max;
    }

    public double sample(java.util.Random random) {
        if (min == max) {
            return min;
        }
        return min + random.nextDouble() * (max - min);
    }
}
