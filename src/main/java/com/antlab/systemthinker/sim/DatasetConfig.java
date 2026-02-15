package com.antlab.systemthinker.sim;

public final class DatasetConfig {
    public final IntRange colonyARange;
    public final IntRange colonyBRange;
    public final IntRange gridWidthRange;
    public final IntRange gridHeightRange;
    public final DoubleRange spawnARange;
    public final DoubleRange spawnBRange;
    public final DoubleRange resourceDensityRange;
    public final DoubleRange movementBiasRange;
    public final IntRange detectionRadiusRange;
    public final DoubleRange lethalityRange;
    public final IntRange maxStepsRange;
    public final DoubleRange territoryDecisionThresholdRange;
    public final IntRange decisionStreakRange;
    public final int scenarios;
    public final int trialsPerScenario;
    public final long baseSeed;

    public DatasetConfig(
            IntRange colonyARange,
            IntRange colonyBRange,
            IntRange gridWidthRange,
            IntRange gridHeightRange,
            DoubleRange spawnARange,
            DoubleRange spawnBRange,
            DoubleRange resourceDensityRange,
            DoubleRange movementBiasRange,
            IntRange detectionRadiusRange,
            DoubleRange lethalityRange,
            IntRange maxStepsRange,
            DoubleRange territoryDecisionThresholdRange,
            IntRange decisionStreakRange,
            int scenarios,
            int trialsPerScenario,
            long baseSeed
    ) {
        this.colonyARange = colonyARange;
        this.colonyBRange = colonyBRange;
        this.gridWidthRange = gridWidthRange;
        this.gridHeightRange = gridHeightRange;
        this.spawnARange = spawnARange;
        this.spawnBRange = spawnBRange;
        this.resourceDensityRange = resourceDensityRange;
        this.movementBiasRange = movementBiasRange;
        this.detectionRadiusRange = detectionRadiusRange;
        this.lethalityRange = lethalityRange;
        this.maxStepsRange = maxStepsRange;
        this.territoryDecisionThresholdRange = territoryDecisionThresholdRange;
        this.decisionStreakRange = decisionStreakRange;
        this.scenarios = scenarios;
        this.trialsPerScenario = trialsPerScenario;
        this.baseSeed = baseSeed;
    }
}
