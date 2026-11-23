package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.player.PlayerGameweekOutcome;
import com.fploptimizer.backend.service.projection.model.PlayerAttackResult;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computational engine responsible for projecting individual attacking output (xG and xA).
 * <p>
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
     * <p>
     * Derives final per-90 rates by blending Bayesian season baselines with recent gameweek form,
     * scales by the player's projected on-pitch minutes ratio, and adjusts for team offensive fixture strength.
     *
     * @param player           the player entity to project
     * @param projectedMinutes the expected playing time in minutes for the upcoming fixture
     * @param teamProjection   team-level strength context and expected team goals for the match
     * @param gameweekLogs     recent match logs for the player used to assess short-term momentum
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
        double attackFactor = Math.clamp(Math.sqrt(rawRatio), 0.6, 1.5);

        // Compute season baseline rates using Bayesian shrinkage
        double seasonXgPer90 = resolveExpectedGoalsPer90(player);
        double seasonXaPer90 = resolveExpectedAssistsPer90(player);

        // Aggregate recent performance logs
        RecentAttackTotals recent = aggregateRecentLogs(gameweekLogs);

        double finalXgPer90;
        double finalXaPer90;

        // If recent sample is negligible, rely entirely on the blended season baseline
        if (recent.minutes() < MIN_MINUTES_TO_CONSIDER) {
            finalXgPer90 = seasonXgPer90;
            finalXaPer90 = seasonXaPer90;
        } else {
            // Dynamically scale recent weight up to MAX_RECENT_WEIGHT as minutes approach the sample threshold
            double weight = MAX_RECENT_WEIGHT * Math.min(1.0, recent.minutes() / SAMPLE_MINUTES_THRESHOLD);
            double recentXgPer90 = Math.min((recent.xg() / recent.minutes()) * 90.0, MAX_REALISTIC_XG_PER_90);
            double recentXaPer90 = Math.min((recent.xa() / recent.minutes()) * 90.0, MAX_REALISTIC_XA_PER_90);

            finalXgPer90 = (1.0 - weight) * seasonXgPer90 + weight * recentXgPer90;
            finalXaPer90 = (1.0 - weight) * seasonXaPer90 + weight * recentXaPer90;
        }

        // Scale per-90 rates by projected pitch time and team fixture multiplier
        double minutesRatio = projectedMinutes / 90.0;
        double xG = finalXgPer90 * minutesRatio * attackFactor;
        double xAs = finalXaPer90 * minutesRatio * attackFactor;

        return new PlayerAttackResult(xG, xAs, xG + xAs);
    }

    /**
     * Aggregates realized minutes, expected goals, and expected assists across historical match logs.
     */
    private RecentAttackTotals aggregateRecentLogs(List<PlayerGameweekLog> gameweekLogs) {
        if (gameweekLogs == null || gameweekLogs.isEmpty()) {
            return new RecentAttackTotals(0.0, 0.0, 0.0);
        }

        double totalMinutes = 0.0;
        double totalXg = 0.0;
        double totalXa = 0.0;

        for (PlayerGameweekLog log : gameweekLogs) {
            PlayerGameweekOutcome outcome = (log != null) ? log.getOutcome() : null;
            if (outcome == null) {
                continue;
            }
            if (outcome.getActualMinutes() != null && outcome.getActualMinutes() >= 0.0) {
                totalMinutes += outcome.getActualMinutes();
            }
            totalXg += nonNegativeOrZero(outcome.getActualXg());
            totalXa += nonNegativeOrZero(outcome.getActualXa());
        }

        return new RecentAttackTotals(totalMinutes, totalXg, totalXa);
    }

    private double nonNegativeOrZero(Double value) {
        return (value != null && value >= 0.0) ? value : 0.0;
    }

    private record RecentAttackTotals(double minutes, double xg, double xa) {
    }

    /**
     * Resolves expected goals per 90 (xG90) using Bayesian shrinkage against positional priors.
     * <p>
     * If sample minutes are fewer than 45, the model relies completely on the positional prior.
     * As accumulated playing time grows toward MIN_SEASON_SAMPLE_MINUTES (270 min), the empirical
     * rate is given progressively more weight.
     *
     * @param player the player entity being assessed
     * @return the shrinkage-adjusted expected goals per 90 minutes
     */
    private double resolveExpectedGoalsPer90(Player player) {
        if (player == null) {
            return 0.0;
        }

        double prior = getPriorXG(player);

        if (player.getAdvancedMetrics() != null
                && player.getAdvancedMetrics().getExpectedGoalsPer90() != null
                && player.getPerformance() != null
                && player.getPerformance().getMinutes() != null) {

            double seasonMinutes = player.getPerformance().getMinutes();
            double rawPer90 = player.getAdvancedMetrics().getExpectedGoalsPer90();

            if (seasonMinutes < 45.0) {
                return prior;
            }

            rawPer90 = Math.clamp(rawPer90, 0.0, MAX_REALISTIC_XG_PER_90);
            double weight = Math.min(1.0, seasonMinutes / MIN_SEASON_SAMPLE_MINUTES);
            return (weight * rawPer90) + ((1.0 - weight) * prior);
        }

        return prior;
    }

    /**
     * Resolves expected assists per 90 (xA90) using Bayesian shrinkage against positional priors.
     * <p>
     * Clamps empirical rates to MAX_REALISTIC_XA_PER_90 and smoothly transitions from positional
     * prior to empirical performance based on total season playing minutes.
     *
     * @param player the player entity being assessed
     * @return the shrinkage-adjusted expected assists per 90 minutes
     */
    private double resolveExpectedAssistsPer90(Player player) {
        if (player == null) {
            return 0.0;
        }

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
        if (player.getPosition() == null) {
            return 0.0;
        }
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
        if (player.getPosition() == null) {
            return 0.0;
        }
        return switch (player.getPosition()) {
            case FWD -> PRIOR_XA_FWD;
            case MID -> PRIOR_XA_MID;
            case DEF -> PRIOR_XA_DEF;
            case GKP -> 0.0;
        };
    }
}