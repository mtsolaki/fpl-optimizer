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
/*
    Used to Create standings table in HashMap of LeagueStandingsSnapshot
 */
@Service
@RequiredArgsConstructor

public class LeagueStandingsService {

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

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

        // 1. HashMap to List and sorting using compareTo
        List<TeamStandingRow> table = new ArrayList<>(standingsMap.values());
        Collections.sort(table);

        // 2. Benchmarks Calculation
        int pointsTitle = table.isEmpty() ? 0 : table.get(0).getPoints();
        int pointsEurope = table.size() >= 6 ? table.get(5).getPoints() : 0;
        int pointsSafe17 = table.size() >= 17 ? table.get(16).getPoints() : 0;
        int pointsRel18 = table.size() >= 18 ? table.get(17).getPoints() : 0;

        // 3. Snapshots creation
        Map<Integer, LeagueStandingsSnapshot> snapshots = new HashMap<>();

        for (int i = 0; i < table.size(); i++) {
            TeamStandingRow row = table.get(i);
            int position = i + 1;

            // For 1 - 17 positions benchmark calculated from 18, for 18-20 from 17
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
    /*
      Helper class for data parsing
     */
    private static class TeamStandingRow implements Comparable<TeamStandingRow>{
        private final int teamId;
        private int matchesPlayed;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;

        @Override
        /*
        Note: this class has a natural ordering that is inconsistent with equals.
         */
        public int compareTo(TeamStandingRow other) {

            int res = Integer.compare(other.getPoints(), getPoints());
            if (res != 0) return res;

            res = Integer.compare(other.goalDiff(), goalDiff());
            if (res != 0) return res;

            res = Integer.compare(other.goalsFor, goalsFor);

            return res;
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
                    getPoints() -pointsEurope,
                    getPoints() - pointsRelegation);


        }


        private void recordResult(int scored, int conceded){
            matchesPlayed++;
            goalsFor = goalsFor + scored;
            goalsAgainst = goalsAgainst + conceded;

            if (scored > conceded){
                wins++;
            } else if (scored == conceded){
                draws++;
            } else {
                losses++;
            }

        }

        private int getPoints(){
            int points = 0;
            points = (3*wins)+draws;
            return points;
        }

        private int goalDiff(){
            int goalDiff = 0;
            goalDiff = goalsFor - goalsAgainst;
            return goalDiff;
        }
    }
}
