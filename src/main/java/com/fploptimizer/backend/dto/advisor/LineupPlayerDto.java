package com.fploptimizer.backend.dto.advisor;

import com.fploptimizer.backend.entity.Position;
import lombok.Builder;

@Builder
public record LineupPlayerDto(
        Integer playerCode,
        Integer fplId,
        String webName,
        String teamShortName,
        Position position,
        Double currentPrice,
        Double projectedXp,
        Boolean isCaptain,
        Boolean isViceCaptain,
        String status,
        String news,
        Double projectedMinutes,
        Integer benchOrder // null for starters, 0 for gkp backup, 1-3 for field players
) {}