package com.antlab.systemthinker.intel.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StatsUtil {
    private StatsUtil() {}

    public static double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    public static double stddev(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double d = v - mean;
            sum += d * d;
        }
        return Math.sqrt(sum / (values.size() - 1));
    }

    public static double quantile(List<Double> values, double q) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double pos = q * (sorted.size() - 1);
        int idx = (int) Math.floor(pos);
        int idx2 = Math.min(idx + 1, sorted.size() - 1);
        double frac = pos - idx;
        return sorted.get(idx) * (1.0 - frac) + sorted.get(idx2) * frac;
    }

    public static SummaryStats summarize(List<Double> values) {
        double mean = mean(values);
        double std = stddev(values, mean);
        double p5 = quantile(values, 0.05);
        double p50 = quantile(values, 0.50);
        double p95 = quantile(values, 0.95);
        return new SummaryStats(mean, std, p5, p50, p95);
    }

    public record SummaryStats(double mean, double std, double p5, double p50, double p95) {
        public String toJson() {
            return String.format("{\"mean\":%.6f,\"std\":%.6f,\"p5\":%.6f,\"p50\":%.6f,\"p95\":%.6f}",
                    mean, std, p5, p50, p95);
        }
    }
}
