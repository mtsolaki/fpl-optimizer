package com.fploptimizer.backend.service.projection.model;

public record PlayerMinutesResult(
        double projectedMinutes,
        double startProbability,
        double sixtyPlusProbability
) {
}
