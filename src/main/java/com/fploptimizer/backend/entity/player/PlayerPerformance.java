package com.fploptimizer.backend.entity.player;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PlayerPerformance {

    private Integer minutes;
    private Integer starts;
    private Integer totalPoints;
    private Double  pointsPerGame;

    private Integer goalsScored;
    private Integer assists;
    private Integer cleanSheets;
    private Integer goalsConceded;
    private Integer saves;
    private Integer bonus;
    private Integer bps;

    private Integer penaltiesSaved;
    private Integer penaltiesMissed;

    private Integer cbi;
    private Integer recoveries;
    private Integer tackles;
    private Integer defensiveContribution;

    private Double savesPer90;
    private Double goalsConcededPer90;
    private Double defensiveContributionPer90;



}
