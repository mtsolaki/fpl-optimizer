package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.entity.player.PlayerAdvancedMetrics;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.service.projection.model.PlayerMinutesResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Computational engine responsible for projecting expected player minutes and appearance probabilities.
 * <p>
 * Deconstructs on-pitch time into starting probability and substitute appearance probability,
 * factors in medical and suspension availability flags, and employs Bayesian smoothing to estimate
 * the likelihood of completing the 60-minute threshold required for FPL clean sheet and appearance points.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
public class PlayerMinutesEngine {

    // Default playing time expectations in minutes
    private static final double DEFAULT_STARTER_MINUTES = 82.0;
    private static final double DEFAULT_SUB_MINUTES = 20.0;

    // Substitute appearance probabilities given that the player does not start
    private static final double GKP_SUB_PROBABILITY = 0.01;
    private static final double OUTFIELD_SUB_PROBABILITY = 0.35;

    // Positional priors for completing 60+ minutes given that the player starts
    private static final double PRIOR_60_MIN_GKP = 0.98;
    private static final double PRIOR_60_MIN_DEF = 0.92;
    private static final double PRIOR_60_MIN_MID = 0.82;
    private static final double PRIOR_60_MIN_FWD = 0.78;

    // Bayesian shrinkage pseudo-starts weight to smooth small sample sizes
    private static final double PSEUDO_STARTS_WEIGHT = 3.0;

    // Minimum combined expected goal involvement per 90 minutes to classify as a team talisman
    private static final double TALISMAN_XGI_THRESHOLD = 0.50;

    /**
     * Projects on-pitch minutes, starting probability, and 60+ minutes probability.
     * <p>
     * Evaluates official availability news, calculates rotation risk from recent logs,
     * blends starter and substitute expected playing time, and clamps results within [0.0, 90.0].
     *
     * @param player       the target player entity
     * @param gameweekLogs historical match logs reflecting recent manager selection trends
     * @return a PlayerMinutesResult containing projected minutes, start probability, and 60+ minute probability
     */
    public PlayerMinutesResult calculateMinutes(Player player, List<PlayerGameweekLog> gameweekLogs) {
        if (player == null) {
            return new PlayerMinutesResult(0.0, 0.0, 0.0);
        }

        double availability = resolveAvailability(player);
        if (availability <= 0.0) {
            return new PlayerMinutesResult(0.0, 0.0, 0.0);
        }

        double conditionalStartProb = calculateStartProbability(player, gameweekLogs);
        double finalStartProbability = Math.clamp(availability * conditionalStartProb, 0.0, 1.0);

        double plus60GivenStart = calculateSixtyPlusGivenStart(player, gameweekLogs);
        double finalPlus60Probability = Math.clamp(finalStartProbability * plus60GivenStart, 0.0, 1.0);

        double subProbability = (player.getPosition() == Position.GKP)
                ? GKP_SUB_PROBABILITY
                : OUTFIELD_SUB_PROBABILITY;

        double expectedMinutesGivenAvailable = (conditionalStartProb * DEFAULT_STARTER_MINUTES)
                + ((1.0 - conditionalStartProb) * subProbability * DEFAULT_SUB_MINUTES);

        double projectedMinutes = Math.clamp(availability * expectedMinutesGivenAvailable, 0.0, 90.0);

        return new PlayerMinutesResult(
                projectedMinutes,
                finalStartProbability,
                finalPlus60Probability
        );
    }

    /**
     * Estimates the probability of a player playing 60+ minutes conditional on starting the match.
     * <p>
     * Utilizes a Bayesian smoothing formula against positional priors to prevent volatility
     * from small sample sizes in the early season.
     *
     * @param player       the player entity
     * @param gameweekLogs historical match logs
     * @return smoothed probability in range [0.0, 1.0]
     */
    private double calculateSixtyPlusGivenStart(Player player, List<PlayerGameweekLog> gameweekLogs) {
        double prior = getPosition60MinPrior(player.getPosition());

        if (gameweekLogs == null || gameweekLogs.isEmpty()) {
            return prior;
        }

        int startsCount = 0;
        int startsWith60PlusCount = 0;

        for (PlayerGameweekLog log : gameweekLogs) {
            if (log != null && log.getOutcome() != null && Boolean.TRUE.equals(log.getOutcome().getWasStarter())) {
                startsCount++;
                Integer minutes = log.getOutcome().getActualMinutes();
                if (minutes != null && minutes >= 60) {
                    startsWith60PlusCount++;
                }
            }
        }

        double smoothedProb = (startsWith60PlusCount + (PSEUDO_STARTS_WEIGHT * prior))
                / (startsCount + PSEUDO_STARTS_WEIGHT);

        return Math.clamp(smoothedProb, 0.0, 1.0);
    }

