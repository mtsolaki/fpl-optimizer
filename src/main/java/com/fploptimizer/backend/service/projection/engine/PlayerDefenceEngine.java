package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.service.projection.model.PlayerDefenceResult;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import com.fploptimizer.backend.service.projection.util.PoissonMathUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computational engine responsible for evaluating defensive metrics and probabilities for players.
 *
 * Models clean sheet chances conditioned on playing 60+ minutes, calculates goals conceded penalties,
 * projects goalkeeper saves, and estimates defensive contribution points (DefCon) using Poisson distributions.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
public class PlayerDefenceEngine {

    // Positional prior for goalkeeper saves per 90 minutes (Premier League average)
    private static final double PRIOR_SAVES_GKP = 3.2;

    // Positional priors for defensive contribution actions per 90 minutes (DefCon)
    private static final double PRIOR_DEFCON_DEF = 11.0;
    private static final double PRIOR_DEFCON_MID = 6.0;
    private static final double PRIOR_DEFCON_FWD = 2.5;
    private static final double PRIOR_DEFCON_GKP = 1.0;

    // Sample threshold parameters for historical logs blending
    private static final double MAX_RECENT_WEIGHT = 0.35;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    /**
     * Calculates projected defensive statistics and point components for an upcoming fixture.
     *
     * Computes expected goals conceded, clean sheet probability (conditioned on completing 60+ minutes),
     * FPL goals conceded deduction penalties, goalkeeper saves, and the tail probability of reaching
     * the DefCon threshold (10+ actions).
     *
     * @param player             the player entity to evaluate
     * @param projectedMinutes   projected playing time in minutes
     * @param plus60probability  probability that the player remains on pitch for at least 60 minutes
     * @param teamProjection     team-level strength context and expected goals conceded
     * @param gameweekLogs       recent match logs to blend empirical saves and defensive actions
     * @return a PlayerDefenceResult containing clean sheet odds, xGC, penalties, saves, and DefCon metrics
     */
    public PlayerDefenceResult calculatePlayerDefence(
            Player player,
            double projectedMinutes,
            double plus60probability,
            TeamStrengthProjection teamProjection,
            List<PlayerGameweekLog> gameweekLogs
    ) {
        if (player == null || teamProjection == null || projectedMinutes <= 0) {
            return new PlayerDefenceResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        // Expected goals conceded scaled to expected pitch time
        double expectedGoalsConceded = teamProjection.expectedGoalsConceded() * (projectedMinutes / 90.0);

        // Clean sheet probability: requires 60+ minutes played and 0 team goals conceded: P(CS) = P(M>=60) * P(GC=0)
        double cleanSheetProbability = plus60probability * PoissonMathUtils.calculateProbability(expectedGoalsConceded, 0);
        cleanSheetProbability = Math.clamp(cleanSheetProbability, 0.0, 1.0);

        // Official FPL rules: only Goalkeepers and Defenders incur deductions for 2+ goals conceded
        double goalsConcededPenalty;
        if (player.getPosition() == null || player.getPosition() == Position.FWD || player.getPosition() == Position.MID) {
            goalsConcededPenalty = 0.0;
        } else {
            goalsConcededPenalty = PoissonMathUtils.calculateExpectedGoalsConcededPenalty(expectedGoalsConceded);
        }

        // Aggregate recent performance logs
        double totalRecentMinutes = 0.0;
        double totalRecentSaves = 0.0;
        double totalRecentDefCon = 0.0;

        if (gameweekLogs != null) {
            for (PlayerGameweekLog log : gameweekLogs) {
                if (log == null || log.getOutcome() == null) continue;

                Integer mins = log.getOutcome().getActualMinutes();
                if (mins != null && mins > 0) {
                    totalRecentMinutes += mins;

                    if (log.getOutcome().getActualSaves() != null) {
                        totalRecentSaves += log.getOutcome().getActualSaves();
                    }
                    if (log.getOutcome().getActualDefensiveContribution() != null) {
                        totalRecentDefCon += log.getOutcome().getActualDefensiveContribution();
                    }
                }
            }
        }

        // Project goalkeeper saves if applicable
        double expectedSaves = 0.0;
        if (player.getPosition() == Position.GKP) {
            double seasonSavesPer90 = resolveSavesPer90(player);
            double blendedSavesPer90 = blendRatePer90(seasonSavesPer90, totalRecentSaves, totalRecentMinutes);
            expectedSaves = blendedSavesPer90 * (projectedMinutes / 90.0);
        }

        // Blend season and recent defensive contribution rates (DefCon per 90)
        double seasonDefCon90 = resolveDefensiveContributionPer90(player);
        double blendedDefCon90 = blendRatePer90(seasonDefCon90, totalRecentDefCon, totalRecentMinutes);

        // Expected defensive actions (mean of Poisson distribution)
        double projectedDefCon = blendedDefCon90 * (projectedMinutes / 90.0);

        // Tail probability of earning defensive bonus threshold: P(DefCon >= 10)
        double projectedDefConProb = PoissonMathUtils.calculateTailProbability(projectedDefCon, 10);

        return new PlayerDefenceResult(
                cleanSheetProbability,
                expectedGoalsConceded,
                goalsConcededPenalty,
                expectedSaves,
                projectedDefCon,
                projectedDefConProb
        );
    }

    /**
     * Resolves the seasonal saves per 90 minutes for a goalkeeper.
     *
     * Falls back to the Premier League positional baseline if empirical metrics are absent.
     *
     * @param player the goalkeeper entity
     * @return saves per 90 minutes rate
     */
    private double resolveSavesPer90(Player player) {
        if (player == null || player.getPosition() != Position.GKP) {
            return 0.0;
        }

        if (player.getPerformance() != null && player.getPerformance().getSavesPer90() != null && player.getPerformance().getSavesPer90() > 0.0) {
            return player.getPerformance().getSavesPer90();
        }

        return PRIOR_SAVES_GKP;
    }

    /**
     * Resolves the baseline defensive contribution per 90 minutes rate.
     *
     * Uses recorded performance data if available; otherwise falls back to positional priors.
     *
     * @param player the player entity being evaluated
     * @return baseline DefCon per 90 minutes
     */
    private double resolveDefensiveContributionPer90(Player player) {
        if (player == null) {
            return 0.0;
        }

        if (player.getPerformance() != null && player.getPerformance().getDefensiveContributionPer90() != null && player.getPerformance().getDefensiveContributionPer90() > 0.0) {
            return player.getPerformance().getDefensiveContributionPer90();
        }

        if (player.getPosition() != null) {
            return switch (player.getPosition()) {
                case DEF -> PRIOR_DEFCON_DEF;
                case MID -> PRIOR_DEFCON_MID;
                case FWD -> PRIOR_DEFCON_FWD;
                case GKP -> PRIOR_DEFCON_GKP;
            };
        }

        return 0.0;
    }

    /**
     * Blends a seasonal per-90 rate with empirical observations from recent match logs.
     *
     * @param seasonRatePer90    the baseline seasonal rate
     * @param totalRecentMetric  total count of actions/events observed recently
     * @param totalRecentMinutes total minutes played across recent logs
     * @return blended rate per 90 minutes
     */
    private double blendRatePer90(double seasonRatePer90, double totalRecentMetric, double totalRecentMinutes) {
        if (totalRecentMinutes < MIN_MINUTES_TO_CONSIDER) {
            return seasonRatePer90;
        }

        double recentRatePer90 = (totalRecentMetric / totalRecentMinutes) * 90.0;
        double weight = MAX_RECENT_WEIGHT * Math.min(1.0, totalRecentMinutes / SAMPLE_MINUTES_THRESHOLD);

        return (1.0 - weight) * seasonRatePer90 + weight * recentRatePer90;
    }
}