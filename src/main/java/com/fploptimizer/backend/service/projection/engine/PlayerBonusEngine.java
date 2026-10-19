package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.service.projection.model.PlayerAttackResult;
import com.fploptimizer.backend.service.projection.model.PlayerCardsResult;
import com.fploptimizer.backend.service.projection.model.PlayerDefenceResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computational engine responsible for estimating expected FPL bonus points (0 to 3 points).
 *
 * Utilizes an event-driven heuristic calibrated against the official Bonus Points System (BPS):
 * 1. Derives positional baseline bonus expectancy from attacking contributions (xG, xA),
 *    defensive solidity (clean sheets, DefCon metrics, goalkeeper saves), and discipline penalties.
 * 2. Incorporates empirical bonus point acquisition rates from recent match logs.
 * 3. Restricts final output strictly to the official range [0.0, 3.0].
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
public class PlayerBonusEngine {

    // Event-driven coefficients for forwards and midfielders
    private static final double WEIGHT_ATTACK_XG = 0.55;
    private static final double WEIGHT_ATTACK_XA = 0.35;
    private static final double WEIGHT_ATTACK_CS = 0.15; // Clean sheet BPS bump for midfielders

    // Event-driven coefficients for defenders and goalkeepers
    private static final double WEIGHT_DEF_CS = 0.50;
    private static final double WEIGHT_DEF_DEFCON = 0.15;
    private static final double WEIGHT_DEF_SAVES = 0.08;
    private static final double WEIGHT_DEF_XG = 0.45;
    private static final double WEIGHT_DEF_XA = 0.30;

    // Disciplinary BPS penalty coefficient
    private static final double WEIGHT_YELLOW_PENALTY = 0.10;

    // Weighting parameters for blending recent match history
    private static final double MAX_RECENT_BONUS_WEIGHT = 0.25;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    /**
     * Projects expected bonus points for a player in an upcoming match.
     *
     * Evaluates positional event drivers (goals, assists, clean sheets, saves, cards)
     * and combines them with recent empirical bonus generation trends from historical logs.
     *
     * @param player           the target player entity
     * @param projectedMinutes projected playing time for the fixture
     * @param attackResult     projected attacking stats (xG, xA)
     * @param defenceResult    projected defensive metrics (clean sheet prob, defcon, saves)
     * @param cardsResult      projected disciplinary risk (yellow card frequency)
     * @param gameweekLogs     historical match logs for empirical bonus rate estimation
     * @return expected bonus points clamped between 0.0 and 3.0
     */
    public double calculatePlayerBonus(
            Player player,
            double projectedMinutes,
            PlayerAttackResult attackResult,
            PlayerDefenceResult defenceResult,
            PlayerCardsResult cardsResult,
            List<PlayerGameweekLog> gameweekLogs
    ) {
        if (player == null || projectedMinutes <= 0.0) {
            return 0.0;
        }

        // 1. Safe extraction of input projections
        double xg = (attackResult != null) ? attackResult.expectedGoals() : 0.0;
        double xa = (attackResult != null) ? attackResult.expectedAssists() : 0.0;
        double csProb = (defenceResult != null) ? defenceResult.cleanSheetProbability() : 0.0;
        double defConProb = (defenceResult != null) ? defenceResult.projectedDefConProb() : 0.0;
        double saves = (defenceResult != null) ? defenceResult.projectedSaves() : 0.0;
        double yellowCards = (cardsResult != null) ? cardsResult.projectedYellowCards() : 0.0;

        // 2. Position-specific event-driven bonus evaluation
        double eventBonus = 0.0;
        Position pos = player.getPosition();

        if (pos != null) {
            switch (pos) {
                case FWD, MID -> {
                    eventBonus = (WEIGHT_ATTACK_XG * xg)
                            + (WEIGHT_ATTACK_XA * xa)
                            + (pos == Position.MID ? WEIGHT_ATTACK_CS * csProb : 0.0)
                            - (WEIGHT_YELLOW_PENALTY * yellowCards);
                }
                case DEF, GKP -> {
                    eventBonus = (WEIGHT_DEF_CS * csProb)
                            + (WEIGHT_DEF_DEFCON * defConProb)
                            + (pos == Position.GKP ? WEIGHT_DEF_SAVES * saves : 0.0)
                            + (WEIGHT_DEF_XG * xg)
                            + (WEIGHT_DEF_XA * xa)
                            - (WEIGHT_YELLOW_PENALTY * yellowCards);
                }
            }
        }

        eventBonus = Math.max(0.0, eventBonus);

        // 3. Blend with empirical bonus points from recent gameweek logs
        double finalBonus = getFinalBonus(projectedMinutes, gameweekLogs, eventBonus);

        // Official FPL rules cap bonus points between 0 and 3
        return Math.clamp(finalBonus, 0.0, 3.0);
    }

    /**
     * Blends the event-driven bonus projection with the player's recent historical bonus trend.
     *
     * Computes the recent bonus rate per 90 minutes across historical logs and applies a dynamic
     * sample-size weight up to MAX_RECENT_BONUS_WEIGHT.
     *
     * @param projectedMinutes projected playing time in minutes
     * @param gameweekLogs     historical match logs for the player
     * @param eventBonus       theoretical event-driven baseline bonus
     * @return blended bonus expectation
     */
    private static double getFinalBonus(double projectedMinutes, List<PlayerGameweekLog> gameweekLogs, double eventBonus) {
        double totalRecentMinutes = 0.0;
        double totalRecentBonus = 0.0;

        if (gameweekLogs != null) {
            for (PlayerGameweekLog log : gameweekLogs) {
                if (log == null || log.getOutcome() == null) continue;

                Integer mins = log.getOutcome().getActualMinutes();
                if (mins != null && mins > 0) {
                    totalRecentMinutes += mins;

                    if (log.getOutcome().getActualBonus() != null) {
                        totalRecentBonus += log.getOutcome().getActualBonus();
                    }
                }
            }
        }

        // Return baseline event bonus if sample size is insufficient
        if (totalRecentMinutes < MIN_MINUTES_TO_CONSIDER) {
            return eventBonus;
        }

        double recentBonusPer90 = (totalRecentBonus / totalRecentMinutes) * 90.0;
        double projectedRecentBonus = recentBonusPer90 * (projectedMinutes / 90.0);

        double weight = MAX_RECENT_BONUS_WEIGHT * Math.min(1.0, totalRecentMinutes / SAMPLE_MINUTES_THRESHOLD);
        return (1.0 - weight) * eventBonus + weight * projectedRecentBonus;
    }
}