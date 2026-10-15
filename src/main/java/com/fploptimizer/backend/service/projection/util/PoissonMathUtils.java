package com.fploptimizer.backend.service.projection.util;

public final class PoissonMathUtils {

    private static final int MAX_POISSON_ITERATIONS = 12;

    private PoissonMathUtils() {
        throw new UnsupportedOperationException();
    }
    /*
        Calculates the probability P( X = k) for Poisson distribution with average expectedGoals
        It uses for loop to avoid k! overlow.
    */
    public static double calculateProbability(double lambda, int k)
    {
        if (k < 0) return 0;

        if (lambda <=0.0)
        {
            return  (k == 0) ? 1.0 : 0.0;
        }

        // P(0) = e^ (-expectedGoals)
        double currentProb = Math.exp(-lambda);

        // P(i) = P(i - 1) * (expectedGoals / i)
        for (int i = 1; i <= k; i++)
        {
            currentProb *=(lambda/i);
        }

        return Math.clamp(currentProb, 0.0, 1.0);

    }
    /*
        Calculates tail probability P( X >= threshold )
     */
    public static double calculateTailProbability(double lambda, int threshold){
        if (threshold <= 0){
            return 1.0;
        }
        if (lambda <=0.0)
        {
            return  0.0;
        }

        // 1 - P(X < threshold) = 1 - sum_{k=0}^{threshold - 1} P(X = k)
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
    /*
        Calculates the expected penalty for goals conceded :
        sum( floor(k / 2) * P(X = k) )

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
            // floor(k / 2): for k = 0,1 -> 0 | for k = 2,3 -> 1 | for k = 4,5 -> 2 κ.ο.κ.
            expectedPenalty += ((double) k / 2) * currentProb;
        }

        return Math.max(0.0, expectedPenalty);
    }
    /*
    Calculates the expected points from saves for GKP:
    sum( floor(k / 3) * P(X = k) )
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
            // floor(k / 3): for k = 0,1,2 -> 0 | for k = 3,4,5 -> 1 | for k = 6,7,8 -> 2 κ.ο.κ.
            expectedSavePoints += (k / 3) * currentProb;
        }

        return Math.max(0.0, expectedSavePoints);
    }


}
