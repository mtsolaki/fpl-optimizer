package com.fploptimizer.backend.service.projection.model;

public record PlayerDefenceResult(
        double cleanSheetProbability, // P (CS)
        double expectedGoalsConceded, // l_gc
        double expectedConcededGoalsDeduction, // Expected blocks of 2 goals
        double projectedSaves, // l_saves for GKP
        double projectedDefCon, // expected defCon
        double projectedDefConProb // P( DefCon >= 10 )
) {
}
