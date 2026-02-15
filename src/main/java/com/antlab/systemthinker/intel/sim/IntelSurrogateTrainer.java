package com.antlab.systemthinker.intel.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class IntelSurrogateTrainer {
    private IntelSurrogateTrainer() {}

    public static IntelSurrogateModel train(IntelScenarioConfig baseConfig,
                                            IntelDatasetConfig datasetConfig,
                                            int scenarios,
                                            int trialsPerScenario,
                                            double lambda) {
        Random rng = new Random(datasetConfig.getBaseSeed());
        List<double[]> features = new ArrayList<>();
        List<Double> costTargets = new ArrayList<>();
        List<Double> overTargets = new ArrayList<>();
        List<Double> missedTargets = new ArrayList<>();
        List<Double> delayTargets = new ArrayList<>();

        MonteCarloRunnerIntel runner = new MonteCarloRunnerIntel(new IntelSimulator(),
                MonteCarloRunnerIntel.DEFAULT_EPSILON,
                MonteCarloRunnerIntel.DEFAULT_CHECK_INTERVAL,
                MonteCarloRunnerIntel.DEFAULT_STABLE_CHECKS);

        for (int i = 0; i < scenarios; i++) {
            double sensitivity = datasetConfig.getSensitivityRange().sample(rng);
            double specificity = datasetConfig.getSpecificityRange().sample(rng);
            double dropout = datasetConfig.getDropoutRange().sample(rng);
            double urgency = datasetConfig.getUrgencyRange().sample(rng);
            double fpCost = datasetConfig.getFalsePositiveCostRange().sample(rng);
            double fnCost = datasetConfig.getFalseNegativeCostRange().sample(rng);
            double actThreshold = datasetConfig.getActThresholdRange().sample(rng);

            IntelScenarioConfig scenario = applyScenario(baseConfig, sensitivity, specificity, dropout, urgency,
                    fpCost, fnCost, actThreshold);
            IntelBatchSummary summary = runner.runBatch(scenario, trialsPerScenario,
                    datasetConfig.getBaseSeed() + i * 1000L, MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);

            features.add(extractFeatures(scenario));
            costTargets.add(summary.getTotalCostStats().mean());
            overTargets.add(summary.getOverreactionStats().mean());
            missedTargets.add(summary.getMissedStats().mean());
            delayTargets.add(summary.getDecisionDelayStats().mean());
        }

        int split = Math.max(1, (int) Math.floor(features.size() * 0.8));
        List<double[]> trainX = features.subList(0, split);
        List<Double> trainCost = costTargets.subList(0, split);
        List<Double> trainOver = overTargets.subList(0, split);
        List<Double> trainMissed = missedTargets.subList(0, split);
        List<Double> trainDelay = delayTargets.subList(0, split);

        RidgeRegression.Model costModel = RidgeRegression.train(trainX, trainCost, lambda);
        RidgeRegression.Model overModel = RidgeRegression.train(trainX, trainOver, lambda);
        RidgeRegression.Model missedModel = RidgeRegression.train(trainX, trainMissed, lambda);
        RidgeRegression.Model delayModel = RidgeRegression.train(trainX, trainDelay, lambda);

        double maeCost = mae(costModel, features, costTargets, split);
        double maeOver = mae(overModel, features, overTargets, split);
        double maeMissed = mae(missedModel, features, missedTargets, split);
        double maeDelay = mae(delayModel, features, delayTargets, split);

        return new IntelSurrogateModel(costModel, overModel, missedModel, delayModel,
                maeCost, maeOver, maeMissed, maeDelay);
    }

    public static double[] extractFeatures(IntelScenarioConfig config) {
        double sensitivity = 0.0;
        double specificity = 0.0;
        double dropout = 0.0;
        double delayMean = 0.0;
        double weight = 0.0;
        int count = config.getSources().size();
        for (SourceConfig source : config.getSources()) {
            sensitivity += source.getSensitivity();
            specificity += source.getSpecificity();
            dropout += source.getDropout();
            delayMean += source.getDelayMean();
            weight += source.getWeight();
        }
        if (count > 0) {
            sensitivity /= count;
            specificity /= count;
            dropout /= count;
            delayMean /= count;
            weight /= count;
        }
        return new double[] {
                sensitivity,
                specificity,
                dropout,
                delayMean,
                weight,
                config.getUrgency(),
                config.getPolicy().getActThresholdBase(),
                config.getCosts().getFalsePositiveCost(),
                config.getCosts().getFalseNegativeCost(),
                config.getCosts().getInvestigationCost()
        };
    }

    private static double mae(RidgeRegression.Model model, List<double[]> features, List<Double> targets, int split) {
        if (features.size() <= split) {
            return 0.0;
        }
        double sum = 0.0;
        int count = 0;
        for (int i = split; i < features.size(); i++) {
            double pred = model.predict(features.get(i));
            sum += Math.abs(pred - targets.get(i));
            count++;
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private static IntelScenarioConfig applyScenario(IntelScenarioConfig baseConfig,
                                                     double sensitivity,
                                                     double specificity,
                                                     double dropout,
                                                     double urgency,
                                                     double fpCost,
                                                     double fnCost,
                                                     double actThreshold) {
        List<SourceConfig> updated = new ArrayList<>();
        for (SourceConfig source : baseConfig.getSources()) {
            updated.add(source.withSensitivity(sensitivity).withSpecificity(specificity).withDropout(dropout));
        }
        AnalystPolicy basePolicy = baseConfig.getPolicy();
        AnalystPolicy policy = new AnalystPolicy(
                basePolicy.getBaselineBelief(),
                basePolicy.getDecayFactor(),
                actThreshold,
                basePolicy.getInvestigateLow(),
                basePolicy.getInvestigateHigh(),
                basePolicy.getUrgencyActShift(),
                basePolicy.getUrgencyInvestigateShift(),
                basePolicy.getInvestigationBoostSteps(),
                basePolicy.getInvestigationSensitivityBoost(),
                basePolicy.getInvestigationSpecificityBoost(),
                basePolicy.getInvestigationDropoutMultiplier(),
                basePolicy.getInvestigationDelayMultiplier()
        );
        CostWeights baseCosts = baseConfig.getCosts();
        CostWeights costs = new CostWeights(fpCost, fnCost, baseCosts.getInvestigationCost(), baseCosts.getActCost(),
                baseCosts.getUrgencyPenaltyMultiplier());
        return new IntelScenarioConfig(baseConfig.getTimeHorizon(), baseConfig.getTransitionMatrix(), updated, policy,
                costs, urgency);
    }
}
