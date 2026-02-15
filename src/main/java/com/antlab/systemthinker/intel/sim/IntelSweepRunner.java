package com.antlab.systemthinker.intel.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class IntelSweepRunner {
    private final MonteCarloRunnerIntel runner;

    public IntelSweepRunner(MonteCarloRunnerIntel runner) {
        this.runner = runner;
    }

    public SweepSeries runSpecificitySweep(IntelScenarioConfig baseConfig,
                                           double specificityStart,
                                           double specificityEnd,
                                           int points,
                                           int trialsPerPoint,
                                           long baseSeed,
                                           MonteCarloRunnerIntel.SeedPolicy seedPolicy,
                                           MonteCarloRunnerIntel.ProgressListener listener,
                                           BooleanSupplier cancelCheck,
                                           SweepProgressListener sweepProgress) {
        return runSweep(baseConfig, SweepType.SPECIFICITY, specificityStart, specificityEnd, points,
                trialsPerPoint, baseSeed, seedPolicy, listener, cancelCheck, sweepProgress);
    }

    public SweepSeries runSensitivitySweep(IntelScenarioConfig baseConfig,
                                           double sensitivityStart,
                                           double sensitivityEnd,
                                           int points,
                                           int trialsPerPoint,
                                           long baseSeed,
                                           MonteCarloRunnerIntel.SeedPolicy seedPolicy,
                                           MonteCarloRunnerIntel.ProgressListener listener,
                                           BooleanSupplier cancelCheck,
                                           SweepProgressListener sweepProgress) {
        return runSweep(baseConfig, SweepType.SENSITIVITY, sensitivityStart, sensitivityEnd, points,
                trialsPerPoint, baseSeed, seedPolicy, listener, cancelCheck, sweepProgress);
    }

    private SweepSeries runSweep(IntelScenarioConfig baseConfig,
                                 SweepType type,
                                 double start,
                                 double end,
                                 int points,
                                 int trialsPerPoint,
                                 long baseSeed,
                                 MonteCarloRunnerIntel.SeedPolicy seedPolicy,
                                 MonteCarloRunnerIntel.ProgressListener listener,
                                 BooleanSupplier cancelCheck,
                                 SweepProgressListener sweepProgress) {
        List<SweepPoint> results = new ArrayList<>();
        int totalSteps = Math.max(2, points);
        for (int i = 0; i < totalSteps; i++) {
            if (cancelCheck != null && cancelCheck.getAsBoolean()) {
                break;
            }
            double fraction = totalSteps == 1 ? 0.0 : ((double) i) / (totalSteps - 1);
            double value = start + (end - start) * fraction;
            IntelScenarioConfig adjusted = adjustConfig(baseConfig, type, value);
            IntelBatchSummary summary = runner.runBatch(adjusted, trialsPerPoint, baseSeed + i * 1000L,
                    seedPolicy, listener, cancelCheck);
            double x = type == SweepType.SPECIFICITY ? 1.0 - value : 1.0 - value;
            results.add(new SweepPoint(x, summary.getTotalCostStats().mean(),
                    summary.getOverreactionStats().mean(), summary.getMissedStats().mean()));
            if (sweepProgress != null) {
                sweepProgress.onPointComplete(i + 1, totalSteps);
            }
        }
        String label = type == SweepType.SPECIFICITY ? "False Positive Rate" : "Missed Detection Rate";
        return new SweepSeries(label, results);
    }

    private IntelScenarioConfig adjustConfig(IntelScenarioConfig config, SweepType type, double value) {
        List<SourceConfig> updated = new ArrayList<>();
        for (SourceConfig source : config.getSources()) {
            if (type == SweepType.SPECIFICITY) {
                updated.add(source.withSpecificity(value));
            } else {
                updated.add(source.withSensitivity(value));
            }
        }
        return config.withSources(updated);
    }

    private enum SweepType {
        SPECIFICITY,
        SENSITIVITY
    }

    public interface SweepProgressListener {
        void onPointComplete(int completedPoints, int totalPoints);
    }
}
