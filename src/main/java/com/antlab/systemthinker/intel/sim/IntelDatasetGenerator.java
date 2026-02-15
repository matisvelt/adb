package com.antlab.systemthinker.intel.sim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class IntelDatasetGenerator {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final MonteCarloRunnerIntel runner;
    private final ExperimentWriter writer;

    public IntelDatasetGenerator(MonteCarloRunnerIntel runner, ExperimentWriter writer) {
        this.runner = runner;
        this.writer = writer;
    }

    public Path generateDataset(IntelScenarioConfig baseConfig, IntelDatasetConfig datasetConfig) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.home"), "SystemThinker", "datasets");
        Files.createDirectories(baseDir);
        Path csv = baseDir.resolve("intel-dataset-" + TS_FORMAT.format(LocalDateTime.now()) + ".csv");

        StringBuilder sb = new StringBuilder();
        sb.append("sensitivity,specificity,dropout,urgency,actThreshold,falsePositiveCost,falseNegativeCost,meanTotalCost,overreactionRate,missedThreatRate,meanDecisionDelay\n");

        Random rng = new Random(datasetConfig.getBaseSeed());
        for (int i = 0; i < datasetConfig.getScenarios(); i++) {
            double sensitivity = datasetConfig.getSensitivityRange().sample(rng);
            double specificity = datasetConfig.getSpecificityRange().sample(rng);
            double dropout = datasetConfig.getDropoutRange().sample(rng);
            double urgency = datasetConfig.getUrgencyRange().sample(rng);
            double fpCost = datasetConfig.getFalsePositiveCostRange().sample(rng);
            double fnCost = datasetConfig.getFalseNegativeCostRange().sample(rng);
            double actThreshold = datasetConfig.getActThresholdRange().sample(rng);

            IntelScenarioConfig scenario = applyScenario(baseConfig, sensitivity, specificity, dropout, urgency,
                    fpCost, fnCost, actThreshold);
            IntelBatchSummary summary = runner.runBatch(scenario, datasetConfig.getTrialsPerScenario(),
                    datasetConfig.getBaseSeed() + i * 1000L, MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);

            sb.append(String.format("%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f\n",
                    sensitivity, specificity, dropout, urgency, actThreshold, fpCost, fnCost,
                    summary.getTotalCostStats().mean(), summary.getOverreactionStats().mean(),
                    summary.getMissedStats().mean(), summary.getDecisionDelayStats().mean()));
        }

        Files.writeString(csv, sb.toString());
        writer.writeDataset(csv, datasetMetadataJson(baseConfig, datasetConfig));
        return csv;
    }

    private IntelScenarioConfig applyScenario(IntelScenarioConfig baseConfig,
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

    private String datasetMetadataJson(IntelScenarioConfig baseConfig, IntelDatasetConfig datasetConfig) {
        return "{\"baseConfig\":" + baseConfig.toJson() + ",\"datasetConfig\":" + datasetConfig.toJson() + "}";
    }
}
