package com.fploptimizer.backend.service.projection;

import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.FixtureProjection;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.repository.FixtureProjectionRepository;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import com.fploptimizer.backend.service.LeagueStandingsService;
import com.fploptimizer.backend.service.projection.engine.FixtureStrengthEngine;
import com.fploptimizer.backend.service.projection.model.LeagueStandingsSnapshot;
import com.fploptimizer.backend.service.projection.model.MatchContext;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for calculating and persisting team-level match projections for a given gameweek.
 * Orchestrates league table stand snapshots, squad roster aggregation, and Poisson match simulation
 * via FixtureStrengthEngine. Persists baseline ratings, team degradation, motivation multipliers,
 * expected goal rates (lambdas), and clean sheet probabilities for both participating clubs.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FixtureProjectionService {

    private final LeagueStandingsService leagueStandingsService;
    private final FixtureStrengthEngine fixtureStrengthEngine;

    private final FixtureRepository fixtureRepository;
    private final PlayerRepository playerRepository;
    private final FixtureProjectionRepository fixtureProjectionRepository;

    /**
     * Executes team strength and scoreline projections for all scheduled fixtures in a target gameweek.
     * Pre-loads players, standings, and existing projection records to prevent MapsId identifier collisions,
     * computes full match contexts via FixtureStrengthEngine, and batch-persists results.
     *
     * @param gameweek the target gameweek round number
     * @return list of persisted FixtureProjection entities
     */
    public List<FixtureProjection> calculateAndPersistProjections(int gameweek) {
        log.info("Starting projection calculation for gameweek: {}", gameweek);
        Map<Integer, LeagueStandingsSnapshot> standings = leagueStandingsService.calculateStandingsUpToGameweek(gameweek);

        List<Fixture> fixtures = fixtureRepository.findByGameweek(gameweek);
        if (fixtures.isEmpty()) {
            log.warn("No fixtures found for gameweek: {}", gameweek);
            return Collections.emptyList();
        }

        List<Player> allPlayers = playerRepository.findAll();

        Map<Integer, List<Player>> playersByTeam = allPlayers.stream()
                .filter(p -> p.getTeam() != null)
                .collect(Collectors.groupingBy(p -> p.getTeam().getId()));

        // Pre-fetch existing projections to avoid primary key conflicts with @MapsId
        Map<Integer, FixtureProjection> existingProjections = fixtureProjectionRepository.findAllById(
                fixtures.stream().map(Fixture::getId).toList()
        ).stream().collect(Collectors.toMap(FixtureProjection::getId, fp -> fp));

        List<FixtureProjection> projectionsToSave = new ArrayList<>();

        for (Fixture fixture : fixtures) {
            Team homeTeam = fixture.getHomeTeam();
            Team awayTeam = fixture.getAwayTeam();

            if (homeTeam == null || awayTeam == null) {
                log.warn("Fixture ID {} is missing home or away team, skipping", fixture.getId());
                continue;
            }
            int homeId = homeTeam.getId();
            int awayId = awayTeam.getId();

            List<Player> homePlayers = playersByTeam.getOrDefault(homeId, Collections.emptyList());
            List<Player> awayPlayers = playersByTeam.getOrDefault(awayId, Collections.emptyList());

            LeagueStandingsSnapshot homeStandings = standings.get(homeId);
            LeagueStandingsSnapshot awayStandings = standings.get(awayId);

            // Execute Poisson model
            MatchContext matchContext = fixtureStrengthEngine.buildMatchContext(
                    fixture.getId(),
                    gameweek,
                    homeTeam,
                    awayTeam,
                    homePlayers,
                    awayPlayers,
                    homeStandings,
                    awayStandings
            );

            // Retrieve existing or initialize a new projection entity
            FixtureProjection projection = existingProjections.getOrDefault(fixture.getId(), new FixtureProjection());
            populateProjection(projection, fixture, gameweek, matchContext);
            projectionsToSave.add(projection);
        }

        List<FixtureProjection> savedProjections = fixtureProjectionRepository.saveAll(projectionsToSave);
        log.info("Successfully persisted {} projections for gameweek: {}", savedProjections.size(), gameweek);

        return savedProjections;
    }

    /**
     * Maps calculated MatchContext outputs and post-match realization metrics onto a FixtureProjection entity.
     *
     * @param projection target projection entity to populate
     * @param fixture    underlying fixture entity
     * @param gameweek   the gameweek identifier
     * @param context    computed team projections and environmental multipliers
     */
    private void populateProjection(FixtureProjection projection, Fixture fixture, int gameweek, MatchContext context) {
        TeamStrengthProjection home = context.homeProjection();
        TeamStrengthProjection away = context.awayProjection();

        // Target fixture automatically assigns ID via JPA @MapsId
        projection.setFixture(fixture);
        projection.setGameweek(gameweek);
        projection.setHomeTeam(fixture.getHomeTeam());
        projection.setAwayTeam(fixture.getAwayTeam());

        // Home metrics
        projection.setHomeBaseAttack(home.baseAttackRating());
        projection.setHomeBaseDefense(home.baseDefenseRating());
        projection.setHomeAttackDegradation(home.attackDegradation());
        projection.setHomeDefenseDegradation(home.defenseDegradation());
        projection.setHomeMotivation(home.motivationMultiplier());
        projection.setHomeFinalAttack(home.finalAttackStrength());
        projection.setHomeFinalDefense(home.finalDefenseStrength());
        projection.setHomeLambda(home.expectedGoals());
        projection.setHomeCleanSheetProb(home.cleanSheetProbability());

        // Away metrics
        projection.setAwayBaseAttack(away.baseAttackRating());
        projection.setAwayBaseDefense(away.baseDefenseRating());
        projection.setAwayAttackDegradation(away.attackDegradation());
        projection.setAwayDefenseDegradation(away.defenseDegradation());
        projection.setAwayMotivation(away.motivationMultiplier());
        projection.setAwayFinalAttack(away.finalAttackStrength());
        projection.setAwayFinalDefense(away.finalDefenseStrength());
        projection.setAwayLambda(away.expectedGoals());
        projection.setAwayCleanSheetProb(away.cleanSheetProbability());

        // Post-match evaluation metrics for completed matches
        if (Boolean.TRUE.equals(fixture.getFinished())
                && fixture.getHomeTeamScore() != null
                && fixture.getAwayTeamScore() != null) {

            projection.setHomeGoalsDelta(fixture.getHomeTeamScore() - home.expectedGoals());
            projection.setAwayGoalsDelta(fixture.getAwayTeamScore() - away.expectedGoals());
            projection.setHomeCsHit(fixture.getAwayTeamScore() == 0);
            projection.setAwayCsHit(fixture.getHomeTeamScore() == 0);
        }
    }
}