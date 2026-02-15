package com.antlab.systemthinker.intel.sim;

public final class BinomialCI {
    private BinomialCI() {}

    public static Interval wilson(int successes, int trials, double z) {
        if (trials == 0) {
            return new Interval(0.0, 0.0);
        }
        double n = trials;
        double phat = successes / n;
        double z2 = z * z;
        double denom = 1.0 + z2 / n;
        double center = (phat + z2 / (2.0 * n)) / denom;
        double margin = z * Math.sqrt((phat * (1.0 - phat) + z2 / (4.0 * n)) / n) / denom;
        double lower = Math.max(0.0, center - margin);
        double upper = Math.min(1.0, center + margin);
        return new Interval(lower, upper);
    }

    public record Interval(double lower, double upper) {
        public String toJson() {
            return String.format("{\"lower\":%.6f,\"upper\":%.6f}", lower, upper);
        }
    }
}