    /**
     * Resolves the default positional prior probability of completing 60+ minutes as a starter.
     *
     * @param position pitch position
     * @return prior probability
     */
    private double getPosition60MinPrior(Position position) {
        if (position == null) {
            return PRIOR_60_MIN_MID;
        }
        return switch (position) {
            case GKP -> PRIOR_60_MIN_GKP;
            case DEF -> PRIOR_60_MIN_DEF;
            case MID -> PRIOR_60_MIN_MID;
            case FWD -> PRIOR_60_MIN_FWD;
        };
    }

    /**
     * Evaluates official FPL news and medical availability status.
     * <p>
     * Returns the decimal probability [0.0, 1.0] representing the chance of playing this round.
     * Status codes 'i' (injured), 's' (suspended), and 'u' (unavailable) resolve to 0.0.
     *
     * @param player the target player
     * @return availability probability in range [0.0, 1.0]
     */
    private double resolveAvailability(Player player) {
        String status = player.getStatus();
        if (status == null) {
            return 0.0;
        }

        if ("a".equalsIgnoreCase(status) || "d".equalsIgnoreCase(status)) {
            PlayerMarketInfo marketInfo = player.getMarketInfo();
            if (marketInfo != null && marketInfo.getChanceOfPlayingThisRound() != null) {
                return Math.clamp(marketInfo.getChanceOfPlayingThisRound() / 100.0, 0.0, 1.0);
            }
            return 1.0;
        }

        return 0.0;
    }

    /**
     * Calculates starting probability by weighting recent lineup patterns against season averages.
     * <p>
     * Key talisman players are heavily anchored to their established season status (65% weight),
     * whereas standard rotation-risk players are weighted more by recent 3-game trends (65% weight).
     *
     * @param player       the target player
     * @param gameweekLogs historical match logs
     * @return starting probability clamped between 0.0 and 1.0
     */
    private double calculateStartProbability(Player player, List<PlayerGameweekLog> gameweekLogs) {
        if (gameweekLogs == null || gameweekLogs.isEmpty()) {
            return calculateColdStartProbability(player);
        }

        List<PlayerGameweekLog> completedLogs = gameweekLogs.stream()
                .filter(log -> log != null
                        && log.getOutcome() != null
                        && log.getOutcome().getActualMinutes() != null)
                .toList();

        if (completedLogs.isEmpty()) {
            return calculateColdStartProbability(player);
        }

        List<PlayerGameweekLog> recentLogs = completedLogs.stream()
                .filter(log -> log.getFixture() != null && log.getFixture().getGameweek() != null)
                .sorted(Comparator.comparingInt(
                        (PlayerGameweekLog log) -> log.getFixture().getGameweek()
                ).reversed())
                .limit(3)
                .toList();

        double totalRecentMinutes = recentLogs.stream()
                .mapToInt(log -> log.getOutcome().getActualMinutes())
                .sum();
        double recentRatio = recentLogs.isEmpty()
                ? 0.0
                : Math.clamp(totalRecentMinutes / (recentLogs.size() * 90.0), 0.0, 1.0);

        long matchesStarted = completedLogs.stream()
                .filter(log -> Boolean.TRUE.equals(log.getOutcome().getWasStarter()))
                .count();
        double seasonRatio = (double) matchesStarted / completedLogs.size();

        double recentWeight = isTalisman(player) ? 0.35 : 0.65;
        double seasonWeight = 1.0 - recentWeight;

        return Math.clamp((recentWeight * recentRatio) + (seasonWeight * seasonRatio), 0.0, 1.0);
    }

