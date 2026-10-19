package com.fploptimizer.backend.service.projection;

import com.fploptimizer.backend.entity.*;
import com.fploptimizer.backend.entity.player.PlayerGameweekProjection;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.GameweekRepository;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import com.fploptimizer.backend.service.projection.model.TeamStrengthProjection;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Batch orchestration service managing the end-to-end execution of the projection pipeline.

 * Coordinates multi-gameweek forward projection horizons and historical backfills.
 * Solves transactional self-invocation using @Lazy self-injection and eliminates N+1 query
 * performance bottlenecks using bulk lookups and in-memory Map structures.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameweekProjectionBatchService {

    private final GameweekRepository gameweekRepository;
    private final FixtureRepository fixtureRepository;
    private final PlayerRepository playerRepository;
    private final PlayerGameweekLogRepository logRepository;
    private final FixtureProjectionService fixtureProjectionService;
    private final PlayerProjectionService playerProjectionService;

    @Value("${fpl.current-season:2026-2027}")
    private String currentSeason;

    /**
     * Self-injected bean proxy ensuring that calls to transactional methods preserve proxy boundaries.
     */
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private GameweekProjectionBatchService self;

    /**
     * Executes the projection pipeline for the default upcoming planning horizon of 5 gameweeks.
     */
    public void runProjectionsForUpcomingGameweeks() {
        runProjectionsForUpcomingGameweeks(5);
    }

    /**
     * Single-gameweek backward compatibility wrapper executing projections for the next active gameweek.
     */
    public void runProjectionsForUpcomingGameweek() {
        runProjectionsForUpcomingGameweeks(1);
    }

    /**
     * Identifies active upcoming gameweeks and sequentially executes the projection pipeline
     * across a configured forward horizon.
     *
     * @param horizon total number of future gameweeks to project
     */
    public void runProjectionsForUpcomingGameweeks(int horizon) {
        log.info("Resolving upcoming gameweeks for a {}-gameweek projection horizon...", horizon);

        // 1. Identify upcoming unfinished gameweeks ordered chronologically
        List<Gameweek> upcomingGameweeks = gameweekRepository.findAll().stream()
                .filter(gw -> !Boolean.TRUE.equals(gw.getFinished()))
                .sorted(Comparator.comparingInt(Gameweek::getId))
                .limit(horizon)
                .toList();

        if (upcomingGameweeks.isEmpty()) {
            log.warn("No upcoming unfinished gameweeks found. Skipping upcoming projections.");
            return;
        }

        List<Integer> upcomingGwIds = upcomingGameweeks.stream().map(Gameweek::getId).toList();
        log.info("Executing projections for upcoming Gameweeks: {}", upcomingGwIds);

        // 2. Sequentially run projections for each gameweek through the injected proxy
        for (Gameweek gw : upcomingGameweeks) {
            try {
                log.info("--> Starting projections calculation for upcoming Gameweek {}", gw.getId());
                self.runProjectionsForGameweek(gw.getId());
                log.info("--> Successfully completed projections for upcoming Gameweek {}", gw.getId());
            } catch (Exception e) {
                log.error("Failed to compute projections for upcoming Gameweek {}: {}", gw.getId(), e.getMessage(), e);
            }
        }

        log.info("Multi-gameweek forward projections horizon ({}) completed.", upcomingGwIds.size());
    }

    /**
     * Core execution pipeline: computes fixture and player projections for an individual gameweek.
     * Eliminates N+1 database queries by batch-loading historical logs and pre-existing gameweek logs
     * into hash-indexed collections, computes expected points (xP) for all active players,
     * and performs a batch upsert.
     *
     * @param gwId the target gameweek ID
     */
    @Transactional
    public void runProjectionsForGameweek(int gwId) {
        Gameweek targetGw = gameweekRepository.findById(gwId).orElse(null);
        if (targetGw == null) {
            log.warn("Gameweek with ID {} not found. Aborting projections run.", gwId);
            return;
        }

        log.info("Executing batch projections pipeline for Gameweek {}", gwId);

        // 1. Calculate & Persist Fixture Projections
        List<FixtureProjection> fixtureProjections = fixtureProjectionService.calculateAndPersistProjections(gwId);
        if (fixtureProjections.isEmpty()) {
            log.warn("No fixture projections generated for GW {}. Aborting player projections.", gwId);
            return;
        }

        // Map teamId to TeamStrengthProjection for O(1) in-memory lookups
        Map<Integer, TeamStrengthProjection> teamProjectionsMap = buildTeamProjectionsMap(fixtureProjections);

        // 2. Load scheduled fixtures for this gameweek
        List<Fixture> fixtures = fixtureRepository.findByGameweek(gwId);
        Map<Integer, Fixture> teamFixtureMap = new HashMap<>();
        for (Fixture f : fixtures) {
            if (f.getHomeTeam() != null) teamFixtureMap.put(f.getHomeTeam().getId(), f);
            if (f.getAwayTeam() != null) teamFixtureMap.put(f.getAwayTeam().getId(), f);
        }

        // 3. Batch load active players and settled historical logs up to this round
        List<Player> allPlayers = playerRepository.findAll();
        List<PlayerGameweekLog> allSettledLogs = logRepository.findAllSettledLogsUpToGameweek(gwId);

        Map<Integer, List<PlayerGameweekLog>> logsByPlayerCode = allSettledLogs.stream()
                .filter(l -> l.getPlayer() != null)
                .collect(Collectors.groupingBy(l -> l.getPlayer().getCode()));

        // Single bulk query loading existing logs for this gameweek (eliminates 700+ individual queries)
        Map<Integer, PlayerGameweekLog> existingLogsByPlayerId = logRepository
                .findAllByGameweekIdAndSeason(gwId, currentSeason).stream()
                .filter(l -> l.getPlayer() != null)
                .collect(Collectors.toMap(
                        l -> l.getPlayer().getId(),
                        l -> l,
                        (existing, replacement) -> existing
                ));

        List<PlayerGameweekLog> logsToSave = new ArrayList<>();

        // 4. Compute model projections for each registered player
        for (Player player : allPlayers) {
            if (player.getTeam() == null) continue;

            int teamId = player.getTeam().getId();
            Fixture fixture = teamFixtureMap.get(teamId);
            TeamStrengthProjection teamStrength = teamProjectionsMap.get(teamId);

            // Skip players whose teams do not play in this gameweek (Blank Gameweek)
            if (fixture == null || teamStrength == null) {
                continue;
            }

            List<PlayerGameweekLog> playerHistoricalLogs = logsByPlayerCode.getOrDefault(player.getCode(), Collections.emptyList());

            // Compute expected points and granular metric projections
            PlayerGameweekProjection projection = playerProjectionService.calculatePlayerProjection(
                    player, teamStrength, playerHistoricalLogs);

            PlayerMarketInfo marketInfo = player.getMarketInfo();

            // Resolve existing entity or instantiate a new upcoming log
            PlayerGameweekLog logEntry = existingLogsByPlayerId.get(player.getId());
            if (logEntry == null) {
                logEntry = createUpcomingLog(player, fixture, targetGw, currentSeason);
            }

            logEntry.setProjection(projection);
            logEntry.setMarketInfo(marketInfo);
            logsToSave.add(logEntry);
        }

        // 5. Batch persist logs containing updated projections
        if (!logsToSave.isEmpty()) {
            logRepository.saveAll(logsToSave);
            log.info("Successfully persisted projections for {} players in GW {}", logsToSave.size(), gwId);
        }
    }

    /**
     * Factory method creating a new PlayerGameweekLog entity pre-populated with match and player metadata snapshots.
     *
     * @param player   target player entity
     * @param fixture  scheduled fixture entity
     * @param gameweek target gameweek entity
     * @param season   active season identifier
     * @return initialized PlayerGameweekLog entity
     */
    private PlayerGameweekLog createUpcomingLog(Player player, Fixture fixture, Gameweek gameweek, String season) {
        PlayerGameweekLog newLog = new PlayerGameweekLog();
        newLog.setPlayer(player);
        newLog.setFixture(fixture);
        newLog.setGameweek(gameweek);
        newLog.setSeason(season);

        boolean isHome = player.getTeam() != null && fixture.getHomeTeam() != null
                && player.getTeam().getId() == fixture.getHomeTeam().getId();

        newLog.setIsHome(isHome);
        newLog.setOpponentTeam(isHome ? fixture.getAwayTeam() : fixture.getHomeTeam());
        newLog.setFixtureDifficulty(isHome ? fixture.getHomeTeamDifficulty() : fixture.getAwayTeamDifficulty());

        // Snapshot current player tactical roles
        newLog.setStatus(player.getStatus());
        newLog.setPenaltiesOrder(player.getPenaltiesOrder());
        newLog.setDirectFreeKicksOrder(player.getDirectFreeKicksOrder());
        newLog.setCornersAndIndirectFreeKicksOrder(player.getCornersAndIndirectFreeKicksOrder());

        return newLog;
    }

    /**
     * Converts a collection of FixtureProjection records into a fast lookup map keyed by Team ID.
     *
     * @param fps fixture projections list
     * @return map linking team IDs to their respective strength and goal rate projections
     */
    private Map<Integer, TeamStrengthProjection> buildTeamProjectionsMap(List<FixtureProjection> fps) {
        Map<Integer, TeamStrengthProjection> map = new HashMap<>();
        for (FixtureProjection fp : fps) {
            if (fp.getHomeTeam() != null) {
                map.put(fp.getHomeTeam().getId(), new TeamStrengthProjection(
                        fp.getHomeTeam().getId(),
                        fp.getHomeBaseAttack(),
                        fp.getHomeBaseDefense(),
                        fp.getHomeAttackDegradation(),
                        fp.getHomeDefenseDegradation(),
                        fp.getHomeMotivation(),
                        fp.getHomeFinalAttack(),
                        fp.getHomeFinalDefense(),
                        fp.getHomeLambda(),
                        fp.getAwayLambda(), // Opponent goals = Away Lambda
                        fp.getHomeCleanSheetProb()
                ));
            }
            if (fp.getAwayTeam() != null) {
                map.put(fp.getAwayTeam().getId(), new TeamStrengthProjection(
                        fp.getAwayTeam().getId(),
                        fp.getAwayBaseAttack(),
                        fp.getAwayBaseDefense(),
                        fp.getAwayAttackDegradation(),
                        fp.getAwayDefenseDegradation(),
                        fp.getAwayMotivation(),
                        fp.getAwayFinalAttack(),
                        fp.getAwayFinalDefense(),
                        fp.getAwayLambda(),
                        fp.getHomeLambda(), // Opponent goals = Home Lambda
                        fp.getAwayCleanSheetProb()
                ));
            }
        }
        return map;
    }

    /**
     * Identifies completed gameweeks that lack computed projections and backfills them sequentially.
     * Ensures consistent backtesting data across past finished rounds without recomputing
     * already settled projections.
     */
    public void runProjectionsForCompletedGameweeks() {
        log.info("Checking for completed gameweeks lacking projections...");

        // 1. Fetch completed gameweeks from persistence
        List<Gameweek> finishedGameweeks = gameweekRepository.findAll().stream()
                .filter(gw -> Boolean.TRUE.equals(gw.getFinished()))
                .toList();

        if (finishedGameweeks.isEmpty()) {
            log.info("No completed gameweeks found in database.");
            return;
        }

        // 2. Query distinct gameweek IDs already possessing projections
        Set<Integer> existingProjectionGwIds = logRepository.findGameweekIdsWithProjections();

        // 3. Filter for finished rounds lacking projection data
        List<Integer> missingGwIds = finishedGameweeks.stream()
                .map(Gameweek::getId)
                .filter(id -> !existingProjectionGwIds.contains(id))
                .sorted()
                .toList();

        if (missingGwIds.isEmpty()) {
            log.info("All completed gameweeks already have projections. Skipping historical backfill.");
            return;
        }

        log.info("Found {} completed gameweek(s) missing projections: {}", missingGwIds.size(), missingGwIds);

        // 4. Sequentially execute projections for identified backfill rounds
        for (Integer gwId : missingGwIds) {
            try {
                log.info("Running projections for completed Gameweek {}...", gwId);
                self.runProjectionsForGameweek(gwId);
                log.info("Successfully completed projections for Gameweek {}", gwId);
            } catch (Exception e) {
                log.error("Failed to run projections for Gameweek {}: {}", gwId, e.getMessage(), e);
            }
        }

        log.info("Completed historical gameweek projections backfill.");
    }
}