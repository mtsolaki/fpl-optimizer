package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.service.projection.model.LeagueStandingsSnapshot;
import org.springframework.stereotype.Component;
/**
 * Computational engine evaluating team motivation multipliers based on league table context and match stakes.
 *
 * Quantifies psychological and competitive urgency as the season approaches its conclusion.
 * Teams fighting relegation, challenging for the title, or chasing European qualification receive an
 * intensity boost, whereas safe mid-table teams with nothing left to play for ("on the beach") experience
 * a motivational decay penalty.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
public class MatchStakesEngine {

    // Threshold gameweek before which table context effects are considered negligible
    private static final int EARLY_SEASON_CUTOFF = 25;
    // Final gameweek of the Premier League season
    private static final int SEASON_END_GW = 38;

    // Motivation adjustments (deltas) by competitive scenario
    private static final double DELTA_RELEGATION = 0.06;
    private static final double DELTA_TITLE = 0.05;
    private static final double DELTA_EUROPE = 0.03;
    private static final double DELTA_BEACH = -0.07;

    /**
     * Calculates the overall motivation multiplier for a team given its current standings and target gamewee.
     *
     * In the early and middle parts of the season (prior to gameweek 25), stakes are assumed neutral (1.0).
     * In the closing stretch (GW25 to GW38), the contextual delta is scaled progressively by the
     * season progress weight factor (tau), producing a multiplier clamped within [0.90, 1.10].
     *
     * @param standings league table metrics snapshot for the team
     * @param gameweek  the active gameweek round number
     * @return the motivation multiplier clamped between 0.90 and 1.10
     */
    public double calculateMotivationMultiplier(LeagueStandingsSnapshot standings, int gameweek) {
        if (standings == null || gameweek < EARLY_SEASON_CUTOFF) {
            return 1.0;
        }

        double tau = calculateSeasonProgressWeight(gameweek);
        double delta = resolveContextDelta(standings);

        double rawMultiplier = 1.0 + (delta * tau);

        // Clamping in [0.90, 1.10]
        return Math.clamp(rawMultiplier, 0.90, 1.10); //  Math.max(0.90, Math.min(1.10, rawMultiplier))
    }

    /**
     * Determines the linear season progression scaling factor (tau) in the range [0.0, 1.0].
     *
     * Scales urgency linearly from gameweek 25 (tau approx. 0.08) through the final round GW38 (tau = 1.0).
     *
     * @param gameweek the target gameweek
     * @return the progress weight factor tau
     */

    private double calculateSeasonProgressWeight(int gameweek) {
        return (double) (gameweek - (EARLY_SEASON_CUTOFF - 1)) / (SEASON_END_GW - (EARLY_SEASON_CUTOFF - 1));
    }

    /**
     * Resolves the competitive context delta according to the team's standing position and point margins.
     *
     * Evaluates criteria in priority sequence:
     * 1. Relegation struggle: Team is in the drop zone or within 3 points of safety (+0.06).
     * 2. Title contention: Team is within 6 points of the league leaders (+0.05).
     * 3. European contention: Team is within 4 points of European qualification spots (+0.03).
     * 4. Safe mid-table: Team is comfortably clear of relegation (> 9 points) and far from Europe (< -9 points) (-0.07).
     *
     * @param s the league standings snapshot
     * @return the contextual motivation delta
     */
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
