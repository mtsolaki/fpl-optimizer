package com.fploptimizer.backend.service.projection.model;

public record TeamStrengthProjection(
        int teamId,
        double baseAttackRating,
        double baseDefenseRating,
        double attackDegradation,
        double defenseDegradation,
        double motivationMultiplier,
        double finalAttackStrength,
        double finalDefenseStrength,
        double expectedGoals,
        double expectedGoalsConceded,
        double cleanSheetProbability
) {

}
