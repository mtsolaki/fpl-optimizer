package com.fploptimizer.backend.service;

import com.fploptimizer.backend.dto.PlayerGameweekReportDto;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service providing analytical reports and projection evaluation metrics across gameweeks.
 *
 * Compiles comparative views of predicted figures (expected points and projected minutes)
 * versus actual realized match outcomes (total points and minutes played) for backtesting and accuracy audits.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameweekAnalyticsService {

    private final PlayerGameweekLogRepository logRepository;

    /**
     * Retrieves aggregated player projections and realized outcomes for a target gameweek.
     *
     * Fetches settled and upcoming player logs joined with parent player entities in a single query
     * and sorts the resulting reports by projected expected points (xP) in descending order.
     *
     * @param gameweekId the unique identifier of the target gameweek
     * @return a list of PlayerGameweekReportDto instances ordered by descending projected xP
     */
    @Transactional(readOnly = true)
    public List<PlayerGameweekReportDto> getGameweekPlayerReports(int gameweekId) {
        List<PlayerGameweekLog> logs = logRepository.findAllByGameweekIdWithPlayer(gameweekId);

        if (logs == null || logs.isEmpty()) {
            log.info("No player logs found for gameweek {}", gameweekId);
            return Collections.emptyList();
        }

        return logs.stream()
                .map(this::mapToDto)
                .sorted((a, b) -> {
                    double xpA = (a.getProjectedXp() != null) ? a.getProjectedXp() : 0.0;
                    double xpB = (b.getProjectedXp() != null) ? b.getProjectedXp() : 0.0;
                    return Double.compare(xpB, xpA); // Highest xP first
                })
                .toList();
    }

    /**
     * Maps an internal PlayerGameweekLog entity and its embedded projection and outcome records
     * into a flattened evaluation DTO.
     *
     * @param logEntry source log entity containing player metadata, projections, and outcomes
     * @return a flattened PlayerGameweekReportDto
     */
    private PlayerGameweekReportDto mapToDto(PlayerGameweekLog logEntry) {
        Player player = logEntry.getPlayer();

        Double projectedXp = (logEntry.getProjection() != null)
                ? logEntry.getProjection().getProjectedXp()
                : null;
        Double projectedMinutes = (logEntry.getProjection() != null)
                ? logEntry.getProjection().getProjectedMinutes()
                : null;

        Integer actualPoints = (logEntry.getOutcome() != null)
                ? logEntry.getOutcome().getActualTotalPoints()
                : null;
        Integer actualMinutes = (logEntry.getOutcome() != null)
                ? logEntry.getOutcome().getActualMinutes()
                : null;

        String teamName = (player != null && player.getTeam() != null)
                ? player.getTeam().getShortName()
                : null;

        return PlayerGameweekReportDto.builder()
                .fplId(player != null ? player.getId() : 0)
                .playerCode(player != null ? player.getCode() : 0)
                .webName(player != null ? player.getWebName() : "Unknown")
                .teamShortName(teamName)
                .position(player != null ? player.getPosition() : null)
                .projectedXp(projectedXp)
                .projectedMinutes(projectedMinutes)
                .actualPoints(actualPoints)
                .actualMinutes(actualMinutes)
                .build();
    }
}