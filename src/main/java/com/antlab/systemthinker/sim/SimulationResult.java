package com.antlab.systemthinker.sim;

public final class SimulationResult {
    public enum Winner {
        A, B, DRAW
    }

    public final Winner winner;
    public final int timeToDecision;
    public final double territoryShareA;
    public final double territoryShareB;
    public final double casualtyRatio;
    public final double frontierVolatility;
    public final long randomSeedUsed;

    public SimulationResult(
            Winner winner,
            int timeToDecision,
            double territoryShareA,
            double territoryShareB,
            double casualtyRatio,
            double frontierVolatility,
            long randomSeedUsed
    ) {
        this.winner = winner;
        this.timeToDecision = timeToDecision;
        this.territoryShareA = territoryShareA;
        this.territoryShareB = territoryShareB;
        this.casualtyRatio = casualtyRatio;
        this.frontierVolatility = frontierVolatility;
        this.randomSeedUsed = randomSeedUsed;
    }

    public String toJson() {
        return "{" +
                "\"winner\":\"" + winner + "\"," +
                "\"timeToDecision\":" + timeToDecision + "," +
                "\"territoryShareA\":" + territoryShareA + "," +
                "\"territoryShareB\":" + territoryShareB + "," +
                "\"casualtyRatio\":" + casualtyRatio + "," +
                "\"frontierVolatility\":" + frontierVolatility + "," +
                "\"randomSeedUsed\":" + randomSeedUsed +
                "}";
    }
}
