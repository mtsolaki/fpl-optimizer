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

    // --- Πραγματική συμμετοχή & συμβάντα ---
    private Integer actualMinutes;
    private Integer actualGoals;
    private Integer actualAssists;
    private Integer actualCleanSheets;
    private Integer actualGoalsConceded;
    private Integer actualSaves;

    private Integer actualBonus;
    private Integer actualBps;

    // --- Πραγματικό DefCon ---
    private Integer actualDefensiveContribution;

    // --- Πειθαρχία & Πέναλτι ---
    private Integer actualYellowCards;
    private Integer actualRedCards;
    private Integer actualPenaltiesMissed;
    private Integer actualPenaltiesSaved;

    // --- Επίσημα Actual Advanced Metrics του αγώνα ---
    private Double actualXg;
    private Double actualXa;
    private Double actualXgc;

    // --- Τελικοί Πόντοι & Σφάλμα Μοντέλου ---
    private Integer actualTotalPoints;


    private Double predictionDelta; // actualTotalPoints - projectedXp
}