package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.service.projection.model.LeagueStandingsSnapshot;
import com.fploptimizer.backend.service.projection.model.LineupDegradation;
import com.fploptimizer.backend.service.projection.model.MatchContext;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FixtureStrengthEngine {

    // Historical home and away averages
    private static final double LEAGUE_AVG_HOME_GOALS = 1.55;
    private static final double LEAGUE_AVG_AWAY_GOALS = 1.25;
    private static final double LEAGUE_AVG_OVERALL = 1.40;

    private static final double HOME_ADVANTAGE_ATTACK = 1.10; // +10% for home team attack
    private static final double AWAY_DISADVANTAGE_DEFENSE = 1.05; //

    private static final double MIN_LAMBDA = 0.20;
    private static final double MAX_LAMBDA = 4.50;

    private static final double MIN_BASE_RATING = 0.65;
    private static final double MAX_BASE_RATING = 1.60;
    private static final int SHRINKAGE_PRIOR_WEIGHT = 10;

    private LineupDegradationEngine lineupDegradationEngine;
    private MatchStakesEngine matchStakesEngine;

    public MatchContext buildMatchContext(
            int fixtureId,
            int gameweek,
            Team homeTeam,
            Team awayTeam,
            List<Player> homePlayers,
            List<Player> awayPlayers,
            LeagueStandingsSnapshot homeStandings,
            LeagueStandingsSnapshot awayStandings
    ) {
        // 1. Lineup Degradation
        LineupDegradation homeDegradation = lineupDegradationEngine.calculateDegradation(homePlayers);
        LineupDegradation awayDegradation = lineupDegradationEngine.calculateDegradation(awayPlayers);

        // 2. Match Stakes
        double homeMotivation = matchStakesEngine.calculateMotivationMultiplier(homeStandings, gameweek);
        double awayMotivation = matchStakesEngine.calculateMotivationMultiplier(awayStandings, gameweek);

        // 3. Base Ratings (Bayesian Blend: Prior FDR + Empirical Form)
        double homeBaseAtt = calculateBaseAttack(homeTeam, homeStandings, true);
        double homeBaseDef = calculateBaseDefense(homeTeam, homeStandings, true);

        double awayBaseAtt = calculateBaseAttack(awayTeam, awayStandings, false);
        double awayBaseDef = calculateBaseDefense(awayTeam, awayStandings, false);

        // 4. Final Attack & Defense Strengths
        double homeFinalAtt = homeBaseAtt * homeDegradation.attackFactor() * homeMotivation * HOME_ADVANTAGE_ATTACK;
        double homeFinalDef = homeBaseDef * homeDegradation.defenceFactor();

        double awayFinalAtt = awayBaseAtt * awayDegradation.attackFactor() * awayMotivation;
        double awayFinalDef = awayBaseDef * awayDegradation.defenceFactor() * AWAY_DISADVANTAGE_DEFENSE;

        // 5. Poisson Lambdas
        double rawHomeLambda = LEAGUE_AVG_HOME_GOALS * homeFinalAtt * awayFinalDef;
        double rawAwayLambda = LEAGUE_AVG_AWAY_GOALS * awayFinalAtt * homeFinalDef;

        double homeLambda = Math.clamp(rawHomeLambda, MIN_LAMBDA, MAX_LAMBDA);
        double awayLambda = Math.clamp(rawAwayLambda, MIN_LAMBDA, MAX_LAMBDA);

        // 6. Clean Sheet Probabilities (P(X=0) = e^(-lambda))
        double homeCsProb = Math.exp(-awayLambda);
        double awayCsProb = Math.exp(-homeLambda);

        // 7. Κατασκευή TeamStrengthProjection records
        TeamStrengthProjection homeProj = new TeamStrengthProjection(
                homeTeam.getId(),
                homeBaseAtt,
                homeBaseDef,
                homeDegradation.attackFactor(),
                homeDegradation.defenceFactor(),
                homeMotivation,
                homeFinalAtt,
                homeFinalDef,
                homeLambda,
                awayLambda, // Expected Goals Conceded
                homeCsProb
        );

        TeamStrengthProjection awayProj = new TeamStrengthProjection(
                awayTeam.getId(),
                awayBaseAtt,
                awayBaseDef,
                awayDegradation.attackFactor(),
                awayDegradation.defenceFactor(),
                awayMotivation,
                awayFinalAtt,
                awayFinalDef,
                awayLambda,
                homeLambda, // Expected Goals Conceded
                awayCsProb
        );

        // 8. MatchContext
        return new MatchContext(
                fixtureId,
                homeProj,
                awayProj,
                homeFinalAtt,
                awayFinalAtt
        );
    }

    private double calculateBaseAttack(Team team, LeagueStandingsSnapshot standings, boolean isHome) {
        int fdr = resolveTeamStrength(team, isHome);

        double priorAtt = 1.0 + (fdr - 3.0) * 0.08;

        if (standings == null || standings.matchesPlayed() == null || standings.matchesPlayed() == 0) {
            return priorAtt;
        }

        int played = standings.matchesPlayed();
        int goalsScored = standings.goalsFor();

        // Weight so we won't overvalue first matches
        double weight = (double) played / (played + SHRINKAGE_PRIOR_WEIGHT);


        double goalsPerMatch = (double) goalsScored / played;
        double empiricalAtt = goalsPerMatch / LEAGUE_AVG_OVERALL;

        double blendedAtt = (weight * empiricalAtt) + ((1.0 - weight) * priorAtt);
        return Math.clamp(blendedAtt, MIN_BASE_RATING, MAX_BASE_RATING);
    }

    private double calculateBaseDefense(Team team, LeagueStandingsSnapshot standings, boolean isHome) {
        int fdr = resolveTeamStrength(team, isHome);

        // Μεγαλύτερο FDR σημαίνει καλύτερη ομάδα, άρα μικρότερη ευπάθεια άμυνας (beta < 1.0)
        double priorDef = 1.0 - (fdr - 3.0) * 0.08;

        if (standings == null || standings.matchesPlayed() == null || standings.matchesPlayed() == 0) {
            return priorDef;
        }

        int played = standings.matchesPlayed();
        int goalsConceded = standings.goalsAgainst();


        double concededPerMatch = (double) goalsConceded / played;
        double empiricalDef = concededPerMatch / LEAGUE_AVG_OVERALL;

        // N / (N + K)
        double weight = (double) played / (played + SHRINKAGE_PRIOR_WEIGHT);

        double blendedDef = (weight * empiricalDef) + ((1.0 - weight) * priorDef);

        // Clamping in [0.65, 1.60]
        return Math.clamp(blendedDef, MIN_BASE_RATING, MAX_BASE_RATING);
    }


    private int resolveTeamStrength(Team team, boolean isHome) {
        if (team == null) {
            return 3;
        }

        if (isHome) {
            if (team.getStrengthOverallHome() != null) {
                return team.getStrengthOverallHome();
            } else {
                return 3;
            }
        } else {
            if (team.getStrengthOverallAway() != null) {
                return team.getStrengthOverallAway();
            } else {
                return 3;
            }
        }
    }

}
