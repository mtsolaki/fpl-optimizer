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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Transactional
    public void runProjectionsForUpcomingGameweek() {
        // 1. Identify upcoming gameweek
        Gameweek upcomingGw = gameweekRepository.findAll().stream()
                .filter(gw -> Boolean.TRUE.equals(gw.getIsNext()) || Boolean.TRUE.equals(gw.getIsCurrent()))
                .filter(gw -> !Boolean.TRUE.equals(gw.getFinished()))
                .findFirst()
                .orElse(null);

        if (upcomingGw == null) {
            log.warn("No upcoming active gameweek found. Aborting projections run.");
            return;
        }

        int gwId = upcomingGw.getId();
        log.info("Executing batch projections pipeline for upcoming Gameweek {}", gwId);

        // 2. Step 1: Calculate & Persist Fixture Projections
        List<FixtureProjection> fixtureProjections = fixtureProjectionService.calculateAndPersistProjections(gwId);
        if (fixtureProjections.isEmpty()) {
            log.warn("No fixture projections generated for GW {}. Aborting player projections.", gwId);
            return;
        }

        // Map teamId to its respective TeamStrengthProjection for quick O(1) lookup
        Map<Integer, TeamStrengthProjection> teamProjectionsMap = buildTeamProjectionsMap(fixtureProjections);

        // 3. Step 2: Load Fixtures for this GW
        List<Fixture> fixtures = fixtureRepository.findByGameweek(gwId);
        Map<Integer, Fixture> teamFixtureMap = new HashMap<>();
        for (Fixture f : fixtures) {
            if (f.getHomeTeam() != null) teamFixtureMap.put(f.getHomeTeam().getId(), f);
            if (f.getAwayTeam() != null) teamFixtureMap.put(f.getAwayTeam().getId(), f);
        }

        // 4. Step 3: Fetch all active players and their recent historical logs
        List<Player> allPlayers = playerRepository.findAll();
        List<PlayerGameweekLog> allSettledLogs = logRepository.findAllSettledLogsUpToGameweek(gwId);

        Map<Integer, List<PlayerGameweekLog>> logsByPlayerCode = allSettledLogs.stream()
                .filter(l -> l.getPlayer() != null)
                .collect(Collectors.groupingBy(l -> l.getPlayer().getCode()));

        List<PlayerGameweekLog> logsToSave = new ArrayList<>();

        // 5. Step 4: Calculate projections for each player
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

            // Upsert PlayerGameweekLog for upcoming gameweek
            PlayerGameweekLog logEntry = logRepository
                    .findByFplPlayerIdAndFixtureIdAndSeason(player.getId(), fixture.getId(), currentSeason)
                    .orElseGet(() -> createUpcomingLog(player, fixture, upcomingGw, currentSeason));

            logEntry.setProjection(projection);
            logEntry.setMarketInfo(marketInfo);
            logsToSave.add(logEntry);
        }

        // 6. Step 5: Batch Upsert Logs with Projections
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
}
