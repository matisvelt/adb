package com.antlab.systemthinker.sim;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTests {

    @Test
    void sameSeedProducesIdenticalResults() {
        ScenarioParameters params = new ScenarioParameters(
                30, 20,
                60, 60,
                0.2, 0.2,
                0.3, 0.5,
                3, 0.35,
                200,
                1234L,
                0.7,
                10
        );
        AntWarSimulator sim = new AntWarSimulator();
        SimulationResult r1 = sim.run(params);
        SimulationResult r2 = sim.run(params);

        assertEquals(r1.winner, r2.winner);
        assertEquals(r1.timeToDecision, r2.timeToDecision);
        assertEquals(r1.territoryShareA, r2.territoryShareA, 1e-9);
        assertEquals(r1.territoryShareB, r2.territoryShareB, 1e-9);
        assertEquals(r1.casualtyRatio, r2.casualtyRatio, 1e-9);
        assertEquals(r1.frontierVolatility, r2.frontierVolatility, 1e-9);
    }

    @Test
    void largerColonyAIncreasesWinProbability() {
        MonteCarloRunner runner = new MonteCarloRunner(new AntWarSimulator(), Logger.getLogger("test"),
                0.000001, 100, 1000);

        ScenarioParameters base = new ScenarioParameters(
                40, 30,
                60, 80,
                0.0, 0.0,
                0.2, 0.6,
                3, 0.35,
                220,
                2000L,
                0.7,
                10
        );
        ScenarioParameters strongerA = new ScenarioParameters(
                40, 30,
                140, 60,
                0.0, 0.0,
                0.2, 0.6,
                3, 0.35,
                220,
                2000L,
                0.7,
                10
        );

        MonteCarloSummary s1 = runner.runBatch(base, 300);
        MonteCarloSummary s2 = runner.runBatch(strongerA, 300);

        assertTrue(s2.winProbabilityA > s1.winProbabilityA);
    }

    @Test
    void higherLethalityReducesMeanTime() {
        MonteCarloRunner runner = new MonteCarloRunner(new AntWarSimulator(), Logger.getLogger("test"),
                0.000001, 100, 1000);

        ScenarioParameters low = new ScenarioParameters(
                15, 15,
                70, 70,
                0.0, 0.0,
                0.1, 0.8,
                2, 0.2,
                150,
                3000L,
                0.7,
                10
        );
        ScenarioParameters high = new ScenarioParameters(
                15, 15,
                70, 70,
                0.0, 0.0,
                0.1, 0.8,
                2, 0.6,
                150,
                3000L,
                0.7,
                10
        );

        MonteCarloSummary sLow = runner.runBatch(low, 400);
        MonteCarloSummary sHigh = runner.runBatch(high, 400);

        assertTrue(sHigh.meanTimeToDecision < sLow.meanTimeToDecision);
    }

    @Test
    void confidenceIntervalNarrowsWithMoreTrials() {
        MonteCarloRunner runner = new MonteCarloRunner(new AntWarSimulator(), Logger.getLogger("test"),
                0.000001, 100, 1000);

        ScenarioParameters params = new ScenarioParameters(
                40, 30,
                80, 80,
                0.2, 0.2,
                0.3, 0.5,
                3, 0.35,
                220,
                4000L,
                0.7,
                10
        );

        MonteCarloSummary small = runner.runBatch(params, 200);
        MonteCarloSummary large = runner.runBatch(params, 800);

        double widthSmall = small.confidenceHigh - small.confidenceLow;
        double widthLarge = large.confidenceHigh - large.confidenceLow;

        assertTrue(widthLarge < widthSmall);
    }
}
