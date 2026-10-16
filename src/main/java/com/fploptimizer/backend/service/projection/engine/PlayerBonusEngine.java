package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.service.projection.model.PlayerAttackResult;
import com.fploptimizer.backend.service.projection.model.PlayerCardsResult;
import com.fploptimizer.backend.service.projection.model.PlayerDefenceResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlayerBonusEngine {

    // Weights Event-Driven Model for FWD / MID
    private static final double WEIGHT_ATTACK_XG = 0.55;
    private static final double WEIGHT_ATTACK_XA = 0.35;
    private static final double WEIGHT_ATTACK_CS = 0.15; // Small BPS boost for MIDS Clean Sheet

    // Wights Event-Driven Model for DEF / GKP
    private static final double WEIGHT_DEF_CS = 0.50;
    private static final double WEIGHT_DEF_DEFCON = 0.15;
    private static final double WEIGHT_DEF_SAVES = 0.08;
    private static final double WEIGHT_DEF_XG = 0.45;
    private static final double WEIGHT_DEF_XA = 0.30;

    // Penalty in BPS
    private static final double WEIGHT_YELLOW_PENALTY = 0.10;

    // Constants for blending with logs
    private static final double MAX_RECENT_BONUS_WEIGHT = 0.25;
    private static final double SAMPLE_MINUTES_THRESHOLD = 270.0;
    private static final double MIN_MINUTES_TO_CONSIDER = 45.0;

    public double calculatePlayerBonus(
            Player player,
            double projectedMinutes,
            PlayerAttackResult attackResult,
            PlayerDefenceResult defenceResult,
            PlayerCardsResult cardsResult,
            List<PlayerGameweekLog> gameweekLogs
    ){
        if (player == null || projectedMinutes <= 0.0) {
            return 0.0;
        }

        // 1. null-safety check to collect the necessary data
        double xg = (attackResult != null) ? attackResult.expectedGoals() : 0.0;
        double xa = (attackResult != null) ? attackResult.expectedAssists() : 0.0;
        double csProb = (defenceResult != null) ? defenceResult.cleanSheetProbability() : 0.0;
        double defConProb = (defenceResult != null) ? defenceResult.projectedDefConProb() : 0.0;
        double saves = (defenceResult != null) ? defenceResult.projectedSaves() : 0.0;
        double yellowCards = (cardsResult != null) ? cardsResult.projectedYellowCards() : 0.0;

        // 2. Event Driven bonus based on position
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

        // 3. Recent Bonus History from logs
        double finalBonus = getFinalBonus(projectedMinutes, gameweekLogs, eventBonus);

        // No player can get negative or more than 3 points bonus
        return Math.clamp(finalBonus, 0.0, 3.0);

    }

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

        // 4. Blending Event Bonus with player trend
        double finalBonus;
        if (totalRecentMinutes < MIN_MINUTES_TO_CONSIDER) {
            finalBonus = eventBonus;
        } else {
            double recentBonusPer90 = (totalRecentBonus / totalRecentMinutes) * 90.0;
            double projectedRecentBonus = recentBonusPer90 * (projectedMinutes / 90.0);

            double weight = MAX_RECENT_BONUS_WEIGHT * Math.min(1.0, totalRecentMinutes / SAMPLE_MINUTES_THRESHOLD);
            finalBonus = (1.0 - weight) * eventBonus + weight * projectedRecentBonus;
        }
        return finalBonus;
    }
}
