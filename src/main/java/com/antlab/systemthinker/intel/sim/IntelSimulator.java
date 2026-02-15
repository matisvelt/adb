package com.antlab.systemthinker.intel.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class IntelSimulator {
    public IntelTrialResult runTrial(IntelScenarioConfig config, long seed) {
        Random rng = new Random(seed);
        int horizon = config.getTimeHorizon();
        double[][] transition = config.getTransitionMatrix();
        AnalystPolicy policy = config.getPolicy();
        CostWeights costs = config.getCosts();
        double urgency = config.getUrgency();

        double baselineLogit = logit(policy.getBaselineBelief());
        double logit = baselineLogit;

        int maxDelay = config.getSources().stream().mapToInt(SourceConfig::getMaxDelay).max().orElse(0);
        @SuppressWarnings("unchecked")
        List<ReportEvent>[] inbox = new List[horizon + maxDelay + 2];

        TruthState state = TruthState.NO_THREAT;
        TruthState prevState = state;

        int actCount = 0;
        int actDuringNoThreat = 0;
        int activeSteps = 0;
        int missedActiveSteps = 0;
        int investigationCount = 0;
        int activeEpisodes = 0;
        boolean actIssuedForEpisode = false;
        int currentActiveStart = -1;
        List<Integer> decisionDelays = new ArrayList<>();

        double totalCost = 0.0;

        int investigationBoostRemaining = 0;

        for (int t = 1; t <= horizon; t++) {
            prevState = state;
            state = sampleNextState(state, transition, rng);

            if (state == TruthState.ACTIVE) {
                activeSteps++;
                if (prevState != TruthState.ACTIVE) {
                    activeEpisodes++;
                    currentActiveStart = t;
                    actIssuedForEpisode = false;
                }
            }
            if (prevState == TruthState.ACTIVE && state != TruthState.ACTIVE) {
                if (!actIssuedForEpisode && currentActiveStart > 0) {
                    decisionDelays.add(t - currentActiveStart);
                }
                currentActiveStart = -1;
                actIssuedForEpisode = false;
            }

            List<ReportEvent> arrivals = inbox[t];
            if (arrivals == null || arrivals.isEmpty()) {
                logit = logit * policy.getDecayFactor() + baselineLogit * (1.0 - policy.getDecayFactor());
            } else {
                for (ReportEvent event : arrivals) {
                    double weight = config.getSources().get(event.getSourceId()).getWeight();
                    if (event.getSignal() == ReportSignal.THREAT) {
                        logit += weight;
                    } else if (event.getSignal() == ReportSignal.NO_THREAT) {
                        logit -= weight;
                    }
                }
            }

            double belief = logistic(logit);
            double actThreshold = policy.actThreshold(urgency);
            double investigateLow = policy.investigateLow(urgency);
            double investigateHigh = policy.investigateHigh(urgency);

            Action action;
            if (belief >= actThreshold) {
                action = Action.ACT;
            } else if (belief >= investigateLow && belief <= investigateHigh) {
                action = Action.INVESTIGATE;
            } else {
                action = Action.HOLD;
            }

            if (action == Action.ACT) {
                actCount++;
                totalCost += costs.getActCost();
                if (state == TruthState.NO_THREAT) {
                    actDuringNoThreat++;
                    totalCost += costs.getFalsePositiveCost();
                }
                if (state == TruthState.ACTIVE && !actIssuedForEpisode) {
                    actIssuedForEpisode = true;
                    if (currentActiveStart > 0) {
                        decisionDelays.add(t - currentActiveStart);
                    }
                }
            } else if (action == Action.INVESTIGATE) {
                investigationCount++;
                totalCost += costs.getInvestigationCost();
                investigationBoostRemaining = policy.getInvestigationBoostSteps();
            }

            if (state == TruthState.ACTIVE && action != Action.ACT) {
                missedActiveSteps++;
                double urgencyPenalty = 1.0 + urgency * costs.getUrgencyPenaltyMultiplier();
                totalCost += costs.getFalseNegativeCost() * urgencyPenalty;
            }

            boolean investigationActive = investigationBoostRemaining > 0;
            for (SourceConfig source : config.getSources()) {
                if (shouldDropout(source, investigationActive, policy, rng)) {
                    continue;
                }
                ReportSignal signal = drawSignal(source, investigationActive, policy, state, rng);
                int delay = sampleDelay(source, investigationActive, policy, rng);
                int arrival = t + delay;
                if (arrival > horizon) {
                    continue;
                }
                if (inbox[arrival] == null) {
                    inbox[arrival] = new ArrayList<>();
                }
                inbox[arrival].add(new ReportEvent(source.getSourceId(), signal, t, arrival));
            }

            if (investigationBoostRemaining > 0) {
                investigationBoostRemaining--;
            }
        }

        if (state == TruthState.ACTIVE && !actIssuedForEpisode && currentActiveStart > 0) {
            decisionDelays.add(horizon - currentActiveStart + 1);
        }

        double overreactionRate = actCount == 0 ? 0.0 : ((double) actDuringNoThreat) / actCount;
        double missedThreatRate = activeSteps == 0 ? 0.0 : ((double) missedActiveSteps) / activeSteps;
        double meanDecisionDelay = decisionDelays.isEmpty() ? 0.0 : decisionDelays.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double decisionAccuracy = 1.0 / (1.0 + totalCost);

        return new IntelTrialResult(seed, totalCost, overreactionRate, missedThreatRate, meanDecisionDelay,
                decisionAccuracy, actCount, actDuringNoThreat, activeSteps, missedActiveSteps, investigationCount,
                activeEpisodes);
    }

    private TruthState sampleNextState(TruthState current, double[][] transition, Random rng) {
        int idx = current.ordinal();
        double roll = rng.nextDouble();
        double cumulative = 0.0;
        for (int i = 0; i < transition[idx].length; i++) {
            cumulative += transition[idx][i];
            if (roll <= cumulative) {
                return TruthState.values()[i];
            }
        }
        return TruthState.values()[transition[idx].length - 1];
    }

    private boolean shouldDropout(SourceConfig source, boolean investigationActive, AnalystPolicy policy, Random rng) {
        double dropout = source.getDropout();
        if (investigationActive) {
            dropout = clamp01(dropout * policy.getInvestigationDropoutMultiplier());
        }
        return rng.nextDouble() < dropout;
    }

    private ReportSignal drawSignal(SourceConfig source, boolean investigationActive, AnalystPolicy policy,
                                    TruthState truth, Random rng) {
        double sensitivity = source.getSensitivity();
        double specificity = source.getSpecificity();
        if (investigationActive) {
            sensitivity = clamp01(sensitivity + policy.getInvestigationSensitivityBoost());
            specificity = clamp01(specificity + policy.getInvestigationSpecificityBoost());
        }
        double bias = source.getBias();
        double threatProb;
        if (truth == TruthState.NO_THREAT) {
            threatProb = clamp01(1.0 - specificity + bias);
        } else {
            threatProb = clamp01(sensitivity + bias);
        }
        if (rng.nextDouble() < threatProb) {
            return ReportSignal.THREAT;
        }
        return ReportSignal.NO_THREAT;
    }

    private int sampleDelay(SourceConfig source, boolean investigationActive, AnalystPolicy policy, Random rng) {
        double mean = source.getDelayMean();
        if (investigationActive) {
            mean = Math.max(0.0, mean * policy.getInvestigationDelayMultiplier());
        }
        if (mean <= 0.0 || source.getMaxDelay() == 0) {
            return 0;
        }
        double p = 1.0 / (mean + 1.0);
        double cumulative = 0.0;
        double roll = rng.nextDouble();
        for (int d = 0; d < source.getMaxDelay(); d++) {
            double prob = Math.pow(1.0 - p, d) * p;
            cumulative += prob;
            if (roll <= cumulative) {
                return d;
            }
        }
        return source.getMaxDelay();
    }

    private double logit(double p) {
        double clamped = clamp01(p);
        if (clamped <= 0.000001) {
            return -13.8;
        }
        if (clamped >= 0.999999) {
            return 13.8;
        }
        return Math.log(clamped / (1.0 - clamped));
    }

    private double logistic(double l) {
        return 1.0 / (1.0 + Math.exp(-l));
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private enum Action {
        ACT,
        INVESTIGATE,
        HOLD
    }
}
