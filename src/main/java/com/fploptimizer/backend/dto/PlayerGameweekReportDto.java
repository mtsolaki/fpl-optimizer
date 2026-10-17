package com.fploptimizer.backend.dto;

import com.fploptimizer.backend.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameweekReportDto {

    // Player metadata
    private int fplId;
    private int playerCode;
    private String webName;
    private String teamShortName;
    private Position position;

    // Projection metrics
    private Double projectedXp;
    private Double projectedMinutes;

    // Actual outcome metrics (null if gameweek is unfinished or in progress)
    private Integer actualPoints;
    private Integer actualMinutes;
}