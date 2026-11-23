package com.fploptimizer.backend.service;

import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.TeamRepository;
import com.fploptimizer.backend.service.projection.model.LeagueStandingsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for reconstructing empirical Premier League standings up to a target gameweek.
 * <p>
 * Simulates the league table strictly using matches concluded before the requested round.
 * Computes official tie-breakers (points, goal difference, goals scored) and calculates key
 * competitive benchmarks (distance to title, European qualification, and relegation safety margins)
 * required by the match stakes engine.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class LeagueStandingsService {

    // Official Premier League tie-breakers: points descending, goal difference descending, goals scored descending
    private static final Comparator<TeamStandingRow> STANDINGS_COMPARATOR = Comparator
            .comparingInt(TeamStandingRow::getPoints)
            .thenComparingInt(TeamStandingRow::goalDiff)
            .thenComparingInt(TeamStandingRow::getGoalsFor)
            .reversed();

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    /**
     * Reconstructs the league table standings based on completed fixtures prior to the target gameweek.
     * <p>
     * Tabulates match records, sorts clubs according to Premier League tie-breaking criteria,
     * establishes target benchmarks for title, European qualification, and relegation boundaries,
     * and generates an immutable snapshot map indexed by Team ID.
     *
     * @param gameweek the gameweek threshold (only matches from earlier gameweeks are processed)
     * @return map linking each team ID to its contextual LeagueStandingsSnapshot
     */
    @Transactional(readOnly = true)
    public Map<Integer, LeagueStandingsSnapshot> calculateStandingsUpToGameweek(int gameweek) {
        List<Team> allTeams = teamRepository.findAll();
        List<Fixture> finishedFixtures = fixtureRepository.findByFinishedTrueAndGameweekLessThan(gameweek);

        Map<Integer, TeamStandingRow> standingsMap = HashMap.newHashMap(allTeams.size());
        for (Team team : allTeams) {
            standingsMap.put(team.getId(), new TeamStandingRow(team.getId()));
        }

        for (Fixture fixture : finishedFixtures) {
            applyFixture(standingsMap, fixture);
        }

        // 1. Sort standings using official tie-breakers
        List<TeamStandingRow> table = new ArrayList<>(standingsMap.values());
        table.sort(STANDINGS_COMPARATOR);

        // 2. Establish competitive benchmarks
        int pointsTitle = getPointsAtPosition(table, 1);
        int pointsEurope = getPointsAtPosition(table, 6);
        int pointsSafe17 = getPointsAtPosition(table, 17);
        int pointsRel18 = getPointsAtPosition(table, 18);

        // 3. Assemble immutable snapshot records
        Map<Integer, LeagueStandingsSnapshot> snapshots = HashMap.newHashMap(table.size());

        for (int i = 0; i < table.size(); i++) {
            TeamStandingRow row = table.get(i);
            int position = i + 1;

            // Relegation margin: teams 1-17 compare against 18th place; teams 18-20 compare against 17th place
            int relegationBenchmark = (position <= 17) ? pointsRel18 : pointsSafe17;

            LeagueStandingsSnapshot snapshot = row.toSnapshot(
                    position,
                    pointsTitle,
                    pointsEurope,
                    relegationBenchmark
            );

            snapshots.put(row.teamId, snapshot);
        }

        return snapshots;
    }

    /**
     * Applies a completed fixture's scoreline to the home and away standing rows if valid.
     */
    private void applyFixture(Map<Integer, TeamStandingRow> standingsMap, Fixture fixture) {
        if (fixture.getHomeTeamScore() == null
                || fixture.getAwayTeamScore() == null
                || fixture.getHomeTeam() == null
                || fixture.getAwayTeam() == null) {
            return;
        }

        TeamStandingRow homeRow = standingsMap.get(fixture.getHomeTeam().getId());
        TeamStandingRow awayRow = standingsMap.get(fixture.getAwayTeam().getId());

        if (homeRow != null && awayRow != null) {
            homeRow.recordResult(fixture.getHomeTeamScore(), fixture.getAwayTeamScore());
            awayRow.recordResult(fixture.getAwayTeamScore(), fixture.getHomeTeamScore());
        }
    }

    /**
     * Safely retrieves the points total at a 1-based table position, defaulting to 0 if unavailable.
     */
    private int getPointsAtPosition(List<TeamStandingRow> table, int position) {
        return table.size() >= position ? table.get(position - 1).getPoints() : 0;
    }

    /**
     * Internal accumulator row modeling team match records and goal tallies.
     */
    private static class TeamStandingRow {
        private final int teamId;
        private int matchesPlayed;
        private int wins;
        private int draws;
        private int goalsFor;
        private int goalsAgainst;

        public TeamStandingRow(int teamId) {
            this.teamId = teamId;
        }

        private LeagueStandingsSnapshot toSnapshot(
                int position, int pointsTitle, int pointsEurope, int pointsRelegation) {
            return new LeagueStandingsSnapshot(
                    teamId,
                    this.getPoints(),
                    position,
                    this.matchesPlayed,
                    this.goalsFor,
                    this.goalsAgainst,
                    getPoints() - pointsTitle,
                    getPoints() - pointsEurope,
                    getPoints() - pointsRelegation
            );
        }

        private void recordResult(int scored, int conceded) {
            matchesPlayed++;
            goalsFor += scored;
            goalsAgainst += conceded;

            if (scored > conceded) {
                wins++;
            } else if (scored == conceded) {
                draws++;
            }
        }

        private int getPoints() {
            return (3 * wins) + draws;
        }

        private int goalDiff() {
            return goalsFor - goalsAgainst;
        }

        private int getGoalsFor() {
            return goalsFor;
        }
    }
}