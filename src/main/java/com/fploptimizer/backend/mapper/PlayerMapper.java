package com.fploptimizer.backend.mapper;

import com.fploptimizer.backend.dto.PlayerDto;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.entity.player.PlayerAdvancedMetrics;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.entity.player.PlayerPerformance;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public Player toEntity(PlayerDto dto, Team team) {
        if (dto == null) {
            return null;
        }

        Player player = new Player();

        // 1. Identity & Core Fields
        player.setId(dto.getId());
        player.setWebName(dto.getWebName());
        player.setFirstName(dto.getFirstName());
        player.setLastName(dto.getLastName());
        player.setPosition(dto.getPosition());
        player.setStatus(dto.getStatus());
        player.setTeam(team);

        // 2. Performance Metrics
        PlayerPerformance performance = player.getPerformance();
        performance.setMinutes(dto.getMinutes());
        performance.setStarts(dto.getStarts());
        performance.setTotalPoints(dto.getTotalPoints());
        performance.setPointsPerGame(parseDoubleSafe(dto.getPointsPerGame()));
        performance.setGoalsScored(dto.getGoalsScored());
        performance.setAssists(dto.getAssists());
        performance.setCleanSheets(dto.getCleanSheets());
        performance.setGoalsConceded(dto.getGoalsConceded());
        performance.setSaves(dto.getSaves());
        performance.setBonus(dto.getBonus());
        performance.setBps(dto.getBps());

        // 3. Advanced Expected Metrics
        PlayerAdvancedMetrics advancedMetrics = player.getAdvancedMetrics();
        advancedMetrics.setForm(dto.getForm());
        advancedMetrics.setExpectedGoals(parseDoubleSafe(dto.getExpectedGoals()));
        advancedMetrics.setExpectedAssists(parseDoubleSafe(dto.getExpectedAssists()));
        advancedMetrics.setExpectedGoalInvolvements(parseDoubleSafe(dto.getExpectedGoalInvolvements()));
        advancedMetrics.setExpectedGoalsConceded(parseDoubleSafe(dto.getExpectedGoalsConceded()));
        advancedMetrics.setExpectedGoalsPer90(parseDoubleSafe(dto.getExpectedGoalsPer90()));
        advancedMetrics.setExpectedAssistsPer90(parseDoubleSafe(dto.getExpectedAssistsPer90()));
        advancedMetrics.setExpectedGoalInvolvementsPer90(parseDoubleSafe(dto.getExpectedGoalInvolvementsPer90()));
        advancedMetrics.setExpectedGoalsConcededPer90(parseDoubleSafe(dto.getExpectedGoalsConcededPer90()));

        // 4. Market & Availability Info
        PlayerMarketInfo marketInfo = player.getMarketInfo();
        if (dto.getCurrentPrice() != null) {
            marketInfo.setCurrentPrice(dto.getCurrentPrice() / 10.0);
        }
        marketInfo.setSelectedByPercent(parseDoubleSafe(dto.getSelectedByPercent()));
        marketInfo.setChanceOfPlayingNextRound(dto.getChanceOfPlayingNextRound());
        marketInfo.setNews(dto.getNews());
        marketInfo.setEpNext(dto.getEpNext());

        return player;
    }

    private Double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException _) {
            return null;
        }
    }
}
