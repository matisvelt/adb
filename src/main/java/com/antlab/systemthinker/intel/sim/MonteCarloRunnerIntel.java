package com.antlab.systemthinker.intel.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class MonteCarloRunnerIntel {
    public static final double DEFAULT_EPSILON = 0.01;
    public static final int DEFAULT_CHECK_INTERVAL = 200;
    public static final int DEFAULT_STABLE_CHECKS = 3;

    private final IntelSimulator simulator;
    private final double epsilon;
    private final int checkInterval;
    private final int stableChecks;

    public MonteCarloRunnerIntel(IntelSimulator simulator, double epsilon, int checkInterval, int stableChecks) {
        this.simulator = simulator;
        this.epsilon = epsilon;
        this.checkInterval = checkInterval;
        this.stableChecks = stableChecks;
    }

    public IntelBatchSummary runBatch(IntelScenarioConfig config,
                                      int trials,
                                      long baseSeed,
                                      SeedPolicy seedPolicy,
                                      ProgressListener listener,
                                      BooleanSupplier cancelCheck) {
        List<Double> totalCosts = new ArrayList<>();
        List<Double> overreactionRates = new ArrayList<>();
        List<Double> missedRates = new ArrayList<>();
        List<Double> decisionDelays = new ArrayList<>();
        List<Double> decisionAccuracy = new ArrayList<>();

        int totalAct = 0;
        int totalActNoThreat = 0;
        int totalActiveSteps = 0;
        int totalMissedSteps = 0;

        long start = System.nanoTime();
        int stableCount = 0;
        double lastWidthOver = 1.0;
        double lastWidthMissed = 1.0;
        int completed = 0;

        for (int i = 0; i < trials; i++) {
            if (cancelCheck != null && cancelCheck.getAsBoolean()) {
                break;
            }
            long seed = seedPolicy.seedFor(baseSeed, i);
            IntelTrialResult result = simulator.runTrial(config, seed);
            completed++;

            totalCosts.add(result.getTotalCost());
            overreactionRates.add(result.getOverreactionRate());
            missedRates.add(result.getMissedThreatRate());
            decisionDelays.add(result.getMeanDecisionDelay());
            decisionAccuracy.add(result.getDecisionAccuracy());

            totalAct += result.getActCount();
            totalActNoThreat += result.getActDuringNoThreat();
            totalActiveSteps += result.getActiveSteps();
            totalMissedSteps += result.getMissedActiveSteps();

            if (listener != null) {
                double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
                double rate = seconds > 0 ? completed / seconds : 0.0;
                listener.onProgress(completed, trials, rate);
            }

            if (completed % checkInterval == 0) {
                BinomialCI.Interval overCI = BinomialCI.wilson(totalActNoThreat, Math.max(1, totalAct), 1.96);
                BinomialCI.Interval missCI = BinomialCI.wilson(totalMissedSteps, Math.max(1, totalActiveSteps), 1.96);
                lastWidthOver = overCI.upper() - overCI.lower();
                lastWidthMissed = missCI.upper() - missCI.lower();
                if (lastWidthOver < epsilon && lastWidthMissed < epsilon) {
                    stableCount++;
                } else {
                    stableCount = 0;
                }
                if (stableCount >= stableChecks) {
                    break;
                }
            }
        }

        StatsUtil.SummaryStats totalCostStats = StatsUtil.summarize(totalCosts);
        StatsUtil.SummaryStats overStats = StatsUtil.summarize(overreactionRates);
        StatsUtil.SummaryStats missStats = StatsUtil.summarize(missedRates);
        StatsUtil.SummaryStats delayStats = StatsUtil.summarize(decisionDelays);
        StatsUtil.SummaryStats accuracyStats = StatsUtil.summarize(decisionAccuracy);

        BinomialCI.Interval overCI = BinomialCI.wilson(totalActNoThreat, Math.max(1, totalAct), 1.96);
        BinomialCI.Interval missCI = BinomialCI.wilson(totalMissedSteps, Math.max(1, totalActiveSteps), 1.96);

        IntelConvergenceMetrics convergence = new IntelConvergenceMetrics(
                stableCount >= stableChecks,
                completed,
                epsilon,
                checkInterval,
                stableChecks,
                lastWidthOver,
                lastWidthMissed
        );

        return new IntelBatchSummary(completed, baseSeed, seedPolicy.name(), totalCostStats, overStats, missStats,
                delayStats, accuracyStats, overCI, missCI, convergence);
    }

    public enum SeedPolicy {
        SEQUENTIAL {
            @Override
            public long seedFor(long base, int index) {
                return base + index;
            }
        },
        HASHED {
            @Override
            public long seedFor(long base, int index) {
                long x = base ^ (index * 0x9E3779B97F4A7C15L);
                x ^= (x >>> 33);
                x *= 0xff51afd7ed558ccdL;
                x ^= (x >>> 33);
                x *= 0xc4ceb9fe1a85ec53L;
                x ^= (x >>> 33);
                return x;
            }
        };

        public abstract long seedFor(long base, int index);
    }

    public interface ProgressListener {
        void onProgress(int completed, int total, double trialsPerSecond);
    }
}
