package com.fploptimizer.backend.entity.player;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public  class PlayerGameweekProjection {

    // --- Inputs from Poisson / CSV ---
    private Double projectedTeamGoals;        // λ_team
    private Double projectedOpponentGoals;    // λ_opp

    // --- Player Projections ---
    private Double projectedMinutes;          // xMin
    private Double projectedXg;               // xG
    private Double projectedXa;               // xA


    private Double projectedCleanSheetProb;   // P(CS)
    private Double projectedGoalsConceded;    // xGC
    // --- Defensive Contribution (10 DefCon = 2 pts) ---
    private Double projectedDefensiveContribution; // μ_def
    private Double projectedDefConProb;            // P(DefCon >= 10)
    // --- Saves & Discipline ---
    private Double projectedSaves;


    private Double projectedCardsProb;

    // ---  ---
    private Double projectedBonus;
    private Double projectedXp;               // Total Xp
}