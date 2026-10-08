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

}
