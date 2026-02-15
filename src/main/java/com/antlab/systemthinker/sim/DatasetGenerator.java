package com.antlab.systemthinker.sim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;
import java.util.logging.Logger;

public class DatasetGenerator {
    private final MonteCarloRunner runner;
    private final Logger logger;

    public DatasetGenerator(MonteCarloRunner runner, Logger logger) {
        this.runner = runner;
        this.logger = logger;
    }

    public Path generate(DatasetConfig config) throws IOException {
        Path baseDir = Path.of(System.getProperty("user.home"), "SystemThinker", "datasets");
        Files.createDirectories(baseDir);
        String timestamp = Instant.now().toString().replace(":", "");
        Path csvPath = baseDir.resolve("dataset-" + timestamp + ".csv");
        Path metaPath = baseDir.resolve("dataset-" + timestamp + ".meta.json");

        String header = String.join(",",
                "colonyA",
                "colonyB",
                "spawnA",
                "spawnB",
                "resourceDensity",
                "movementBias",
                "detectionRadius",
                "lethality",
                "winProbabilityA",
                "meanTime",
                "casualtyRatio"
        );

        StringBuilder csv = new StringBuilder();
        csv.append(header).append("\n");

        Random random = new Random(config.baseSeed);

        for (int i = 0; i < config.scenarios; i++) {
            int colonyA = config.colonyARange.sample(random);
            int colonyB = config.colonyBRange.sample(random);
            int gridW = config.gridWidthRange.sample(random);
            int gridH = config.gridHeightRange.sample(random);
            double spawnA = config.spawnARange.sample(random);
            double spawnB = config.spawnBRange.sample(random);
            double resourceDensity = config.resourceDensityRange.sample(random);
            double movementBias = config.movementBiasRange.sample(random);
            int detectionRadius = config.detectionRadiusRange.sample(random);
            double lethality = config.lethalityRange.sample(random);
            int maxSteps = config.maxStepsRange.sample(random);
            double threshold = config.territoryDecisionThresholdRange.sample(random);
            int streak = config.decisionStreakRange.sample(random);

            long scenarioSeed = config.baseSeed + (i * 100_000L);

            ScenarioParameters params = new ScenarioParameters(
                    gridW,
                    gridH,
                    colonyA,
                    colonyB,
                    spawnA,
                    spawnB,
                    resourceDensity,
                    movementBias,
                    detectionRadius,
                    lethality,
                    maxSteps,
                    scenarioSeed,
                    threshold,
                    streak
            );

            logger.info("Dataset scenario " + (i + 1) + "/" + config.scenarios + " params=" + params);

            MonteCarloSummary summary = runner.runBatch(params, config.trialsPerScenario);

            csv.append(colonyA).append(',')
                    .append(colonyB).append(',')
                    .append(spawnA).append(',')
                    .append(spawnB).append(',')
                    .append(resourceDensity).append(',')
                    .append(movementBias).append(',')
                    .append(detectionRadius).append(',')
                    .append(lethality).append(',')
                    .append(summary.winProbabilityA).append(',')
                    .append(summary.meanTimeToDecision).append(',')
                    .append(summary.meanCasualtyRatio)
                    .append('\n');
        }

        Files.writeString(csvPath, csv.toString());
        Files.writeString(metaPath, buildMetadata(config));

        return csvPath;
    }

    private String buildMetadata(DatasetConfig config) {
        return "{" +
                "\"generatedAt\":\"" + Instant.now() + "\"," +
                "\"versionHash\":\"" + VersionUtil.getVersionHash() + "\"," +
                "\"scenarios\":" + config.scenarios + "," +
                "\"trialsPerScenario\":" + config.trialsPerScenario + "," +
                "\"baseSeed\":" + config.baseSeed +
                "}";
    }
}
