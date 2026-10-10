package com.fploptimizer.backend.entity.player;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PlayerMarketInfo {

    private Double currentPrice;
    private Double selectedByPercent;

    private Integer transfersIn;
    private Integer transfersOut;
    private Integer transfersInGameweek;
    private Integer transfersOutGameweek;

    private Integer chanceOfPlayingThisRound;
    private Integer chanceOfPlayingNextRound;
    private String news;

    private Double epThis;
    private Double epNext;


}
