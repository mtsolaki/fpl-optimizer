package com.fploptimizer.backend.service;

import com.fploptimizer.backend.entity.*;
import com.fploptimizer.backend.entity.player.PlayerGameweekProjection;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.GameweekRepository;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import com.fploptimizer.backend.service.projection.FixtureProjectionService;
import com.fploptimizer.backend.service.projection.PlayerProjectionService;
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

    // Self-injection of the proxy to guarantee @Transactional execution
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private GameweekProjectionBatchService self;


    /**
     * Executes the projection pipeline for the default upcoming horizon of 5 gameweeks.
     */
    public void runProjectionsForUpcomingGameweeks() {
        runProjectionsForUpcomingGameweeks(5);
    }

    /**
     * Backward compatibility method for single-gameweek execution.
     */
    public void runProjectionsForUpcomingGameweek() {
        runProjectionsForUpcomingGameweeks(1);
    }


    /**
     * Identifies the upcoming active gameweeks and sequentially executes projections
     * for a forward-looking horizon (e.g. next 5 gameweeks).
     *
     * @param horizon the number of future gameweeks to project
     */

    public void runProjectionsForUpcomingGameweeks(int horizon) {
        log.info("Resolving upcoming gameweeks for a {}-gameweek projection horizon...", horizon);

        // 1. Find all unfinished gameweeks, ordered chronologically
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

        // 2. Sequentially run projections for each upcoming gameweek via proxy
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
     * Core execution engine: computes fixture and player projections for a given Gameweek ID.
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

        // Map teamId to its respective TeamStrengthProjection for quick O(1) lookup
        Map<Integer, TeamStrengthProjection> teamProjectionsMap = buildTeamProjectionsMap(fixtureProjections);

        // 2. Load Fixtures for this GW
        List<Fixture> fixtures = fixtureRepository.findByGameweek(gwId);
        Map<Integer, Fixture> teamFixtureMap = new HashMap<>();
        for (Fixture f : fixtures) {
            if (f.getHomeTeam() != null) teamFixtureMap.put(f.getHomeTeam().getId(), f);
            if (f.getAwayTeam() != null) teamFixtureMap.put(f.getAwayTeam().getId(), f);
        }

        // 3. Fetch all active players and their recent historical logs
        List<Player> allPlayers = playerRepository.findAll();
        List<PlayerGameweekLog> allSettledLogs = logRepository.findAllSettledLogsUpToGameweek(gwId);

        Map<Integer, List<PlayerGameweekLog>> logsByPlayerCode = allSettledLogs.stream()
                .filter(l -> l.getPlayer() != null)
                .collect(Collectors.groupingBy(l -> l.getPlayer().getCode()));

        // OPTIMIZATION: Fetch all existing logs for this gameweek in 1 single query (eliminates 700+ SELECT queries)
        Map<Integer, PlayerGameweekLog> existingLogsByPlayerId = logRepository
                .findAllByGameweekIdAndSeason(gwId, currentSeason).stream()
                .filter(l -> l.getPlayer() != null)
                .collect(Collectors.toMap(
                        l -> l.getPlayer().getId(),
                        l -> l,
                        (existing, replacement) -> existing
                ));

        List<PlayerGameweekLog> logsToSave = new ArrayList<>();

        // 4. Calculate projections for each player
        for (Player player : allPlayers) {
            if (player.getTeam() == null) continue;

            int teamId = player.getTeam().getId();
            Fixture fixture = teamFixtureMap.get(teamId);
            TeamStrengthProjection teamStrength = teamProjectionsMap.get(teamId);

            // If team doesn't have a match this gameweek (Blank Gameweek)
            if (fixture == null || teamStrength == null) {
                continue;
            }

            List<PlayerGameweekLog> playerHistoricalLogs = logsByPlayerCode.getOrDefault(player.getCode(), Collections.emptyList());

            // Calculate xP and stats
            PlayerGameweekProjection projection = playerProjectionService.calculatePlayerProjection(
                    player, teamStrength, playerHistoricalLogs);

            PlayerMarketInfo marketInfo = player.getMarketInfo();

            // Instant in-memory check instead of issuing a database query
            PlayerGameweekLog logEntry = existingLogsByPlayerId.get(player.getId());
            if (logEntry == null) {
                logEntry = createUpcomingLog(player, fixture, targetGw, currentSeason);
            }

            logEntry.setProjection(projection);
            logEntry.setMarketInfo(marketInfo);
            logsToSave.add(logEntry);
        }

        // 5. Batch Upsert Logs with Projections
        if (!logsToSave.isEmpty()) {
            logRepository.saveAll(logsToSave);
            log.info("Successfully persisted projections for {} players in GW {}", logsToSave.size(), gwId);
        }

    }
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

        // Snapshot player metadata
        newLog.setStatus(player.getStatus());
        newLog.setPenaltiesOrder(player.getPenaltiesOrder());
        newLog.setDirectFreeKicksOrder(player.getDirectFreeKicksOrder());
        newLog.setCornersAndIndirectFreeKicksOrder(player.getCornersAndIndirectFreeKicksOrder());

        return newLog;
    }

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
     * Backfills model projections for completed gameweeks strictly if projections are missing.
     * Gameweeks that already have computed projections are skipped.
     */
    public void runProjectionsForCompletedGameweeks() {
        log.info("Checking for completed gameweeks lacking projections...");

        // 1. Fetch finished gameweeks from database
        List<Gameweek> finishedGameweeks = gameweekRepository.findAll().stream()
                .filter(gw -> Boolean.TRUE.equals(gw.getFinished()))
                .toList();

        if (finishedGameweeks.isEmpty()) {
            log.info("No completed gameweeks found in database.");
            return;
        }

        // 2. Fetch all gameweek IDs that already have projections computed
        Set<Integer> existingProjectionGwIds = logRepository.findGameweekIdsWithProjections();

        // 3. Keep only completed gameweeks that do NOT have projections yet
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

        // 4. Calculate projections sequentially for each missing gameweek
        for (Integer gwId : missingGwIds) {
            try {
                log.info("Running projections for completed Gameweek {}...", gwId);
                // Call your projection calculation method for this gameweek:
                self.runProjectionsForGameweek(gwId);
                log.info("Successfully completed projections for Gameweek {}", gwId);
            } catch (Exception e) {
                log.error("Failed to run projections for Gameweek {}: {}", gwId, e.getMessage(), e);
            }
        }

        log.info("Completed historical gameweek projections backfill.");
    }
}
