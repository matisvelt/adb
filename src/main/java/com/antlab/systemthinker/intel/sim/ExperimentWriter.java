package com.antlab.systemthinker.intel.sim;

import com.antlab.systemthinker.sim.VersionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExperimentWriter {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public Path writeBatchArtifacts(IntelScenarioConfig config,
                                    IntelBatchSummary summary,
                                    String seedPolicy) throws IOException {
        Path dir = createRunDir("batch");
        Files.writeString(dir.resolve("config.json"), config.toJson());
        Files.writeString(dir.resolve("summary.json"), summary.toJson());
        Files.writeString(dir.resolve("app_version.txt"), VersionUtil.getVersionHash());
        Files.writeString(dir.resolve("seed_policy.txt"), seedPolicy);
        return dir;
    }

    public Path writeSweepArtifacts(IntelScenarioConfig config,
                                    SweepSeries baseline,
                                    SweepSeries urgency,
                                    String sweepName) throws IOException {
        Path dir = createRunDir("sweep-" + sweepName);
        Files.writeString(dir.resolve("config.json"), config.toJson());
        Files.writeString(dir.resolve("app_version.txt"), VersionUtil.getVersionHash());
        Files.writeString(dir.resolve("seed_policy.txt"), "sweep");
        Files.writeString(dir.resolve("sweep.csv"), toSweepCsv(baseline, urgency));
        return dir;
    }

    public Path writeDataset(Path csvPath, String metadataJson) throws IOException {
        Path dir = csvPath.getParent();
        if (dir != null) {
            Files.writeString(dir.resolve("dataset_metadata.json"), metadataJson);
            Files.writeString(dir.resolve("app_version.txt"), VersionUtil.getVersionHash());
        }
        return csvPath;
    }

    private Path createRunDir(String prefix) throws IOException {
        Path base = Path.of(System.getProperty("user.home"), "SystemThinker", "experiments",
                LocalDate.now().toString());
        Files.createDirectories(base);
        String name = prefix + "-" + TS_FORMAT.format(LocalDateTime.now());
        Path dir = base.resolve(name);
        Files.createDirectories(dir);
        return dir;
    }

    private String toSweepCsv(SweepSeries baseline, SweepSeries urgency) {
        StringBuilder sb = new StringBuilder();
        sb.append("x_value,baseline_cost,urgency_cost,baseline_overreaction,urgency_overreaction,baseline_missed,urgency_missed\n");
        int size = Math.min(baseline.getPoints().size(), urgency.getPoints().size());
        for (int i = 0; i < size; i++) {
            SweepPoint b = baseline.getPoints().get(i);
            SweepPoint u = urgency.getPoints().get(i);
            sb.append(String.format("%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f\n",
                    b.xValue(), b.meanTotalCost(), u.meanTotalCost(), b.meanOverreaction(), u.meanOverreaction(),
                    b.meanMissed(), u.meanMissed()));
        }
        return sb.toString();
    }

    public static String jsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(items.get(i).replace("\"", "'")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
