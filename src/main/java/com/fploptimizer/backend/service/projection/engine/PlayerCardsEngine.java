package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.service.projection.model.PlayerCardsResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlayerCardsEngine {

    // Priors για κίτρινες κάρτες ανά 90λεπτο (Premier League averages)
    private static final double PRIOR_YELLOW_DEF = 0.14;
    private static final double PRIOR_YELLOW_MID = 0.12;
    private static final double PRIOR_YELLOW_FWD = 0.08;
    private static final double PRIOR_YELLOW_GKP = 0.02;

    // Priors για κόκκινες κάρτες ανά 90λεπτο
    private static final double PRIOR_RED_DEF = 0.006;
    private static final double PRIOR_RED_MID = 0.005;
    private static final double PRIOR_RED_FWD = 0.003;
    private static final double PRIOR_RED_GKP = 0.001;

    // Constants για το blending με τα logs
    private static final double MAX_RECENT_WEIGHT = 0.35;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    public PlayerCardsResult calculatePlayerCards(
            Player player,
            double projectedMinutes,
            List<PlayerGameweekLog> gameweekLogs
    ){
        if (player == null || projectedMinutes <= 0.0) return new PlayerCardsResult(0.0, 0.0);

        double totalRecentMinutes = 0.0;
        double totalRecentYellows = 0.0;
        double totalRecentReds = 0.0;

        if (gameweekLogs != null) {
            for (PlayerGameweekLog log : gameweekLogs) {
                if (log == null || log.getOutcome() == null) continue;

                Integer mins = log.getOutcome().getActualMinutes();
                if (mins != null && mins > 0) {
                    totalRecentMinutes += mins;

                    if (log.getOutcome().getActualYellowCards() != null) {
                        totalRecentYellows += log.getOutcome().getActualYellowCards();
                    }
                    if (log.getOutcome().getActualRedCards() != null) {
                        totalRecentReds += log.getOutcome().getActualRedCards();
                    }
                }
            }
        }

        // Macro baselines from season or prior
        double seasonYellow90 = resolveYellowCardsPer90(player);
        double seasonRed90 = resolveRedCardsPer90(player);

        // Blending season and recent games
        double blendedYellow90 = blendRatePer90(seasonYellow90, totalRecentYellows, totalRecentMinutes);
        double blendedRed90 = blendRatePer90(seasonRed90, totalRecentReds, totalRecentMinutes);

        // Escalation based on expected minutes played
        double minutesRatio = projectedMinutes / 90.0;
        double expectedYellowCards = blendedYellow90 * minutesRatio;
        double expectedRedCards = blendedRed90 * minutesRatio;

        return new PlayerCardsResult(expectedYellowCards, expectedRedCards);

    }

    private double resolveYellowCardsPer90(Player player) {
        if (player == null) {
            return 0.0;
        }

        if (player.getPerformance() != null
                && player.getPerformance().getMinutes() != null
                && player.getPerformance().getMinutes() >= 90
                && player.getPerformance().getYellowCards() != null) {
            return ((double) player.getPerformance().getYellowCards() / player.getPerformance().getMinutes()) * 90.0;
        }

        if (player.getPosition() != null) {
            return switch (player.getPosition()) {
                case DEF -> PRIOR_YELLOW_DEF;
                case MID -> PRIOR_YELLOW_MID;
                case FWD -> PRIOR_YELLOW_FWD;
                case GKP -> PRIOR_YELLOW_GKP;
            };
        }

        return 0.0;
    }

    private double resolveRedCardsPer90(Player player) {
        if (player == null) {
            return 0.0;
        }

        if (player.getPerformance() != null
                && player.getPerformance().getMinutes() != null
                && player.getPerformance().getMinutes() >= 90
                && player.getPerformance().getRedCards() != null) {
            return ((double) player.getPerformance().getRedCards() / player.getPerformance().getMinutes()) * 90.0;
        }

        if (player.getPosition() != null) {
            return switch (player.getPosition()) {
                case DEF -> PRIOR_RED_DEF;
                case MID -> PRIOR_RED_MID;
                case FWD -> PRIOR_RED_FWD;
                case GKP -> PRIOR_RED_GKP;
            };
        }

        return 0.0;
    }

    private double blendRatePer90(double seasonRatePer90, double totalRecentMetric, double totalRecentMinutes) {
        if (totalRecentMinutes < MIN_MINUTES_TO_CONSIDER) {
            return seasonRatePer90;
        }

        double recentRatePer90 = (totalRecentMetric / totalRecentMinutes) * 90.0;
        double weight = MAX_RECENT_WEIGHT * Math.min(1.0, totalRecentMinutes / SAMPLE_MINUTES_THRESHOLD);

        return (1.0 - weight) * seasonRatePer90 + weight * recentRatePer90;
    }
}
