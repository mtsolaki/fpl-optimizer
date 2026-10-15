package com.fploptimizer.backend.service.projection.model;

public record PlayerAttackResult(
        double expectedGoals,       // Probable xG for the fixture
        double expectedAssists,     // Probable xA for the fixture
        double expectedInvolvements // xGI = xG + xA
) {
}
