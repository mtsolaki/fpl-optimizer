package com.fploptimizer.backend.service.projection.util;

import com.fploptimizer.backend.entity.Position;

/**
 * Utility helper encapsulating the official Fantasy Premier League (FPL) scoring and point allocation rules.
 *
 * Provides constants and pure evaluation functions for position-dependent scoring, appearance points,
 * defensive clean sheet rewards, and penalty point deductions.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
public final class FplRulesHelper {

    // Scoring and penalty point constants
    public static final double POINTS_PER_ASSIST = 3.0;
    public static final double POINTS_DEFCON_BONUS = 2.0;
    public static final double SAVES_DIVISOR = 3.0; // 1 point awarded per 3 goalkeeper saves
    public static final double PENALTY_MISS_COST = 2.0; // -2 points deduction for a missed penalty kick
    public static final double YELLOW_CARD_COST = 1.0;  // -1 point deduction per yellow card
    public static final double RED_CARD_COST = 3.0;     // -3 points deduction per red card

    /**
     * Private constructor to prevent direct instantiation of utility class.
     */
    private FplRulesHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Resolves the points awarded for scoring a goal according to player pitch position.
     *
     * Official FPL allocation:
     * - Forward (FWD): 4 points
     * - Midfielder (MID): 5 points
     * - Defender (DEF) / Goalkeeper (GKP): 6 points
     *
     * @param position the player's tactical position
     * @return points earned for scoring a single goal
     */
    public static int getPointsPerGoal(Position position) {
        return switch (position) {
            case FWD -> 4;
            case MID -> 5;
            case DEF, GKP -> 6;
            default -> 5;
        };
    }

    /**
     * Resolves the points awarded for securing a clean sheet according to player pitch position.
     *
     * Official FPL allocation (conditioned on completing 60+ minutes):
     * - Forward (FWD): 0 points
     * - Midfielder (MID): 1 point
     * - Defender (DEF) / Goalkeeper (GKP): 4 points
     *
     * @param position the player's tactical position
     * @return points earned for maintaining a clean sheet
     */
    public static int getPointsPerCleanSheet(Position position) {
        return switch (position) {
            case FWD -> 0;
            case MID -> 1;
            case DEF, GKP -> 4;
            default -> 0;
        };
    }

    /**
     * Determines whether a position is subject to points deduction for goals conceded.
     *
     * In FPL, only goalkeepers and defenders lose 1 point for every 2 goals conceded.
     *
     * @param position the player's tactical position
     * @return true if the position incurs goals conceded penalties, false otherwise
     */
    public static boolean hasGoalsConcededPenalty(Position position) {
        return position == Position.GKP || position == Position.DEF;
    }

    /**
     * Calculates baseline expected appearance points based on projected on-pitch playing time.
     *
     * Point thresholds:
     * - Projected minutes >= 60.0: 2.0 points
     * - Projected minutes in (0.0, 60.0): 1.0 point
     * - Projected minutes <= 0.0: 0.0 points
     *
     * @param projectedMinutes expected on-pitch playing time in minutes
     * @return expected appearance points
     */
    public static double getAppearancePoints(double projectedMinutes) {
        if (projectedMinutes >= 60.0) {
            return 2.0;
        }
        if (projectedMinutes > 0.0) {
            return 1.0;
        }
        return 0.0;
    }
}