package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.service.projection.model.PlayerAttackResult;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import org.springframework.stereotype.Component;

import java.util.List;
/*
    Calculate expected goals and assists for one player
 */
@Component
public class PlayerAttackEngine {

    // Priors per position when advanced metrics don't exist
    private static final double PRIOR_XG_FWD = 0.35;
    private static final double PRIOR_XG_MID = 0.16;
    private static final double PRIOR_XG_DEF = 0.04;

    private static final double PRIOR_XA_FWD = 0.14;
    private static final double PRIOR_XA_MID = 0.18;
    private static final double PRIOR_XA_DEF = 0.08;

    // Hard statistical ceilings to eliminate API anomalies/glitches
    private static final double MAX_REALISTIC_XG_PER_90 = 1.15;
    private static final double MAX_REALISTIC_XA_PER_90 = 0.70;

    // Minimum minutes in current season before trusting raw per90 stats
    private static final double MIN_SEASON_SAMPLE_MINUTES = 270.0;

    // Average premier league team xG
    private static final double LEAGUE_AVG_TEAM_XG = 1.35;

    // Constants used for player gameweek logs calculations
    private static final double MAX_RECENT_WEIGHT = 0.35;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    public PlayerAttackResult calculatePlayerAttack(Player player, double projectedMinutes, TeamStrengthProjection teamProjection, List<PlayerGameweekLog> gameweekLogs) {

        if (player == null || teamProjection == null || projectedMinutes <= 0) {
            return new PlayerAttackResult(0.0, 0.0,0.0);
        }

        double rawRatio = teamProjection.expectedGoals() / LEAGUE_AVG_TEAM_XG;
        double attackFactor = Math.sqrt(rawRatio);
        attackFactor = Math.clamp(attackFactor, 0.6, 1.5);

        // Seasonal xGper90
        double seasonxGper90 = resolveExpectedGoalsPer90(player);
        double seasonxAper90 = resolveExpectedAssistsPer90(player);

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
        /*
            If total recent minutes are less than the threshold we use only the season baselines.
         */
        if (totalRecentMinutes < MIN_MINUTES_TO_CONSIDER) {
            finalXgPer90 = seasonxGper90;
            finalXaPer90 = seasonxAper90;
        } else {
            double weight = MAX_RECENT_WEIGHT * Math.min(1.0, totalRecentMinutes / SAMPLE_MINUTES_THRESHOLD);
            double recentXgPer90 = Math.min((totalRecentXG / totalRecentMinutes) * 90.0, MAX_REALISTIC_XG_PER_90);
            double recentXAPer90 = Math.min((totalRecentXA / totalRecentMinutes) * 90.0, MAX_REALISTIC_XA_PER_90);

            finalXgPer90 = (1.0 - weight) * seasonxGper90 + weight * recentXgPer90;
            finalXaPer90 = (1.0 - weight) * seasonxAper90 + weight * recentXAPer90;
        }

        double minutesRatio = projectedMinutes / 90.0;
        double xG = finalXgPer90 * minutesRatio * attackFactor;
        double xAs = finalXaPer90 * minutesRatio * attackFactor;

        return new PlayerAttackResult(xG, xAs, xG + xAs);
    }

    /*
     Function to get xGPer90 in case we don't have the necessary information. Using PL average per position
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

            // If he practically didn't play we trust prior
            if (seasonMinutes < 45.0) {
                return prior;
            }

            // Clamping in realistic ceiling before blend
            rawPer90 = Math.clamp(rawPer90, 0.0, MAX_REALISTIC_XG_PER_90);

            // Bayesian Shrinkage to prior regarding minutes played
            double weight = Math.min(1.0, seasonMinutes / MIN_SEASON_SAMPLE_MINUTES);
            return (weight * rawPer90) + ((1.0 - weight) * prior);
        }

        return prior;
    }
    /*
     Function to get xAPer90 in case we don't have the necessary information. Using PL average per position
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

    private double getPriorXG(Player player) {
        if (player.getPosition() == null) return 0.0;
        return switch (player.getPosition()) {
            case FWD -> PRIOR_XG_FWD;
            case MID -> PRIOR_XG_MID;
            case DEF -> PRIOR_XG_DEF;
            case GKP -> 0.0;
        };
    }

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
