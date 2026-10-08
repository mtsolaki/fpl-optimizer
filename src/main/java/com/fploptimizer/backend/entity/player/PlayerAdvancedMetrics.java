package com.fploptimizer.backend.entity.player;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PlayerAdvancedMetrics {

    private Double form;

    private Double expectedGoals;
    private Double expectedAssists;

    private Double expectedGoalInvolvements;
    private Double expectedGoalsConceded;


    private Double expectedGoalsPer90;
    private Double expectedAssistsPer90;

    private Double expectedGoalInvolvementsPer90;
    private Double expectedGoalsConcededPer90;

}
