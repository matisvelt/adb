package com.antlab.systemthinker.sim;

import java.util.Objects;

public final class ScenarioParameters {
    public final int gridWidth;
    public final int gridHeight;
    public final int initialColonyASize;
    public final int initialColonyBSize;
    public final double spawnRateA;
    public final double spawnRateB;
    public final double resourceDensity;
    public final double movementBias;
    public final int detectionRadius;
    public final double lethalityCoefficient;
    public final int maxSteps;
    public final long seed;
    public final double territoryDecisionThreshold;
    public final int decisionStreak;

    public ScenarioParameters(
            int gridWidth,
            int gridHeight,
            int initialColonyASize,
            int initialColonyBSize,
            double spawnRateA,
            double spawnRateB,
            double resourceDensity,
            double movementBias,
            int detectionRadius,
            double lethalityCoefficient,
            int maxSteps,
            long seed,
            double territoryDecisionThreshold,
            int decisionStreak
    ) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.initialColonyASize = initialColonyASize;
        this.initialColonyBSize = initialColonyBSize;
        this.spawnRateA = spawnRateA;
        this.spawnRateB = spawnRateB;
        this.resourceDensity = resourceDensity;
        this.movementBias = movementBias;
        this.detectionRadius = detectionRadius;
        this.lethalityCoefficient = lethalityCoefficient;
        this.maxSteps = maxSteps;
        this.seed = seed;
        this.territoryDecisionThreshold = territoryDecisionThreshold;
        this.decisionStreak = decisionStreak;

        validate();
    }

    private void validate() {
        if (gridWidth <= 1 || gridHeight <= 1) {
            throw new IllegalArgumentException("Grid size must be > 1");
        }
        if (initialColonyASize < 1 || initialColonyBSize < 1) {
            throw new IllegalArgumentException("Initial colony sizes must be >= 1");
        }
        if (resourceDensity < 0 || resourceDensity > 1) {
            throw new IllegalArgumentException("Resource density must be in [0,1]");
        }
        if (movementBias < 0 || movementBias > 1) {
            throw new IllegalArgumentException("Movement bias must be in [0,1]");
        }
        if (detectionRadius < 0) {
            throw new IllegalArgumentException("Detection radius must be >= 0");
        }
        if (lethalityCoefficient < 0) {
            throw new IllegalArgumentException("Lethality coefficient must be >= 0");
        }
        if (maxSteps < 1) {
            throw new IllegalArgumentException("Max steps must be >= 1");
        }
        if (territoryDecisionThreshold <= 0 || territoryDecisionThreshold >= 1) {
            throw new IllegalArgumentException("Territory decision threshold must be in (0,1)");
        }
        if (decisionStreak < 1) {
            throw new IllegalArgumentException("Decision streak must be >= 1");
        }
    }

    public ScenarioParameters withSeed(long newSeed) {
        return new ScenarioParameters(
                gridWidth,
                gridHeight,
                initialColonyASize,
                initialColonyBSize,
                spawnRateA,
                spawnRateB,
                resourceDensity,
                movementBias,
                detectionRadius,
                lethalityCoefficient,
                maxSteps,
                newSeed,
                territoryDecisionThreshold,
                decisionStreak
        );
    }

    @Override
    public String toString() {
        return "ScenarioParameters{" +
                "gridWidth=" + gridWidth +
                ", gridHeight=" + gridHeight +
                ", initialColonyASize=" + initialColonyASize +
                ", initialColonyBSize=" + initialColonyBSize +
                ", spawnRateA=" + spawnRateA +
                ", spawnRateB=" + spawnRateB +
                ", resourceDensity=" + resourceDensity +
                ", movementBias=" + movementBias +
                ", detectionRadius=" + detectionRadius +
                ", lethalityCoefficient=" + lethalityCoefficient +
                ", maxSteps=" + maxSteps +
                ", seed=" + seed +
                ", territoryDecisionThreshold=" + territoryDecisionThreshold +
                ", decisionStreak=" + decisionStreak +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScenarioParameters)) return false;
        ScenarioParameters that = (ScenarioParameters) o;
        return gridWidth == that.gridWidth &&
                gridHeight == that.gridHeight &&
                initialColonyASize == that.initialColonyASize &&
                initialColonyBSize == that.initialColonyBSize &&
                Double.compare(that.spawnRateA, spawnRateA) == 0 &&
                Double.compare(that.spawnRateB, spawnRateB) == 0 &&
                Double.compare(that.resourceDensity, resourceDensity) == 0 &&
                Double.compare(that.movementBias, movementBias) == 0 &&
                detectionRadius == that.detectionRadius &&
                Double.compare(that.lethalityCoefficient, lethalityCoefficient) == 0 &&
                maxSteps == that.maxSteps &&
                seed == that.seed &&
                Double.compare(that.territoryDecisionThreshold, territoryDecisionThreshold) == 0 &&
                decisionStreak == that.decisionStreak;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gridWidth, gridHeight, initialColonyASize, initialColonyBSize,
                spawnRateA, spawnRateB, resourceDensity, movementBias, detectionRadius,
                lethalityCoefficient, maxSteps, seed, territoryDecisionThreshold, decisionStreak);
    }
}