    /**
     * Heuristic cold-start starting probability estimation used when no completed match logs exist.
     * <p>
     * Uses market price tiers and ownership percentages as proxies for squad hierarchy.
     *
     * @param player the target player
     * @return initial estimated starting probability
     */
    private double calculateColdStartProbability(Player player) {
        if (player == null || player.getPerformance() == null) {
            return 0.0;
        }

        Integer starts = player.getPerformance().getStarts();
        if (starts != null && starts > 0) {
            return Math.clamp(starts / 38.0, 0.0, 1.0);
        }

        PlayerMarketInfo marketInfo = player.getMarketInfo();
        Position position = player.getPosition();
        if (marketInfo == null || position == null) {
            return 0.0;
        }

        double ownership = marketInfo.getSelectedByPercent() != null ? marketInfo.getSelectedByPercent() : 0.0;
        double currentPrice = marketInfo.getCurrentPrice() != null ? marketInfo.getCurrentPrice() : 0.0;

        return switch (position) {
            case GKP -> estimateGoalkeeperColdStart(currentPrice, ownership);
            case DEF -> estimateDefenderColdStart(currentPrice, ownership);
            case MID, FWD -> estimateAttackerColdStart(currentPrice, ownership);
        };
    }

    /**
     * Estimates cold-start probability for a goalkeeper based on price and ownership.
     *
     * @param currentPrice player market price
     * @param ownership    percentage of teams owning the player
     * @return estimated starting probability
     */
    private double estimateGoalkeeperColdStart(double currentPrice, double ownership) {
        if (currentPrice >= 4.5 && ownership >= 2.0) {
            return 0.95;
        }
        return ownership > 10.0 ? 0.75 : 0.05;
    }

    /**
     * Estimates cold-start probability for a defender based on price and ownership.
     *
     * @param currentPrice player market price
     * @param ownership    percentage of teams owning the player
     * @return estimated starting probability
     */
    private double estimateDefenderColdStart(double currentPrice, double ownership) {
        if (currentPrice >= 5.0 && ownership >= 10.0) {
            return 0.90;
        }
        if (currentPrice >= 4.5) {
            return ownership >= 15.0 ? 0.85 : 0.70;
        }
        if (ownership >= 20.0) {
            return 0.75;
        }
        return ownership >= 8.0 ? 0.45 : 0.15;
    }

    /**
     * Estimates cold-start probability for a midfielder or forward based on price and ownership.
     *
     * @param currentPrice player market price
     * @param ownership    percentage of teams owning the player
     * @return estimated starting probability
     */
    private double estimateAttackerColdStart(double currentPrice, double ownership) {
        if (currentPrice >= 7.5) {
            return 0.90;
        }
        if (currentPrice >= 5.5) {
            return ownership >= 15.0 ? 0.88 : 0.78;
        }
        if (ownership >= 15.0) {
            return 0.65;
        }
        return ownership >= 5.0 ? 0.40 : 0.15;
    }

    /**
     * Determines whether a player qualifies as a primary team talisman.
     * <p>
     * A player is designated as a talisman if their attacking rate (xG90 + xA90) exceeds 0.50
     * or if they are the designated first-choice penalty taker.
     *
     * @param player the player entity being assessed
     * @return true if the player is identified as a talisman, false otherwise
     */
    private boolean isTalisman(Player player) {
        if (player == null) {
            return false;
        }

        if (Integer.valueOf(1).equals(player.getPenaltiesOrder())) {
            return true;
        }

        PlayerAdvancedMetrics metrics = player.getAdvancedMetrics();
        if (metrics == null) {
            return false;
        }

        double xGoalsPer90 = metrics.getExpectedGoalsPer90() != null ? metrics.getExpectedGoalsPer90() : 0.0;
        double xAssistsPer90 = metrics.getExpectedAssistsPer90() != null ? metrics.getExpectedAssistsPer90() : 0.0;
        return (xGoalsPer90 + xAssistsPer90) > TALISMAN_XGI_THRESHOLD;
    }
}