package com.antlab.systemthinker.sim;

public final class IntRange {
    public final int min;
    public final int max;

    public IntRange(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("max must be >= min");
        }
        this.min = min;
        this.max = max;
    }

    public int sample(java.util.Random random) {
        if (min == max) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }
}
