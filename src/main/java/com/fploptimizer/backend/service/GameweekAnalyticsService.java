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

@Slf4j
@Service
@RequiredArgsConstructor
public class GameweekAnalyticsService {

    private final PlayerGameweekLogRepository logRepository;

    /**
     * Retrieves all player projections and actual outcomes for a target gameweek.
     *
     * @param gameweekId the target gameweek ID
     * @return a list of player reports sorted by projected xP descending
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