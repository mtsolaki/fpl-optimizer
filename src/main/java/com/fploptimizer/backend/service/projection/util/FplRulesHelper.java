package com.fploptimizer.backend.service.projection.util;

import com.fploptimizer.backend.entity.Position;

public final class FplRulesHelper {

    //points final
    public static final double POINTS_PER_ASSIST = 3.0;
    public static final double POINTS_DEFCON_BONUS = 2.0;
    public static final double SAVES_DIVISOR = 3.0; // 1 pt ανά 3 saves
    public static final double PENALTY_MISS_COST = 2.0; // -2 pts
    public static final double YELLOW_CARD_COST = 1.0;  // -1 pt
    public static final double RED_CARD_COST = 3.0;


    private FplRulesHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /*
        Goal points based on Position
        FWD : 4, MID : 5, DEF/GKP: 6
     */
    public static int getPointsPerGoal(Position position) {
        return switch (position)
        {
            case FWD -> 4;
            case MID -> 5;
            case DEF,GKP -> 6;
            default -> 5;
        };

    }

    /*
        Points for clean sheet based on Position
        FWD: 0, MID: 1, DEF/GPK: 4
     */
    public static int getPointsPerCleanSheet(Position position) {
        return switch (position){
            case FWD -> 0;
            case MID -> 1;
            case DEF,GKP -> 4;
            default -> 0;
        };
    }

    /*
        Checks if player is GKP or DEF for goals conceded penalty
     */
    public static boolean hasGoalsConcededPenalty(Position position) {
        return position == Position.GKP || position == Position.DEF;
    }

    /*
        Calculates expected Appearance Points
        If xMin >= 60 => 2.0 points, if 0 < xMin < 60 => 1.0 point, else 0
     */
    public static double getAppearancePoints(double projectedMinutes){
        if (projectedMinutes >= 60.0) {
            return 2.0;
        }
        if (projectedMinutes > 0.0) {
            return 1.0;
        }
        return 0.0;
    }
}
