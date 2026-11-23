package com.fploptimizer.backend.service.projection.util;

import com.fploptimizer.backend.entity.Position;

/**
 * Utility helper encapsulating the official Fantasy Premier League (FPL) scoring and point allocation rules.
 * <p>
 * Provides constants and pure evaluation functions for position-dependent scoring,
 * defensive clean sheet rewards, and penalty point deductions.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
public final class FplRulesHelper {

    // Scoring and penalty point constants
    public static final double POINTS_PER_ASSIST = 3.0;
    public static final double POINTS_DEFCON_BONUS = 2.0;
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
     * <p>
     * Official FPL allocation:
     * - Forward (FWD): 4 points
     * - Midfielder (MID): 5 points
     * - Defender (DEF) / Goalkeeper (GKP): 6 points
     *
     * @param position the player's tactical position
     * @return points earned for scoring a single goal
     */
    public static int getPointsPerGoal(Position position) {
        if (position == null) {
            return 4;
        }
        return switch (position) {
            case FWD -> 4;
            case MID -> 5;
            case DEF, GKP -> 6;
        };
    }

    /**
     * Resolves the points awarded for securing a clean sheet according to player pitch position.
     * <p>
     * Official FPL allocation (conditioned on completing 60+ minutes):
     * - Forward (FWD): 0 points
     * - Midfielder (MID): 1 point
     * - Defender (DEF) / Goalkeeper (GKP): 4 points
     *
     * @param position the player's tactical position
     * @return points earned for maintaining a clean sheet
     */
    public static int getPointsPerCleanSheet(Position position) {
        if (position == null) {
            return 0;
        }
        return switch (position) {
            case FWD -> 0;
            case MID -> 1;
            case DEF, GKP -> 4;
        };
    }

    /**
     * Determines whether a position is subject to points deduction for goals conceded.
     * <p>
     * In FPL, only goalkeepers and defenders lose 1 point for every 2 goals conceded.
     *
     * @param position the player's tactical position
     * @return true if the position incurs goals conceded penalties, false otherwise
     */
    public static boolean hasGoalsConcededPenalty(Position position) {
        return position == Position.GKP || position == Position.DEF;
    }
}