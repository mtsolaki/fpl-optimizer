package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.service.projection.model.PlayerMinutesResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Computational engine responsible for projecting expected player minutes and appearance probabilities.
 *
 * Deconstructs on-pitch time into starting probability and substitute appearance probability,
 * factors in health and suspension availability flags, and employs Bayesian smoothing to estimate
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

    // Positional priors for completing 60+ minutes given that the player starts
    private static final double PRIOR_60_MIN_GKP = 0.98;
    private static final double PRIOR_60_MIN_DEF = 0.92;
    private static final double PRIOR_60_MIN_MID = 0.82;
    private static final double PRIOR_60_MIN_FWD = 0.78;

    // Bayesian shrinkage pseudo-starts weight to smooth small sample sizes
    private static final double PSEUDO_STARTS_WEIGHT = 3.0;

    /**
     * Projects on-pitch minutes, starting probability, and 60+ minutes probability.
     *
     * Evaluates official availability news, calculates rotation risk from recent logs,
     * blends starter and substitute expected playing time, and clamps results within [0.0, 90.0].
     *
     * @param player       the target player entity
     * @param gameweekLogs historical match logs reflecting recent manager selection trends
     * @return a PlayerMinutesResult containing projected minutes, start prob, and 60+ min prob
     */
    public PlayerMinutesResult calculateMinutes(Player player, List<PlayerGameweekLog> gameweekLogs) {
        if (player == null) {
            return new PlayerMinutesResult(0.0, 0.0, 0.0);
        }

        double availability = resolveAvailability(player);
        if (availability == 0.0) {
            return new PlayerMinutesResult(0.0, 0.0, 0.0);
        }

        double startProbability = calculateStartProbability(player, gameweekLogs);
        double finalStartProbability = Math.clamp(startProbability * availability, 0.0, 1.0);

        double plus60Probability = calculateSixtyPlusGivenStart(player, gameweekLogs);
        double finalPlus60Probability = Math.clamp(finalStartProbability * plus60Probability, 0.0, 1.0);

        boolean isGoalkeeper = (player.getPosition() == Position.GKP);
        double subAppearanceProbability = isGoalkeeper ? 0.01 : 0.35;

        double starterMinutes = finalStartProbability * DEFAULT_STARTER_MINUTES;
        double benchMinutes = (1.0 - finalStartProbability) * subAppearanceProbability * DEFAULT_SUB_MINUTES;

        double rawProjectedMinutes = starterMinutes + benchMinutes;
        double projectedMinutes = Math.clamp(rawProjectedMinutes * availability, 0.0, 90.0);

        return new PlayerMinutesResult(
                projectedMinutes,
                finalStartProbability,
                finalPlus60Probability
        );
    }

    /**
     * Estimates the probability of a player playing 60+ minutes conditional on starting the match.
     *
     * Utilizes a Bayesian smoothing formula against positional priors to prevent volatility
     * from small sample sizes in the early season.
     *
     * @param player       the player entity
     * @param gameweekLogs historical match logs
     * @return smoothed probability in range [0.0, 1.0]
     */
    private double calculateSixtyPlusGivenStart(Player player, List<PlayerGameweekLog> gameweekLogs) {
        if (player == null) {
            return 0.0;
        }

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

        // Bayesian Shrinkage Formula: (Successes + K * Prior) / (Trials + K)
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
     *
     * Returns the decimal probability [0.0, 1.0] representing the chance of playing this round.
     *
     * @param player the target player
     * @return availability probability in range [0.0, 1.0]
     */
    private double resolveAvailability(Player player) {
        if (player == null || player.getStatus() == null) {
            return 0.0;
        }

        String status = player.getStatus();

        // Status codes: 'a' (available), 'd' (doubtful), 'u' (unavailable/unsettled)
        if (status.equals("a") || status.equals("d") || status.equals("u")) {
            if (player.getMarketInfo() != null && player.getMarketInfo().getChanceOfPlayingThisRound() != null) {
                return player.getMarketInfo().getChanceOfPlayingThisRound() / 100.0;
            }
            return 1.0;
        }

        return 0.0;
    }

    /**
     * Calculates starting probability by weighting recent lineup patterns against season averages.
     *
     * Key "talisman" players are heavily anchored to their established season status (65% weight),
     * whereas standard rotation-risk players are more heavily weighted by recent 3-game trends (65% weight).
     *
     * @param player       the target player
     * @param gameweekLogs historical match logs
     * @return starting probability clamped between 0.0 and 1.0
     */
    private double calculateStartProbability(Player player, List<PlayerGameweekLog> gameweekLogs) {
        if (gameweekLogs == null || gameweekLogs.isEmpty()) {
            return calculateColdStartProbability(player);
        }

        List<PlayerGameweekLog> recentLogs = gameweekLogs.stream()
                .filter(log -> log != null && log.getFixture() != null && log.getFixture().getGameweek() != null)
                .sorted(Comparator.comparing(
                        (PlayerGameweekLog log) -> log.getFixture().getGameweek()
                ).reversed())
                .limit(3)
                .toList();

        if (recentLogs.isEmpty()) {
            return calculateColdStartProbability(player);
        }

        double totalMinutes = 0;
        for (PlayerGameweekLog log : recentLogs) {
            if (log.getOutcome() != null && log.getOutcome().getActualMinutes() != null) {
                totalMinutes += log.getOutcome().getActualMinutes();
            }
        }
        double recentRatio = Math.clamp(totalMinutes / (recentLogs.size() * 90.0), 0.0, 1.0);

        int matchesStarted = 0;
        for (PlayerGameweekLog log : gameweekLogs) {
            if (log != null && log.getOutcome() != null && Boolean.TRUE.equals(log.getOutcome().getWasStarter())) {
                matchesStarted++;
            }
        }
        double seasonRatio = (double) matchesStarted / gameweekLogs.size();

        // Talismans rely more heavily on overall season security
        double recentWeight = isTalisman(player) ? 0.35 : 0.65;
        double seasonWeight = 1.0 - recentWeight;

        return Math.clamp((recentWeight * recentRatio) + (seasonWeight * seasonRatio), 0.0, 1.0);
    }

    /**
     * Heuristic cold-start starting probability estimation used when no gameweek logs exist.
     *
     * Uses market price tiers and ownership percentages as proxies for squad hierarchy.
     *
     * @param player the target player
     * @return initial estimated starting probability
     */
    private double calculateColdStartProbability(Player player) {
        if (player == null || player.getPerformance() == null) {
            return 0.0;
        }

        if (player.getPerformance().getStarts() != null && player.getPerformance().getStarts() > 0) {
            return player.getPerformance().getStarts() / 38.0;
        }

        if (player.getMarketInfo() == null || player.getPosition() == null) {
            return 0.0;
        }

        double probability;
        double ownership = player.getMarketInfo().getSelectedByPercent();
        double currentPrice = player.getMarketInfo().getCurrentPrice();

        if (player.getPosition() == Position.GKP) {
            if (currentPrice >= 4.5 && ownership >= 2.0) {
                probability = 0.95;
            } else if (ownership > 10.00) {
                probability = 0.75;
            } else {
                probability = 0.05;
            }
        } else if (player.getPosition() == Position.DEF) {
            if (currentPrice >= 5.0 && ownership >= 10.0) {
                probability = 0.90;
            } else if (currentPrice >= 4.5) {
                probability = (ownership >= 15.0) ? 0.85 : 0.70;
            } else {
                if (ownership >= 20.0) {
                    probability = 0.75;
                } else if (ownership >= 8.0) {
                    probability = 0.45;
                } else {
                    probability = 0.15;
                }
            }
        } else if (player.getPosition() == Position.MID || player.getPosition() == Position.FWD) {
            if (currentPrice >= 7.5) {
                probability = 0.90;
            } else if (currentPrice >= 5.5) {
                probability = (ownership >= 15.0) ? 0.88 : 0.78;
            } else {
                if (ownership >= 15.0) {
                    probability = 0.65;
                } else if (ownership >= 5.0) {
                    probability = 0.40;
                } else {
                    probability = 0.15;
                }
            }
        } else {
            probability = 0.5;
        }
        return probability;
    }

    /**
     * Determines whether a player qualifies as a primary team talisman.
     *
     * A player is designated as a talisman if their attacking rate (xG90 + xA90) exceeds 0.50
     * or if they are the designated first-choice penalty taker.
     *
     * @param player the player entity being assessed
     * @return true if the player is identified as a talisman, false otherwise
     */
    private boolean isTalisman(Player player) {
        if (player == null) return false;

        double xGoalsPer90 = 0.0;
        double xAssistsPer90 = 0.0;

        if (player.getAdvancedMetrics() != null) {
            if (player.getAdvancedMetrics().getExpectedGoalsPer90() != null) {
                xGoalsPer90 = player.getAdvancedMetrics().getExpectedGoalsPer90();
            }
            if (player.getAdvancedMetrics().getExpectedAssistsPer90() != null) {
                xAssistsPer90 = player.getAdvancedMetrics().getExpectedAssistsPer90();
            }
        }

        if (player.getPenaltiesOrder() != null) {
            return (xGoalsPer90 + xAssistsPer90 > 0.5) || (player.getPenaltiesOrder() == 1);
        }
        return (xGoalsPer90 + xAssistsPer90 > 0.5);
    }
}