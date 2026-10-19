package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.service.projection.model.PlayerAttackResult;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computational engine responsible for projecting individual attacking output (xG and xA).
 *
 * Employs a multi-layered Bayesian estimation framework:
 * 1. Establishes positional priors when sample sizes or underlying statistics are insufficient.
 * 2. Applies Bayesian shrinkage to season-long metrics based on total accumulated minutes.
 * 3. Incorporates recent match momentum from historical gameweek logs.
 * 4. Adjusts individual production based on team fixture strength and expected team goals.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
public class PlayerAttackEngine {

    // Positional prior expected goals (xG per 90) when metrics are missing or unreliable
    private static final double PRIOR_XG_FWD = 0.35;
    private static final double PRIOR_XG_MID = 0.16;
    private static final double PRIOR_XG_DEF = 0.04;

    // Positional prior expected assists (xA per 90) when metrics are missing or unreliable
    private static final double PRIOR_XA_FWD = 0.14;
    private static final double PRIOR_XA_MID = 0.18;
    private static final double PRIOR_XA_DEF = 0.08;

    // Hard statistical ceilings per 90 minutes to eliminate API anomalies and small-sample spikes
    private static final double MAX_REALISTIC_XG_PER_90 = 1.15;
    private static final double MAX_REALISTIC_XA_PER_90 = 0.70;

    // Minimum accumulated season minutes required to fully trust empirical season stats
    private static final double MIN_SEASON_SAMPLE_MINUTES = 270.0;

    // Benchmark Premier League team expected goals per fixture
    private static final double LEAGUE_AVG_TEAM_XG = 1.35;

    // Weighting and threshold boundaries for recent form calculations
    private static final double MAX_RECENT_WEIGHT = 0.35;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    /**
     * Calculates projected expected goals (xG), expected assists (xA), and total offensive involvements (xGI).
     *
     * Derives final per-90 rates by blending Bayesian season baselines with recent gameweek form,
     * scales by the player's projected on-pitch minutes ratio, and adjusts for team offensive fixture strength.
     *
     * @param player            the player entity to project
     * @param projectedMinutes  the expected playing time in minutes for the upcoming fixture
     * @param teamProjection    team-level strength context and expected team goals for the match
     * @param gameweekLogs      recent match logs for the player used to assess short-term momentum
     * @return a PlayerAttackResult containing projected xG, xA, and combined xGI
     */
    public PlayerAttackResult calculatePlayerAttack(
            Player player,
            double projectedMinutes,
            TeamStrengthProjection teamProjection,
            List<PlayerGameweekLog> gameweekLogs
    ) {
        if (player == null || teamProjection == null || projectedMinutes <= 0) {
            return new PlayerAttackResult(0.0, 0.0, 0.0);
        }

        // Team fixture scaling factor: square root dampens extreme team ratings
        double rawRatio = teamProjection.expectedGoals() / LEAGUE_AVG_TEAM_XG;
        double attackFactor = Math.sqrt(rawRatio);
        attackFactor = Math.clamp(attackFactor, 0.6, 1.5);

        // Compute season baseline rates using Bayesian shrinkage
        double seasonxGper90 = resolveExpectedGoalsPer90(player);
        double seasonxAper90 = resolveExpectedAssistsPer90(player);

        // Aggregate recent performance logs
        double totalRecentMinutes = 0.0;
        double totalRecentXG = 0.0;
        double totalRecentXA = 0.0;

        if (gameweekLogs != null) {
            for (PlayerGameweekLog log : gameweekLogs) {
                if (log == null || log.getOutcome() == null) continue;
                if (log.getOutcome().getActualMinutes() != null && log.getOutcome().getActualMinutes() >= 0.0) {
                    totalRecentMinutes += log.getOutcome().getActualMinutes();
                }
                if (log.getOutcome().getActualXg() != null && log.getOutcome().getActualXg() >= 0.0) {
                    totalRecentXG += log.getOutcome().getActualXg();
                }
                if (log.getOutcome().getActualXa() != null && log.getOutcome().getActualXa() >= 0.0) {
                    totalRecentXA += log.getOutcome().getActualXa();
                }
            }
        }

        double finalXgPer90;
        double finalXaPer90;

        // If recent sample is negligible, rely entirely on the blended season baseline
        if (totalRecentMinutes < MIN_MINUTES_TO_CONSIDER) {
            finalXgPer90 = seasonxGper90;
            finalXaPer90 = seasonxAper90;
        } else {
            // Dynamically scale recent weight up to MAX_RECENT_WEIGHT as minutes approach the sample threshold
            double weight = MAX_RECENT_WEIGHT * Math.min(1.0, totalRecentMinutes / SAMPLE_MINUTES_THRESHOLD);
            double recentXgPer90 = Math.min((totalRecentXG / totalRecentMinutes) * 90.0, MAX_REALISTIC_XG_PER_90);
            double recentXAPer90 = Math.min((totalRecentXA / totalRecentMinutes) * 90.0, MAX_REALISTIC_XA_PER_90);

            finalXgPer90 = (1.0 - weight) * seasonxGper90 + weight * recentXgPer90;
            finalXaPer90 = (1.0 - weight) * seasonxAper90 + weight * recentXAPer90;
        }

        // Scale per-90 rates by projected pitch time and team fixture multiplier
        double minutesRatio = projectedMinutes / 90.0;
        double xG = finalXgPer90 * minutesRatio * attackFactor;
        double xAs = finalXaPer90 * minutesRatio * attackFactor;

        return new PlayerAttackResult(xG, xAs, xG + xAs);
    }

