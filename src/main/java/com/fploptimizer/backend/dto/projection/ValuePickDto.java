package com.fploptimizer.backend.dto.projection;

import com.fploptimizer.backend.entity.Position;
import lombok.Builder;

@Builder
public record ValuePickDto(
        Integer playerCode,
        String webName,
        String teamShortName,
        Position position,
        Double currentPrice,
        Double selectedByPercent,
        Double projectedXp,
        Double valueIndex,         // xP / Price
        Boolean isDifferential,     // e.g. ownership < 10.0%
        Integer fixtureDifficulty,
        Boolean isHome,
        String opponentShortName
) {}