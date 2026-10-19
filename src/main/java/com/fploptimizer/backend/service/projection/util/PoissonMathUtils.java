package com.fploptimizer.backend.service.projection.util;

/**
 * Mathematical utility class providing Poisson distribution algorithms and expected value calculations.
 *
 * Implements numerical recurrence algorithms to avoid factorial overflow when computing
 * discrete probabilities, cumulative tail distributions, expected goals conceded penalties,
 * and goalkeeper save points.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
public final class PoissonMathUtils {

    // Maximum iteration bound for infinite series Poisson summation
    private static final int MAX_POISSON_ITERATIONS = 12;

    /**
     * Private constructor to prevent direct instantiation of utility class.
     */
    private PoissonMathUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Computes the exact discrete Poisson probability mass P(X = k) for a given rate lambda.
     *
     * Utilizes iterative recurrence P(i) = P(i - 1) * (lambda / i) to bypass factorial overflow.
     *
     * @param lambda the expected mean event count rate (must be >= 0.0)
     * @param k      the exact number of event occurrences (must be >= 0)
     * @return probability P(X = k) clamped within [0.0, 1.0]
     */
    public static double calculateProbability(double lambda, int k) {
        if (k < 0) return 0.0;

        if (lambda <= 0.0) {
            return (k == 0) ? 1.0 : 0.0;
        }

        // Base case: P(0) = e^(-lambda)
        double currentProb = Math.exp(-lambda);

        // Recurrence step: P(i) = P(i - 1) * (lambda / i)
        for (int i = 1; i <= k; i++) {
            currentProb *= (lambda / i);
        }

        return Math.clamp(currentProb, 0.0, 1.0);
    }

    /**
     * Computes the upper tail cumulative probability P(X >= threshold) for a Poisson distribution.
     *
     * Evaluated using the complement identity: 1 - sum_{k=0}^{threshold - 1} P(X = k).
     *
     * @param lambda    the expected mean event count rate
     * @param threshold the minimum inclusive occurrence count
     * @return cumulative tail probability clamped within [0.0, 1.0]
     */
    public static double calculateTailProbability(double lambda, int threshold) {
        if (threshold <= 0) {
            return 1.0;
        }
        if (lambda <= 0.0) {
            return 0.0;
        }

        double cumulativeProb = 0.0;
        double currentProb = Math.exp(-lambda);

        for (int k = 0; k < threshold; k++) {
            if (k > 0) {
                currentProb *= (lambda / k);
            }
            cumulativeProb += currentProb;
        }

        return Math.clamp(1.0 - cumulativeProb, 0.0, 1.0);
    }

    /**
     * Computes the expected FPL goals conceded penalty points deduction for defenders and goalkeepers.
     *
     * FPL deducts 1 point for every 2 goals conceded: sum_{k=0}^{MAX} floor(k / 2) * P(X = k).
     *
     * @param lambda the expected goals conceded rate
     * @return expected points deduction (positive value representing point loss)
     */
    public static double calculateExpectedGoalsConcededPenalty(double lambda) {
        if (lambda <= 0.0) {
            return 0.0;
        }

        double expectedPenalty = 0.0;
        double currentProb = Math.exp(-lambda);

        for (int k = 0; k <= MAX_POISSON_ITERATIONS; k++) {
            if (k > 0) {
                currentProb *= (lambda / k);
            }
            // Floor division (k / 2): k in {0, 1} -> 0 | k in {2, 3} -> 1 | k in {4, 5} -> 2
            expectedPenalty += (k / 2) * currentProb;
        }

        return Math.max(0.0, expectedPenalty);
    }

    /**
     * Computes the expected save points earned by a goalkeeper.
     *
     * FPL awards 1 point for every 3 saves recorded: sum_{k=0}^{MAX} floor(k / 3) * P(X = k).
     *
     * @param lambdaSaves the expected saves rate for the fixture
     * @return expected points earned from goalkeeper saves
     */
    public static double calculateExpectedSavePoints(double lambdaSaves) {
        if (lambdaSaves <= 0.0) {
            return 0.0;
        }

        double expectedSavePoints = 0.0;
        double currentProb = Math.exp(-lambdaSaves);

        for (int k = 0; k <= MAX_POISSON_ITERATIONS; k++) {
            if (k > 0) {
                currentProb *= (lambdaSaves / k);
            }
            // Floor division (k / 3): k in {0, 1, 2} -> 0 | k in {3, 4, 5} -> 1 | k in {6, 7, 8} -> 2
            expectedSavePoints += (k / 3) * currentProb;
        }

        return Math.max(0.0, expectedSavePoints);
    }
}