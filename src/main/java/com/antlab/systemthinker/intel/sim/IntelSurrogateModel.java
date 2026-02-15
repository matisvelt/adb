package com.antlab.systemthinker.intel.sim;

public final class IntelSurrogateModel {
    private final RidgeRegression.Model costModel;
    private final RidgeRegression.Model overreactionModel;
    private final RidgeRegression.Model missedModel;
    private final RidgeRegression.Model delayModel;
    private final double maeCost;
    private final double maeOverreaction;
    private final double maeMissed;
    private final double maeDelay;

    public IntelSurrogateModel(RidgeRegression.Model costModel,
                               RidgeRegression.Model overreactionModel,
                               RidgeRegression.Model missedModel,
                               RidgeRegression.Model delayModel,
                               double maeCost,
                               double maeOverreaction,
                               double maeMissed,
                               double maeDelay) {
        this.costModel = costModel;
        this.overreactionModel = overreactionModel;
        this.missedModel = missedModel;
        this.delayModel = delayModel;
        this.maeCost = maeCost;
        this.maeOverreaction = maeOverreaction;
        this.maeMissed = maeMissed;
        this.maeDelay = maeDelay;
    }

    public RidgeRegression.Model getCostModel() {
        return costModel;
    }

    public RidgeRegression.Model getOverreactionModel() {
        return overreactionModel;
    }

    public RidgeRegression.Model getMissedModel() {
        return missedModel;
    }

    public RidgeRegression.Model getDelayModel() {
        return delayModel;
    }

    public double getMaeCost() {
        return maeCost;
    }

    public double getMaeOverreaction() {
        return maeOverreaction;
    }

    public double getMaeMissed() {
        return maeMissed;
    }

    public double getMaeDelay() {
        return maeDelay;
    }
}
