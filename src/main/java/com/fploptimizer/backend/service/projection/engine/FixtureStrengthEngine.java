package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.service.projection.model.LeagueStandingsSnapshot;
import com.fploptimizer.backend.service.projection.model.LineupDegradation;
import com.fploptimizer.backend.service.projection.model.MatchContext;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * Computational engine responsible for evaluating team-level match strengths and fixture dynamics.
 *
 * Combines baseline official Fixture Difficulty Ratings (FDR) with empirical form using
 * Bayesian shrinkage, adjusts for squad injury degradation and table motivation stakes,
 * and derives expected Poisson scoring rates (lambdas) and clean sheet probabilities.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class FixtureStrengthEngine {

    // Historical home and away averages
    private static final double LEAGUE_AVG_HOME_GOALS = 1.55;
    private static final double LEAGUE_AVG_AWAY_GOALS = 1.25;
    private static final double LEAGUE_AVG_OVERALL = 1.40;

    // Environmental home advantage and away disadvantage multipliers
    private static final double HOME_ADVANTAGE_ATTACK = 1.10; // +10% for home team attack
    private static final double AWAY_DISADVANTAGE_DEFENSE = 1.05; //

    // Clamping boundaries for Poisson expected goals (lambda)
    private static final double MIN_LAMBDA = 0.20;
    private static final double MAX_LAMBDA = 4.50;

    // Boundaries and shrinkage strength for baseline team ratings
    private static final double MIN_BASE_RATING = 0.65;
    private static final double MAX_BASE_RATING = 1.60;
    private static final int SHRINKAGE_PRIOR_WEIGHT = 10; // Equivalent sample size of prior belief

    private final LineupDegradationEngine lineupDegradationEngine;
    private final MatchStakesEngine matchStakesEngine;

    /**
     * Constructs a comprehensive MatchContext for a specific fixture by evaluating both participating teams.
     *
     * The evaluation process runs in the following phases:
     * 1. Evaluates squad availability and injury penalties via LineupDegradationEngine.
     * 2. Computes competitive motivation from league standings via MatchStakesEngine.
     * 3. Calculates Bayesian blended baseline attack and defense ratings.
     * 4. Synthesizes final team strengths considering venue factors.
     * 5. Solves Poisson scoring lambdas (expected goals scored and conceded).
     * 6. Calculates zero-goal clean sheet probabilities using P(X=0) = e^(-lambda).
     *
     * @param fixtureId      the unique identifier of the scheduled fixture
     * @param gameweek       the active gameweek round number
     * @param homeTeam       the home team entity
     * @param awayTeam       the away team entity
     * @param homePlayers    all registered squad members belonging to the home team
     * @param awayPlayers    all registered squad members belonging to the away team
     * @param homeStandings  current league table snapshot for the home team
     * @param awayStandings  current league table snapshot for the away team
     * @return a fully populated MatchContext containing team projections and environmental multipliers
     */
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

        // 6. Clean Sheet Probabilities (P(X=0) = e^(-expectedGoals))
        double homeCsProb = Math.exp(-awayLambda);
        double awayCsProb = Math.exp(-homeLambda);

        // 7.TeamStrengthProjection records construction
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

    /**
     * Estimates baseline team offensive strength using a Bayesian shrinkage weighting formula.
     *
     * Balances official pre-season FDR strength with empirical goals scored per game.
     * Early in the season, when matches played (N) is small, the prior FDR carries most weight.
     * As the season advances, empirical output progressively dominates the rating.
     *
     * @param team       the team being evaluated
     * @param standings  league performance snapshot containing matches played and goals scored
     * @param isHome     indicates whether the match is played at home venue
     * @return the normalized base attack multiplier, clamped within [MIN_BASE_RATING, MAX_BASE_RATING]
     */
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

    /**
     * Estimates baseline team defensive strength using a Bayesian shrinkage weighting formula.
     *
     * Lower values indicate a stronger defense (fewer goals conceded).
     * High FDR teams start with lower defensive multipliers (superior defense), adjusted
     * continuously against realized goals conceded as match sample size accumulates.
     *
     * @param team       the team being evaluated
     * @param standings  league performance snapshot containing matches played and goals conceded
     * @param isHome     indicates whether the match is played at home venue
     * @return the normalized base defense multiplier, clamped within [MIN_BASE_RATING, MAX_BASE_RATING]
     */
    private double calculateBaseDefense(Team team, LeagueStandingsSnapshot standings, boolean isHome) {
        int fdr = resolveTeamStrength(team, isHome);

        // Bigger FDR means better team
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

    /**
     * Resolves the official FPL team strength rating based on venue (Home/Away).
     *
     * Defaults to 3 (average league strength) if the team entity or metric is absent.
     *
     * @param team    the target team entity
     * @param isHome  true if resolving for home venue, false for away
     * @return the integer strength rating (typically 1 to 5)
     */
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
