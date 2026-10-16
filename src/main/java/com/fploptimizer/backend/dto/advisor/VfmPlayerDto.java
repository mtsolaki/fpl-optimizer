package com.fploptimizer.backend.dto.advisor;

import com.fploptimizer.backend.entity.Position;
import lombok.Builder;

@Builder
public record VfmPlayerDto(
        Integer playerCode,
        Integer fplId,
        String webName,
        String teamShortName,
        Position position,
        Double currentPrice,
        Double projectedXp,
        Double projectedMinutes,
        Double vfmScore, // projectedXp / currentPrice
        String status,
        String news
) {}