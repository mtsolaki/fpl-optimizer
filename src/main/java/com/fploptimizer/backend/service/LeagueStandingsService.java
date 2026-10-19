package com.fploptimizer.backend.service;

import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.TeamRepository;
import com.fploptimizer.backend.service.projection.model.LeagueStandingsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service responsible for reconstructing empirical Premier League standings up to a target gameweek.
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

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    /**
     * Reconstructs the league table standings based on completed fixtures prior to the target gameweek.
     *
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

        Map<Integer, TeamStandingRow> standingsMap = new HashMap<>();
        for (Team team : allTeams) {
            standingsMap.put(team.getId(), new TeamStandingRow(team.getId()));
        }

        for (Fixture fixture : finishedFixtures) {
            if (fixture.getHomeTeamScore() != null && fixture.getAwayTeamScore() != null) {
                int homeId = fixture.getHomeTeam().getId();
                int awayId = fixture.getAwayTeam().getId();

                standingsMap.get(homeId).recordResult(fixture.getHomeTeamScore(), fixture.getAwayTeamScore());
                standingsMap.get(awayId).recordResult(fixture.getAwayTeamScore(), fixture.getHomeTeamScore());
            }
        }

        // 1. Sort standings using official tie-breakers
        List<TeamStandingRow> table = new ArrayList<>(standingsMap.values());
        Collections.sort(table);

        // 2. Establish competitive benchmarks
        int pointsTitle = table.isEmpty() ? 0 : table.get(0).getPoints();
        int pointsEurope = table.size() >= 6 ? table.get(5).getPoints() : 0;
        int pointsSafe17 = table.size() >= 17 ? table.get(16).getPoints() : 0;
        int pointsRel18 = table.size() >= 18 ? table.get(17).getPoints() : 0;

        // 3. Assemble immutable snapshot records
        Map<Integer, LeagueStandingsSnapshot> snapshots = new HashMap<>();

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
     * Internal accumulator row modeling team match records, goal tallies, and tie-breaking order.
     */
    private static class TeamStandingRow implements Comparable<TeamStandingRow> {
        private final int teamId;
        private int matchesPlayed;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;

        /**
         * Orders teams by points descending, then goal difference descending, then goals scored descending.
         *
         * @param other team standing row to compare against
         * @return comparison result
         */
        @Override
        public int compareTo(TeamStandingRow other) {
            int res = Integer.compare(other.getPoints(), getPoints());
            if (res != 0) return res;

            res = Integer.compare(other.goalDiff(), goalDiff());
            if (res != 0) return res;

            return Integer.compare(other.goalsFor, goalsFor);
        }

        public TeamStandingRow(int teamId) {
            this.teamId = teamId;
            this.matchesPlayed = 0;
            this.wins = 0;
            this.draws = 0;
            this.losses = 0;
            this.goalsFor = 0;
            this.goalsAgainst = 0;
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
            } else {
                losses++;
            }
        }

        private int getPoints() {
            return (3 * wins) + draws;
        }

        private int goalDiff() {
            return goalsFor - goalsAgainst;
        }
    }
}