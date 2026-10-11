package com.fploptimizer.backend.service.projection.model;

public record LeagueStandingsSnapshot(
        int teamId,
        Integer points,
        Integer position,
        Integer matchesPlayed,
        int pointsFromTitle,
        int pointsFromEuropeanSpots,
        int pointsFromRelegationZone
        ) {

    // Usual Premier League Standards
    private static final int TITLE_POSITION = 1;
    private static final int EUROPEAN_CUTOFF = 6;      // Top 6  Champions/Europa League
    private static final int RELEGATION_CUTOFF = 18;   // 18, 19, 20 υποβιβάζονται

    public int distanceFromTitle() {
        if (position == null) return 0;
        return position - TITLE_POSITION;
    }

    public int distanceFromEuropeanSpots() {
        if (position == null) return 0;
        return position - EUROPEAN_CUTOFF;
    }

    public int distanceFromRelegationZone() {
        if (position == null) return 0;
        return RELEGATION_CUTOFF - position;
    }

    public boolean isInRelegationZone() {
        return position >= RELEGATION_CUTOFF;
    }

    public boolean isInEuropeanSpots() {
        return position <= EUROPEAN_CUTOFF;
    }
}
