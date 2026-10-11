package com.fploptimizer.backend.service;

import com.fploptimizer.backend.dto.live.LiveElementDto;
import com.fploptimizer.backend.dto.live.LiveGameweekResponseDto;
import com.fploptimizer.backend.dto.live.LiveStatsDto;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.player.PlayerGameweekOutcome;

import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


/*
 Service used to collect all the data for every match for every player
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveGameweekSyncService {

    private final RestTemplate restTemplate;
    private final PlayerGameweekLogRepository logRepository;


    @Transactional
    public void syncEntireGameweek(int gameweekId, String season) {
        String url = "https://fantasy.premierleague.com/api/event/" + gameweekId + "/live/";
        LiveGameweekResponseDto response = restTemplate.getForObject(url, LiveGameweekResponseDto.class);

        if (response == null || response.getElements() == null) {
            return;
        }

        for (LiveElementDto element : response.getElements()) {
            if (element.getExplain() == null || element.getExplain().isEmpty()) {
                continue; // Blank Gameweek
            }

            // Get FixtureId from explainObject

            Integer fixtureId = element.getExplain().getFirst().getFixtureId();

            // 1 find & update
            logRepository.findByFplPlayerIdAndFixtureIdAndSeason(element.getId(), fixtureId, season)
                    .ifPresent(logEntry -> {
                        mapStatsToOutcome(element.getStats(), logEntry);
                        logRepository.save(logEntry);
                    });
        }
    }

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

        // Calculation for model error: actual - projected
        if (stats.getTotalPoints() != null && log.getProjection() != null && log.getProjection().getProjectedXp() != null) {
            outcome.setPredictionDelta(stats.getTotalPoints() - log.getProjection().getProjectedXp());
        }
    }

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