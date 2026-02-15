package com.antlab.systemthinker.intel.sim;

import java.util.List;

public final class RidgeRegression {
    private RidgeRegression() {}

    public static Model train(List<double[]> features, List<Double> targets, double lambda) {
        int n = features.size();
        int p = features.get(0).length + 1;
        double[][] xtx = new double[p][p];
        double[] xty = new double[p];

        for (int i = 0; i < n; i++) {
            double[] x = features.get(i);
            double[] xb = new double[p];
            xb[0] = 1.0;
            System.arraycopy(x, 0, xb, 1, x.length);
            for (int r = 0; r < p; r++) {
                xty[r] += xb[r] * targets.get(i);
                for (int c = 0; c < p; c++) {
                    xtx[r][c] += xb[r] * xb[c];
                }
            }
        }

        for (int i = 0; i < p; i++) {
            xtx[i][i] += lambda;
        }

        double[] weights = solve(xtx, xty);
        return new Model(weights);
    }

    private static double[] solve(double[][] a, double[] b) {
        int n = b.length;
        double[][] aug = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, aug[i], 0, n);
            aug[i][n] = b[i];
        }

        for (int i = 0; i < n; i++) {
            int pivot = i;
            for (int r = i + 1; r < n; r++) {
                if (Math.abs(aug[r][i]) > Math.abs(aug[pivot][i])) {
                    pivot = r;
                }
            }
            if (Math.abs(aug[pivot][i]) < 1e-12) {
                continue;
            }
            if (pivot != i) {
                double[] temp = aug[i];
                aug[i] = aug[pivot];
                aug[pivot] = temp;
            }
            double div = aug[i][i];
            for (int c = i; c < n + 1; c++) {
                aug[i][c] /= div;
            }
            for (int r = 0; r < n; r++) {
                if (r == i) {
                    continue;
                }
                double factor = aug[r][i];
                for (int c = i; c < n + 1; c++) {
                    aug[r][c] -= factor * aug[i][c];
                }
            }
        }

        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = aug[i][n];
        }
        return x;
    }

    public record Model(double[] weights) {
        public double predict(double[] features) {
            double sum = weights[0];
            for (int i = 0; i < features.length; i++) {
                sum += weights[i + 1] * features[i];
            }
            return sum;
        }
    }
}
