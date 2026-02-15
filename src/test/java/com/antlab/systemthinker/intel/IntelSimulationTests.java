package com.antlab.systemthinker.intel;

import com.antlab.systemthinker.intel.sim.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntelSimulationTests {
    @Test
    void determinismTrial() {
        IntelSimulator simulator = new IntelSimulator();
        IntelScenarioConfig config = baseConfig(0.3);
        IntelTrialResult first = simulator.runTrial(config, 1234L);
        IntelTrialResult second = simulator.runTrial(config, 1234L);
        assertEquals(first.getTotalCost(), second.getTotalCost(), 1e-9);
        assertEquals(first.getOverreactionRate(), second.getOverreactionRate(), 1e-9);
        assertEquals(first.getMissedThreatRate(), second.getMissedThreatRate(), 1e-9);
        assertEquals(first.getMeanDecisionDelay(), second.getMeanDecisionDelay(), 1e-9);
    }

    @Test
    void determinismBatch() {
        IntelSimulator simulator = new IntelSimulator();
        MonteCarloRunnerIntel runner = new MonteCarloRunnerIntel(simulator, 0.0, 200, 3);
        IntelScenarioConfig config = baseConfig(0.2);
        IntelBatchSummary first = runner.runBatch(config, 200, 9001L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);
        IntelBatchSummary second = runner.runBatch(config, 200, 9001L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);
        assertEquals(first.getTotalCostStats().mean(), second.getTotalCostStats().mean(), 1e-9);
        assertEquals(first.getOverreactionStats().mean(), second.getOverreactionStats().mean(), 1e-9);
        assertEquals(first.getMissedStats().mean(), second.getMissedStats().mean(), 1e-9);
    }

    @Test
    void specificityReducesOverreaction() {
        IntelSimulator simulator = new IntelSimulator();
        MonteCarloRunnerIntel runner = new MonteCarloRunnerIntel(simulator, 0.0, 200, 3);
        IntelScenarioConfig base = baseConfig(0.2);
        IntelScenarioConfig highSpec = adjustSpecificity(base, 0.95);
        IntelScenarioConfig lowSpec = adjustSpecificity(base, 0.70);

        IntelBatchSummary high = runner.runBatch(highSpec, 350, 101L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);
        IntelBatchSummary low = runner.runBatch(lowSpec, 350, 101L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);

        assertTrue(high.getOverreactionStats().mean() <= low.getOverreactionStats().mean() + 0.02);
    }

    @Test
    void sensitivityReducesMissedThreats() {
        IntelSimulator simulator = new IntelSimulator();
        MonteCarloRunnerIntel runner = new MonteCarloRunnerIntel(simulator, 0.0, 200, 3);
        IntelScenarioConfig base = baseConfig(0.2);
        IntelScenarioConfig highSens = adjustSensitivity(base, 0.92);
        IntelScenarioConfig lowSens = adjustSensitivity(base, 0.70);

        IntelBatchSummary high = runner.runBatch(highSens, 350, 202L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);
        IntelBatchSummary low = runner.runBatch(lowSens, 350, 202L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);

        assertTrue(high.getMissedStats().mean() <= low.getMissedStats().mean() + 0.02);
    }

    @Test
    void urgencyIncreasesOverreactionAndReducesDelay() {
        IntelSimulator simulator = new IntelSimulator();
        MonteCarloRunnerIntel runner = new MonteCarloRunnerIntel(simulator, 0.0, 200, 3);
        IntelScenarioConfig calm = baseConfig(0.0);
        IntelScenarioConfig urgent = baseConfig(0.8);

        IntelBatchSummary calmSummary = runner.runBatch(calm, 350, 404L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);
        IntelBatchSummary urgentSummary = runner.runBatch(urgent, 350, 404L,
                MonteCarloRunnerIntel.SeedPolicy.SEQUENTIAL, null, null);

        assertTrue(urgentSummary.getOverreactionStats().mean() >= calmSummary.getOverreactionStats().mean() - 0.01);
        assertTrue(urgentSummary.getDecisionDelayStats().mean() <= calmSummary.getDecisionDelayStats().mean() + 0.05);
    }

    private IntelScenarioConfig baseConfig(double urgency) {
        double[][] transitions = new double[][] {
                {0.98, 0.02, 0.00},
                {0.12, 0.70, 0.18},
                {0.05, 0.10, 0.85}
        };
        List<SourceConfig> sources = List.of(
                new SourceConfig(0, "S1", 0.75, 0.90, 0.0, 0.05, 0.70, 1.5, 4),
                new SourceConfig(1, "S2", 0.72, 0.88, 0.0, 0.08, 0.60, 2.0, 5),
                new SourceConfig(2, "S3", 0.70, 0.90, 0.02, 0.06, 0.55, 1.0, 3)
        );
        AnalystPolicy policy = new AnalystPolicy(
                0.25,
                0.92,
                0.75,
                0.45,
                0.65,
                0.12,
                0.08,
                4,
                0.08,
                0.05,
                0.6,
                0.7
        );
        CostWeights costs = new CostWeights(6.0, 10.0, 0.8, 0.2, 1.2);
        return new IntelScenarioConfig(400, transitions, sources, policy, costs, urgency);
    }

    private IntelScenarioConfig adjustSpecificity(IntelScenarioConfig config, double specificity) {
        List<SourceConfig> updated = new ArrayList<>();
        for (SourceConfig source : config.getSources()) {
            updated.add(source.withSpecificity(specificity));
        }
        return config.withSources(updated);
    }

    private IntelScenarioConfig adjustSensitivity(IntelScenarioConfig config, double sensitivity) {
        List<SourceConfig> updated = new ArrayList<>();
        for (SourceConfig source : config.getSources()) {
            updated.add(source.withSensitivity(sensitivity));
        }
        return config.withSources(updated);
    }
}
