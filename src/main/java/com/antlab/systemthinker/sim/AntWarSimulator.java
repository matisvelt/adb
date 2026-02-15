package com.antlab.systemthinker.sim;

import java.util.Random;

public class AntWarSimulator {
    private static final int EMPTY = 0;
    private static final int A = 1;
    private static final int B = 2;

    public SimulationResult run(ScenarioParameters params) {
        Random random = new Random(params.seed);
        int width = params.gridWidth;
        int height = params.gridHeight;

        boolean[][] resources = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                resources[x][y] = random.nextDouble() < params.resourceDensity;
            }
        }

        int[][] aCounts = new int[width][height];
        int[][] bCounts = new int[width][height];

        placeInitial(random, aCounts, params.initialColonyASize, 0, width / 3, height);
        placeInitial(random, bCounts, params.initialColonyBSize, width * 2 / 3, width, height);

        int totalInitial = params.initialColonyASize + params.initialColonyBSize;
        int totalDeaths = 0;
        int totalSpawns = 0;

        int[][] lastOwner = new int[width][height];
        double frontierVolatilitySum = 0.0;

        int territoryStreakA = 0;
        int territoryStreakB = 0;

        for (int step = 1; step <= params.maxSteps; step++) {
            int[][] nextA = new int[width][height];
            int[][] nextB = new int[width][height];

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (aCounts[x][y] > 0) {
                        int[] dir = chooseDirection(x, y, A, params, aCounts, bCounts, random);
                        moveAgents(aCounts[x][y], x, y, dir, nextA, width, height, random);
                    }
                    if (bCounts[x][y] > 0) {
                        int[] dir = chooseDirection(x, y, B, params, aCounts, bCounts, random);
                        moveAgents(bCounts[x][y], x, y, dir, nextB, width, height, random);
                    }
                }
            }

            int deathsThisStep = resolveCombat(nextA, nextB, params, random);
            totalDeaths += deathsThisStep;

            int spawnsThisStep = spawnFromResources(nextA, nextB, resources, params, random);
            totalSpawns += spawnsThisStep;

            aCounts = nextA;
            bCounts = nextB;

            int[] territory = computeTerritory(aCounts, bCounts, lastOwner);
            int cellsA = territory[0];
            int cellsB = territory[1];
            int changed = territory[2];

            frontierVolatilitySum += (double) changed / (width * height);

            double shareA = (double) cellsA / (width * height);
            double shareB = (double) cellsB / (width * height);

            if (shareA > params.territoryDecisionThreshold) {
                territoryStreakA++;
            } else {
                territoryStreakA = 0;
            }

            if (shareB > params.territoryDecisionThreshold) {
                territoryStreakB++;
            } else {
                territoryStreakB = 0;
            }

            int popA = countTotal(aCounts);
            int popB = countTotal(bCounts);

            if (popA == 0 && popB == 0) {
                return buildResult(SimulationResult.Winner.DRAW, step, shareA, shareB,
                        totalDeaths, totalInitial + totalSpawns, frontierVolatilitySum, step, params.seed);
            }
            if (popA == 0) {
                return buildResult(SimulationResult.Winner.B, step, shareA, shareB,
                        totalDeaths, totalInitial + totalSpawns, frontierVolatilitySum, step, params.seed);
            }
            if (popB == 0) {
                return buildResult(SimulationResult.Winner.A, step, shareA, shareB,
                        totalDeaths, totalInitial + totalSpawns, frontierVolatilitySum, step, params.seed);
            }
            if (territoryStreakA >= params.decisionStreak) {
                return buildResult(SimulationResult.Winner.A, step, shareA, shareB,
                        totalDeaths, totalInitial + totalSpawns, frontierVolatilitySum, step, params.seed);
            }
            if (territoryStreakB >= params.decisionStreak) {
                return buildResult(SimulationResult.Winner.B, step, shareA, shareB,
                        totalDeaths, totalInitial + totalSpawns, frontierVolatilitySum, step, params.seed);
            }
        }

        int[] territory = computeTerritory(aCounts, bCounts, lastOwner);
        double shareA = (double) territory[0] / (width * height);
        double shareB = (double) territory[1] / (width * height);

        SimulationResult.Winner winner;
        if (shareA > shareB) {
            winner = SimulationResult.Winner.A;
        } else if (shareB > shareA) {
            winner = SimulationResult.Winner.B;
        } else {
            winner = SimulationResult.Winner.DRAW;
        }

        return buildResult(winner, params.maxSteps, shareA, shareB,
                totalDeaths, totalInitial + totalSpawns, frontierVolatilitySum, params.maxSteps, params.seed);
    }

    private void placeInitial(Random random, int[][] counts, int size, int xStart, int xEnd, int height) {
        int width = counts.length;
        int attempts = 0;
        int placed = 0;
        while (placed < size && attempts < size * 10) {
            int x = xStart + random.nextInt(Math.max(1, xEnd - xStart));
            int y = random.nextInt(height);
            if (x >= 0 && x < width) {
                counts[x][y] += 1;
                placed++;
            }
            attempts++;
        }
        while (placed < size) {
            int x = xStart + (placed % Math.max(1, xEnd - xStart));
            int y = (placed * 31) % height;
            if (x >= 0 && x < width) {
                counts[x][y] += 1;
                placed++;
            }
        }
    }

    private int[] chooseDirection(int x, int y, int colony, ScenarioParameters params,
                                  int[][] aCounts, int[][] bCounts, Random random) {
        if (params.detectionRadius > 0 && random.nextDouble() < params.movementBias) {
            int[] target = findNearestEnemy(x, y, colony, params.detectionRadius, aCounts, bCounts);
            if (target != null) {
                int dx = Integer.signum(target[0] - x);
                int dy = Integer.signum(target[1] - y);
                return new int[]{dx, dy};
            }
        }
        int r = random.nextInt(4);
        if (r == 0) return new int[]{1, 0};
        if (r == 1) return new int[]{-1, 0};
        if (r == 2) return new int[]{0, 1};
        return new int[]{0, -1};
    }

    private int[] findNearestEnemy(int x, int y, int colony, int radius,
                                  int[][] aCounts, int[][] bCounts) {
        int width = aCounts.length;
        int height = aCounts[0].length;
        int bestDist = Integer.MAX_VALUE;
        int bestX = -1;
        int bestY = -1;

        int minX = Math.max(0, x - radius);
        int maxX = Math.min(width - 1, x + radius);
        int minY = Math.max(0, y - radius);
        int maxY = Math.min(height - 1, y + radius);

        for (int ix = minX; ix <= maxX; ix++) {
            for (int iy = minY; iy <= maxY; iy++) {
                int enemyCount = colony == A ? bCounts[ix][iy] : aCounts[ix][iy];
                if (enemyCount <= 0) {
                    continue;
                }
                int dist = Math.abs(ix - x) + Math.abs(iy - y);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestX = ix;
                    bestY = iy;
                }
            }
        }

        if (bestX >= 0) {
            return new int[]{bestX, bestY};
        }
        return null;
    }

    private void moveAgents(int count, int x, int y, int[] dir, int[][] target,
                            int width, int height, Random random) {
        for (int i = 0; i < count; i++) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                nx = x;
                ny = y;
            }
            target[nx][ny] += 1;
        }
    }

    private int resolveCombat(int[][] aCounts, int[][] bCounts, ScenarioParameters params, Random random) {
        int width = aCounts.length;
        int height = aCounts[0].length;
        int deaths = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int a = aCounts[x][y];
                int b = bCounts[x][y];
                if (a > 0 && b > 0) {
                    double pA = Math.min(1.0, params.lethalityCoefficient * ((double) b / (a + b)));
                    double pB = Math.min(1.0, params.lethalityCoefficient * ((double) a / (a + b)));
                    int killedA = sampleBinomial(a, pA, random);
                    int killedB = sampleBinomial(b, pB, random);
                    aCounts[x][y] = Math.max(0, a - killedA);
                    bCounts[x][y] = Math.max(0, b - killedB);
                    deaths += killedA + killedB;
                }
            }
        }
        return deaths;
    }

    private int spawnFromResources(int[][] aCounts, int[][] bCounts, boolean[][] resources,
                                   ScenarioParameters params, Random random) {
        int width = aCounts.length;
        int height = aCounts[0].length;
        int spawns = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!resources[x][y]) {
                    continue;
                }
                if (aCounts[x][y] > bCounts[x][y]) {
                    int spawn = spawnCount(params.spawnRateA, random);
                    aCounts[x][y] += spawn;
                    spawns += spawn;
                } else if (bCounts[x][y] > aCounts[x][y]) {
                    int spawn = spawnCount(params.spawnRateB, random);
                    bCounts[x][y] += spawn;
                    spawns += spawn;
                }
            }
        }
        return spawns;
    }

    private int spawnCount(double rate, Random random) {
        if (rate <= 0) {
            return 0;
        }
        int base = (int) Math.floor(rate);
        double frac = rate - base;
        return base + (random.nextDouble() < frac ? 1 : 0);
    }

    private int[] computeTerritory(int[][] aCounts, int[][] bCounts, int[][] lastOwner) {
        int width = aCounts.length;
        int height = aCounts[0].length;
        int cellsA = 0;
        int cellsB = 0;
        int changed = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int owner = EMPTY;
                if (aCounts[x][y] > bCounts[x][y]) {
                    owner = A;
                    cellsA++;
                } else if (bCounts[x][y] > aCounts[x][y]) {
                    owner = B;
                    cellsB++;
                }
                if (lastOwner[x][y] != owner) {
                    changed++;
                }
                lastOwner[x][y] = owner;
            }
        }
        return new int[]{cellsA, cellsB, changed};
    }

    private int countTotal(int[][] counts) {
        int total = 0;
        for (int[] row : counts) {
            for (int v : row) {
                total += v;
            }
        }
        return total;
    }

    private SimulationResult buildResult(SimulationResult.Winner winner,
                                         int step,
                                         double shareA,
                                         double shareB,
                                         int totalDeaths,
                                         int totalPopulation,
                                         double frontierVolatilitySum,
                                         int stepsElapsed,
                                         long seed) {
        double casualtyRatio = totalPopulation == 0 ? 0 : (double) totalDeaths / totalPopulation;
        double frontierVolatility = stepsElapsed == 0 ? 0 : frontierVolatilitySum / stepsElapsed;
        return new SimulationResult(
                winner,
                step,
                shareA,
                shareB,
                casualtyRatio,
                frontierVolatility,
                seed
        );
    }

    private int sampleBinomial(int n, double p, Random random) {
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (random.nextDouble() < p) {
                k++;
            }
        }
        return k;
    }
}
