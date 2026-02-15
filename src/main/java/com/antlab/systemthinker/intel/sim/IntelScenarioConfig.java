package com.antlab.systemthinker.intel.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IntelScenarioConfig {
    private final int timeHorizon;
    private final double[][] transitionMatrix;
    private final List<SourceConfig> sources;
    private final AnalystPolicy policy;
    private final CostWeights costs;
    private final double urgency;

    public IntelScenarioConfig(int timeHorizon,
                               double[][] transitionMatrix,
                               List<SourceConfig> sources,
                               AnalystPolicy policy,
                               CostWeights costs,
                               double urgency) {
        this.timeHorizon = timeHorizon;
        this.transitionMatrix = copyMatrix(transitionMatrix);
        this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
        this.policy = policy;
        this.costs = costs;
        this.urgency = urgency;
    }

    public int getTimeHorizon() {
        return timeHorizon;
    }

    public double[][] getTransitionMatrix() {
        return copyMatrix(transitionMatrix);
    }

    public List<SourceConfig> getSources() {
        return sources;
    }

    public AnalystPolicy getPolicy() {
        return policy;
    }

    public CostWeights getCosts() {
        return costs;
    }

    public double getUrgency() {
        return urgency;
    }

    public IntelScenarioConfig withUrgency(double value) {
        return new IntelScenarioConfig(timeHorizon, transitionMatrix, sources, policy, costs, value);
    }

    public IntelScenarioConfig withSources(List<SourceConfig> value) {
        return new IntelScenarioConfig(timeHorizon, transitionMatrix, value, policy, costs, urgency);
    }

    public IntelScenarioConfig withPolicy(AnalystPolicy value) {
        return new IntelScenarioConfig(timeHorizon, transitionMatrix, sources, value, costs, urgency);
    }

    public IntelScenarioConfig withCosts(CostWeights value) {
        return new IntelScenarioConfig(timeHorizon, transitionMatrix, sources, policy, value, urgency);
    }

    public IntelScenarioConfig withTransitionMatrix(double[][] value) {
        return new IntelScenarioConfig(timeHorizon, value, sources, policy, costs, urgency);
    }

    public IntelScenarioConfig withTimeHorizon(int value) {
        return new IntelScenarioConfig(value, transitionMatrix, sources, policy, costs, urgency);
    }

    private double[][] copyMatrix(double[][] matrix) {
        double[][] copy = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = matrix[i].clone();
        }
        return copy;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"timeHorizon\":").append(timeHorizon);
        sb.append(",\"urgency\":").append(String.format("%.5f", urgency));
        sb.append(",\"transitionMatrix\":[");
        for (int i = 0; i < transitionMatrix.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("[");
            for (int j = 0; j < transitionMatrix[i].length; j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append(String.format("%.5f", transitionMatrix[i][j]));
            }
            sb.append("]");
        }
        sb.append("]");
        sb.append(",\"policy\":").append(policy.toJson());
        sb.append(",\"costs\":").append(costs.toJson());
        sb.append(",\"sources\":[");
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(sources.get(i).toJson());
        }
        sb.append("]}");
        return sb.toString();
    }
}
