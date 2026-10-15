package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.service.projection.model.PlayerDefenceResult;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import com.fploptimizer.backend.service.projection.util.PoissonMathUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlayerDefenceEngine {
    // Priors for Saves per 90
    private static final double PRIOR_SAVES_GKP = 3.2; // Average Gkp Premier League

    // Priors for DefConPer 90
    private static final double PRIOR_DEFCON_DEF = 11.0;
    private static final double PRIOR_DEFCON_MID = 6.0;
    private static final double PRIOR_DEFCON_FWD = 2.5;
    private static final double PRIOR_DEFCON_GKP = 1.0;

    // Constants used for player gameweek logs calculations
    private static final double MAX_RECENT_WEIGHT = 0.35;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    public PlayerDefenceResult calculatePlayerDefence(
            Player player,
            double projectedMinutes,
            double plus60probability,
            TeamStrengthProjection teamProjection,
            List<PlayerGameweekLog> gameweekLogs) {

        if (player == null || teamProjection == null || projectedMinutes <= 0) {
            return new PlayerDefenceResult(0.0,0.0,0.0, 0.0, 0.0, 0.0);
        }

        double expectedGoalsConceded = teamProjection.expectedGoalsConceded() * (projectedMinutes / 90.0);

        double cleanSheetProbability = plus60probability * PoissonMathUtils.calculateProbability(expectedGoalsConceded, 0);

        cleanSheetProbability = Math.clamp(cleanSheetProbability, 0.0, 1.0);

        double goalsConcededPenalty;
        if (player.getPosition() == null || player.getPosition() == Position.FWD || player.getPosition() == Position.MID)
        {
            goalsConcededPenalty = 0.0;
        }else {
            goalsConcededPenalty = PoissonMathUtils.calculateExpectedGoalsConcededPenalty(expectedGoalsConceded);
        }
        // calculate sum from logs
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

        double expectedSaves = 0.0;
        if (player.getPosition() == Position.GKP){

            double seasonSavesPer90 = resolveSavesPer90(player);

            double blendedSavesPer90 = blendRatePer90(seasonSavesPer90, totalRecentSaves, totalRecentMinutes);

            expectedSaves = blendedSavesPer90 * (projectedMinutes / 90.0);
        }
        // season baseline or prior for position
        double seasonDefCon90 = resolveDefensiveContributionPer90(player);

        // Blending season and logs
        double blendedDefCon90 = blendRatePer90(seasonDefCon90, totalRecentDefCon, totalRecentMinutes);

        // Expected defcon for match (μ_def)
        double projectedDefCon= blendedDefCon90 * (projectedMinutes / 90.0);

        //  P(DefCon >= 10) of Poisson
        double projectedDefConProb = PoissonMathUtils.calculateTailProbability(projectedDefCon, 10);

        return new PlayerDefenceResult(cleanSheetProbability, expectedGoalsConceded,  goalsConcededPenalty, expectedSaves, projectedDefCon, projectedDefConProb);
    }

    private double resolveSavesPer90(Player player) {
        if (player == null || player.getPosition() != Position.GKP  ) {
            return 0.0;
        }

        // Αν το πεδίο βρίσκεται στο player (ή στο player.getAdvancedMetrics())
        if (player.getPerformance() != null  && player.getPerformance().getSavesPer90() != null && player.getPerformance().getSavesPer90()> 0.0) {
            return player.getPerformance().getSavesPer90();
        }

        return PRIOR_SAVES_GKP;
    }

    private double resolveDefensiveContributionPer90(Player player) {
        if (player == null) {
            return 0.0;
        }

        // Saved player prior check
        if (player.getPerformance() != null &&player.getPerformance().getDefensiveContributionPer90() != null && player.getPerformance().getDefensiveContributionPer90() > 0.0) {
            return player.getPerformance().getDefensiveContributionPer90();
        }

        // Fallback to priors per position
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

    private double blendRatePer90(double seasonRatePer90, double totalRecentMetric, double totalRecentMinutes) {
        // Not enough minutes we trust seasonRate
        if (totalRecentMinutes < MIN_MINUTES_TO_CONSIDER) {
            return seasonRatePer90;
        }

        // reccent Rate per 90
        double recentRatePer90 = (totalRecentMetric / totalRecentMinutes) * 90.0;

        // Dynamic weight depending recent minutes
        double weight = MAX_RECENT_WEIGHT * Math.min(1.0, totalRecentMinutes / SAMPLE_MINUTES_THRESHOLD);

        // Convex combination (blended rate)
        return (1.0 - weight) * seasonRatePer90 + weight * recentRatePer90;
    }

}
