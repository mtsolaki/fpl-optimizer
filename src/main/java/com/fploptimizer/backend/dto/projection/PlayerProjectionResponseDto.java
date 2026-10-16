package com.fploptimizer.backend.dto.projection;

import com.fploptimizer.backend.entity.Position;
import lombok.Builder;

@Builder
public record PlayerProjectionResponseDto(
        Integer playerCode,
        Integer fplId,
        String webName,
        String teamShortName,
        Position position,
        Double currentPrice,
        Double selectedByPercent,
        Integer fixtureDifficulty,
        Boolean isHome,
        String opponentShortName,
        Double projectedMinutes,
        Double projectedXp,
        Double projectedXg,
        Double projectedXa,
        Double projectedCleanSheetProb,
        Double projectedDefConProb,
        Double projectedBonus,
        Double valuePerMillion // xp / currentPrice
) {}