package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.service.projection.model.LeagueStandingsSnapshot;
import org.springframework.stereotype.Component;

@Component
public class MatchStakesEngine {

    private static final int EARLY_SEASON_CUTOFF = 25;
    private static final int SEASON_END_GW = 38;

    private static final double DELTA_RELEGATION = 0.06;
    private static final double DELTA_TITLE = 0.05;
    private static final double DELTA_EUROPE = 0.03;
    private static final double DELTA_BEACH = -0.07;

    public double calculateMotivationMultiplier(LeagueStandingsSnapshot standings, int gameweek) {
        if (standings == null || gameweek < EARLY_SEASON_CUTOFF) {
            return 1.0;
        }

        double tau = calculateSeasonProgressWeight(gameweek);
        double delta = resolveContextDelta(standings);

        double rawMultiplier = 1.0 + (delta * tau);

        // Clamping στο [0.90, 1.10]
        return Math.clamp(rawMultiplier, 0.90, 1.10); //  Math.max(0.90, Math.min(1.10, rawMultiplier))
    }

    private double calculateSeasonProgressWeight(int gameweek) {
        return (double) (gameweek - (EARLY_SEASON_CUTOFF - 1)) / (SEASON_END_GW - (EARLY_SEASON_CUTOFF - 1));
    }

    private double resolveContextDelta(LeagueStandingsSnapshot s) {
        //
        if (s.isInRelegationZone() || s.pointsFromRelegationZone() <= 3) {
            return DELTA_RELEGATION;
        }
        if (s.pointsFromTitle() >= -6) {
            return DELTA_TITLE;
        }
        if (s.pointsFromEuropeanSpots() >= -4) {
            return DELTA_EUROPE;
        }
        if (s.pointsFromRelegationZone() > 9 && s.pointsFromEuropeanSpots() < -9) {
            return DELTA_BEACH;
        }
        return 0.0;
    }
}