    /**
     * Resolves expected goals per 90 (xG90) using Bayesian shrinkage against positional priors.
     *
     * If sample minutes are fewer than 45, the model relies completely on the positional prior.
     * As accumulated playing time grows toward MIN_SEASON_SAMPLE_MINUTES (270 min), the empirical
     * rate is given progressively more weight.
     *
     * @param player the player entity being assessed
     * @return the shrinkage-adjusted expected goals per 90 minutes
     */
    private double resolveExpectedGoalsPer90(Player player) {
        if (player == null) return 0.0;

        double prior = getPriorXG(player);

        if (player.getAdvancedMetrics() != null
                && player.getAdvancedMetrics().getExpectedGoalsPer90() != null
                && player.getPerformance() != null
                && player.getPerformance().getMinutes() != null) {

            double seasonMinutes = player.getPerformance().getMinutes();
            double rawPer90 = player.getAdvancedMetrics().getExpectedGoalsPer90();

            // Default to prior if the player has negligible playing time
            if (seasonMinutes < 45.0) {
                return prior;
            }

            // Clamp outliers to the statistical ceiling before shrinkage
            rawPer90 = Math.clamp(rawPer90, 0.0, MAX_REALISTIC_XG_PER_90);

            // Bayesian shrinkage weighting factor W = min(1.0, minutes / 270)
            double weight = Math.min(1.0, seasonMinutes / MIN_SEASON_SAMPLE_MINUTES);
            return (weight * rawPer90) + ((1.0 - weight) * prior);
        }

        return prior;
    }

    /**
     * Resolves expected assists per 90 (xA90) using Bayesian shrinkage against positional priors.
     *
     * Clamps empirical rates to MAX_REALISTIC_XA_PER_90 and smoothly transitions from positional
     * prior to empirical performance based on total season playing minutes.
     *
     * @param player the player entity being assessed
     * @return the shrinkage-adjusted expected assists per 90 minutes
     */
    private double resolveExpectedAssistsPer90(Player player) {
        if (player == null) return 0.0;

        double prior = getPriorXA(player);

        if (player.getAdvancedMetrics() != null
                && player.getAdvancedMetrics().getExpectedAssistsPer90() != null
                && player.getPerformance() != null
                && player.getPerformance().getMinutes() != null) {

            double seasonMinutes = player.getPerformance().getMinutes();
            double rawPer90 = player.getAdvancedMetrics().getExpectedAssistsPer90();

            if (seasonMinutes < 45.0) {
                return prior;
            }

            rawPer90 = Math.clamp(rawPer90, 0.0, MAX_REALISTIC_XA_PER_90);

            double weight = Math.min(1.0, seasonMinutes / MIN_SEASON_SAMPLE_MINUTES);
            return (weight * rawPer90) + ((1.0 - weight) * prior);
        }

        return prior;
    }

    /**
     * Resolves the default prior expected goals (xG per 90) for a given pitch position.
     *
     * @param player the target player
     * @return prior xG per 90
     */
    private double getPriorXG(Player player) {
        if (player.getPosition() == null) return 0.0;
        return switch (player.getPosition()) {
            case FWD -> PRIOR_XG_FWD;
            case MID -> PRIOR_XG_MID;
            case DEF -> PRIOR_XG_DEF;
            case GKP -> 0.0;
        };
    }

    /**
     * Resolves the default prior expected assists (xA per 90) for a given pitch position.
     *
     * @param player the target player
     * @return prior xA per 90
     */
    private double getPriorXA(Player player) {
        if (player.getPosition() == null) return 0.0;
        return switch (player.getPosition()) {
            case FWD -> PRIOR_XA_FWD;
            case MID -> PRIOR_XA_MID;
            case DEF -> PRIOR_XA_DEF;
            case GKP -> 0.0;
        };
    }
}