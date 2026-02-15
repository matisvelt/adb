package com.antlab.systemthinker.sim;

import java.util.logging.Logger;

public class MonteCarloRunner {
    public static final double DEFAULT_EPSILON = 0.005;
    public static final int DEFAULT_CHECK_INTERVAL = 100;
    public static final int DEFAULT_STABLE_CHECKS = 3;

    private final AntWarSimulator simulator;
    private final Logger logger;
    private final double epsilon;
    private final int checkInterval;
    private final int stableChecksRequired;

    public MonteCarloRunner() {
        this(new AntWarSimulator(), Logger.getLogger(MonteCarloRunner.class.getName()),
                DEFAULT_EPSILON, DEFAULT_CHECK_INTERVAL, DEFAULT_STABLE_CHECKS);
    }

    public MonteCarloRunner(AntWarSimulator simulator, Logger logger,
                             double epsilon, int checkInterval, int stableChecksRequired) {
        this.simulator = simulator;
        this.logger = logger;
        this.epsilon = epsilon;
        this.checkInterval = checkInterval;
        this.stableChecksRequired = stableChecksRequired;
    }

    public MonteCarloSummary runBatch(ScenarioParameters template, int trials) {
        long baseSeed = template.seed;
        int winsA = 0;
        int winsB = 0;
        int draws = 0;

        double meanTime = 0.0;
        double m2Time = 0.0;
        double meanCasualty = 0.0;

        double lastEstimate = Double.NaN;
        int stableChecks = 0;
        int stabilizedAt = -1;
        double lastDelta = Double.NaN;

        int trialsRun = 0;

        for (int i = 0; i < trials; i++) {
            ScenarioParameters params = template.withSeed(baseSeed + i);
            SimulationResult result = simulator.run(params);
            trialsRun++;

            if (result.winner == SimulationResult.Winner.A) {
                winsA++;
            } else if (result.winner == SimulationResult.Winner.B) {
                winsB++;
            } else {
                draws++;
            }

            double delta = result.timeToDecision - meanTime;
            meanTime += delta / trialsRun;
            m2Time += delta * (result.timeToDecision - meanTime);

            meanCasualty += (result.casualtyRatio - meanCasualty) / trialsRun;

            if (trialsRun % checkInterval == 0) {
                double estimate = (double) winsA / trialsRun;
                if (!Double.isNaN(lastEstimate)) {
                    lastDelta = Math.abs(estimate - lastEstimate);
                    if (lastDelta < epsilon) {
                        stableChecks++;
                    } else {
                        stableChecks = 0;
                    }
                }
                lastEstimate = estimate;

                logger.info("Convergence check @" + trialsRun + ": pA=" + estimate + " delta=" + lastDelta);

                if (stableChecksRequired > 0 && stableChecks >= stableChecksRequired) {
                    stabilizedAt = trialsRun;
                    break;
                }
            }
        }

        double varianceTime = trialsRun > 1 ? (m2Time / (trialsRun - 1)) : 0.0;
        double stdTime = Math.sqrt(Math.max(0.0, varianceTime));

        double pA = trialsRun == 0 ? 0 : (double) winsA / trialsRun;
        double pB = trialsRun == 0 ? 0 : (double) winsB / trialsRun;
        double pD = trialsRun == 0 ? 0 : (double) draws / trialsRun;

        double[] ci = wilsonInterval(winsA, trialsRun, 1.96);

        ConvergenceMetrics convergence = new ConvergenceMetrics(
                stabilizedAt > 0,
                stabilizedAt,
                epsilon,
                checkInterval,
                lastDelta
        );

        return new MonteCarloSummary(
                trials,
                trialsRun,
                baseSeed,
                pA,
                pB,
                pD,
                meanTime,
                stdTime,
                meanCasualty,
                ci[0],
                ci[1],
                convergence
        );
    }

    private double[] wilsonInterval(int successes, int n, double z) {
        if (n == 0) {
            return new double[]{0.0, 0.0};
        }
        double phat = (double) successes / n;
        double z2 = z * z;
        double denom = 1.0 + z2 / n;
        double center = (phat + z2 / (2.0 * n)) / denom;
        double margin = z * Math.sqrt((phat * (1 - phat) + z2 / (4.0 * n)) / n) / denom;
        return new double[]{Math.max(0, center - margin), Math.min(1, center + margin)};
    }
}
