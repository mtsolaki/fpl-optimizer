package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.service.projection.model.PlayerCardsResult;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * Computational engine responsible for evaluating disciplinary risk (expected yellow and red cards).
 *
 * Implements a Bayesian blending approach:
 * 1. Establishes positional priors based on historical Premier League disciplinary benchmarks.
 * 2. Derives baseline season-long per-90 rates from accumulated player performance metrics.
 * 3. Blends seasonal expectations with short-term form from recent match logs.
 * 4. Scales blended card rates by the player's projected pitch minutes.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
public class PlayerCardsEngine {

    // Priors for yellow cards per 90 mins (Premier League averages)
    private static final double PRIOR_YELLOW_DEF = 0.14;
    private static final double PRIOR_YELLOW_MID = 0.12;
    private static final double PRIOR_YELLOW_FWD = 0.08;
    private static final double PRIOR_YELLOW_GKP = 0.02;

    // Priors for red cards per 90 mins
    private static final double PRIOR_RED_DEF = 0.006;
    private static final double PRIOR_RED_MID = 0.005;
    private static final double PRIOR_RED_FWD = 0.003;
    private static final double PRIOR_RED_GKP = 0.001;

    // Constants for blending with logs
    private static final double MAX_RECENT_WEIGHT = 0.35;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    /**
     * Projects expected yellow and red cards for a player in an upcoming match.
     *
     * Aggregates recent cards and minutes from historical logs, blends empirical rates
     * with seasonal or prior baselines, and normalizes by projected playing time.
     *
     * @param player           the target player entity
     * @param projectedMinutes expected on-pitch playing time in minutes
     * @param gameweekLogs     historical match logs for recent disciplinary trends
     * @return a PlayerCardsResult containing projected expected yellow and red cards
     */
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

    /**
     * Resolves the baseline expected yellow cards per 90 minutes.
     *
     * If the player has accumulated at least 90 minutes in the current campaign,
     * the empirical rate is utilized; otherwise, defaults to the positional prior.
     *
     * @param player the player entity being evaluated
     * @return seasonal yellow card rate per 90 minutes
     */
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

    /**
     * Resolves the baseline expected red cards per 90 minutes.
     *
     * If empirical minutes meet the 90-minute threshold, calculates the player's realized
     * rate; otherwise, falls back to positional prior expectations.
     *
     * @param player the player entity being evaluated
     * @return seasonal red card rate per 90 minutes
     */
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

    /**
     * Blends the long-term seasonal per-90 rate with the player's recent sample rate.
     *
     * Applies dynamic weight scaling up to MAX_RECENT_WEIGHT as recent minutes
     * approach the sample threshold.
     *
     * @param seasonRatePer90    the baseline seasonal rate
     * @param totalRecentMetric  total count of events (cards) across recent gameweeks
     * @param totalRecentMinutes total minutes played across recent gameweeks
     * @return the blended per-90 rate
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
