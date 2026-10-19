package com.fploptimizer.backend.service.sync;

import com.fploptimizer.backend.dto.live.LiveElementDto;
import com.fploptimizer.backend.dto.live.LiveGameweekResponseDto;
import com.fploptimizer.backend.dto.live.LiveStatsDto;
import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.player.PlayerGameweekOutcome;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.GameweekRepository;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for ingesting live and settled match performance statistics for all players.
 * Fetches actual performance data from the official FPL live gameweek endpoint, updates
 * PlayerGameweekLog entities with realized match outcomes, and calculates prediction discrepancy
 * deltas against projected expected points (xP) for analytics and model backtesting.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveGameweekSyncService {

    private final RestTemplate restTemplate;
    private final PlayerGameweekLogRepository logRepository;
    private final PlayerRepository playerRepository;
    private final FixtureRepository fixtureRepository;
    private final GameweekRepository gameweekRepository;

    /**
     * Synchronizes realized statistics for all active players in a designated gameweek.
     * Iterates through elements in the live payload, matches or initializes the corresponding
     * PlayerGameweekLog, maps Opta and official FPL actual performance outcomes, computes prediction
     * error deltas, and persists updates in batch.
     *
     * @param gameweekId the unique identifier of the target gameweek
     * @param season     the competitive season string (e.g., "2026-2027")
     */
    @Transactional
    public void syncEntireGameweek(int gameweekId, String season) {
        String url = "https://fantasy.premierleague.com/api/event/" + gameweekId + "/live/";

        log.info("Fetching live stats for GW {} from: {}", gameweekId, url);
        LiveGameweekResponseDto response = restTemplate.getForObject(url, LiveGameweekResponseDto.class);

        if (response == null || response.getElements() == null) {
            log.warn("No element data found for gameweek: {}", gameweekId);
            return;
        }

        // Fetch Gameweek entity once per batch
        Gameweek gameweek = gameweekRepository.findById(gameweekId).orElse(null);

        List<PlayerGameweekLog> logsToSave = new ArrayList<>();

        for (LiveElementDto element : response.getElements()) {
            if (element.getExplain() == null || element.getExplain().isEmpty()) {
                continue; // Blank Gameweek for this player
            }

            Integer fixtureId = element.getExplain().getFirst().getFixtureId();
            if (fixtureId == null) {
                continue;
            }

            int fplPlayerId = element.getId();

            // 1. Find existing log or create a new one (e.g. historical backfill)
            PlayerGameweekLog logEntry = logRepository
                    .findByFplPlayerIdAndFixtureIdAndSeason(fplPlayerId, fixtureId, season)
                    .orElseGet(() -> createNewLog(fplPlayerId, fixtureId, gameweek, season));

            if (logEntry == null) {
                // Skip safely if player or fixture is not found in database
                continue;
            }

            // 2. Map incoming statistics to the embedded Outcome
            mapStatsToOutcome(element.getStats(), logEntry);
            logsToSave.add(logEntry);
        }

        // 3. Batch upsert
        if (!logsToSave.isEmpty()) {
            logRepository.saveAll(logsToSave);
            log.info("Successfully persisted {} logs/outcomes for GW {}", logsToSave.size(), gameweekId);
        }
    }

    /**
     * Maps raw live statistics from the FPL API onto the embedded PlayerGameweekOutcome component.
     * Computes the model projection discrepancy delta (Actual Points - Projected xP) if prior
     * projections are present on the log.
     *
     * @param stats incoming live statistics payload
     * @param log   target log entity to update
     */
    private void mapStatsToOutcome(LiveStatsDto stats, PlayerGameweekLog log) {
        if (stats == null) return;

        PlayerGameweekOutcome outcome = log.getOutcome();
        if (outcome == null) {
            outcome = new PlayerGameweekOutcome();
            log.setOutcome(outcome);
        }

        outcome.setWasStarter(stats.getStarts() != null && stats.getStarts() == 1);
        outcome.setActualMinutes(stats.getMinutes());
        outcome.setActualGoals(stats.getGoalsScored());
        outcome.setActualAssists(stats.getAssists());
        outcome.setActualCleanSheets(stats.getCleanSheets());
        outcome.setActualGoalsConceded(stats.getGoalsConceded());
        outcome.setActualSaves(stats.getSaves());

        outcome.setActualBonus(stats.getBonus());
        outcome.setActualBps(stats.getBps());

        outcome.setActualDefensiveContribution(stats.getDefensiveContribution());

        outcome.setActualYellowCards(stats.getYellowCards());
        outcome.setActualRedCards(stats.getRedCards());
        outcome.setActualPenaltiesMissed(stats.getPenaltiesMissed());
        outcome.setActualPenaltiesSaved(stats.getPenaltiesSaved());

        // Safe double parsing for Opta Advanced Metrics
        outcome.setActualXg(parseDouble(stats.getExpectedGoals()));
        outcome.setActualXa(parseDouble(stats.getExpectedAssists()));
        outcome.setActualXgc(parseDouble(stats.getExpectedGoalsConceded()));

        outcome.setActualTotalPoints(stats.getTotalPoints());

        // Calculate model error: actual - projected
        if (stats.getTotalPoints() != null
                && log.getProjection() != null
                && log.getProjection().getProjectedXp() != null) {
            outcome.setPredictionDelta(stats.getTotalPoints() - log.getProjection().getProjectedXp());
        }
    }

    /**
     * Instantiates a new historical or backfill PlayerGameweekLog when none exists.
     * Captures fixture home/away context, difficulty, and player set-piece responsibilities.
     *
     * @param fplPlayerId unique external FPL player identifier
     * @param fixtureId   target fixture ID
     * @param gameweek    associated gameweek entity
     * @param season      target season identifier
     * @return initialized PlayerGameweekLog or null if required entities are absent
     */
    private PlayerGameweekLog createNewLog(int fplPlayerId, int fixtureId, Gameweek gameweek, String season) {
        Player player = playerRepository.findByFplId(fplPlayerId).orElse(null);
        Fixture fixture = fixtureRepository.findById(fixtureId).orElse(null);

        if (player == null || fixture == null) {
            log.warn("Entity not found: player (id: {}) or fixture (id: {})", fplPlayerId, fixtureId);
            return null;
        }

        PlayerGameweekLog newLog = new PlayerGameweekLog();
        newLog.setPlayer(player);
        newLog.setFixture(fixture);
        newLog.setGameweek(gameweek);
        newLog.setSeason(season);

        // Determine home/away context and opponent
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

    /**
     * Safely converts string-encoded metric values to Double.
     *
     * @param value the string representation of a floating-point value
     * @return parsed Double value, or 0.0 if unparseable or blank
     */
    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException _) {
            return 0.0;
        }
    }
}