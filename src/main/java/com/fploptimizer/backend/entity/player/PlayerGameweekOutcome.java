package com.fploptimizer.backend.entity.player;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PlayerGameweekOutcome {

    private Boolean wasStarter;

    // ---Actual events ---
    private Integer actualMinutes;
    private Integer actualGoals;
    private Integer actualAssists;
    private Integer actualCleanSheets;
    private Integer actualGoalsConceded;
    private Integer actualSaves;

    private Integer actualBonus;
    private Integer actualBps;

    // --- Actual DefCon ---
    private Integer actualDefensiveContribution;

    // --- Disciplinary and Penalties ---
    private Integer actualYellowCards;
    private Integer actualRedCards;
    private Integer actualPenaltiesMissed;
    private Integer actualPenaltiesSaved;

    // --- Official Actual Advanced Metrics of the match ---
    private Double actualXg;
    private Double actualXa;
    private Double actualXgc;

    // --- Actual total points and Model Error ---
    private Integer actualTotalPoints;


    private Double predictionDelta; // actualTotalPoints - projectedXp
}