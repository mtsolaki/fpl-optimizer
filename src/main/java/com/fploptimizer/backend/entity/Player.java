package com.fploptimizer.backend.entity;

import com.fploptimizer.backend.entity.player.PlayerAdvancedMetrics;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.entity.player.PlayerPerformance;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
    This class corresponds to PostgreSQL table "Players"
 */

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor

public class Player {

    @Column(unique = true, nullable = false)
    private int id;

    @Id // Primary key of the table. Taken directly from fpl API - NOT a generated value
    private int code;


    private String webName;
    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    private String status;

    /*
        Used to see if a player is in order for FK.
        1. First to execute
        2. Second
        ... etc ...

     */
    private Integer penaltiesOrder;
    private Integer directFreeKicksOrder;
    private Integer cornersAndIndirectFreeKicksOrder;

    /*
    Εmbedded entities for usability
     */
    @Embedded
    private PlayerPerformance performance = new PlayerPerformance();
    @Embedded
    private PlayerAdvancedMetrics  advancedMetrics = new PlayerAdvancedMetrics();
    @Embedded
    private PlayerMarketInfo marketInfo = new PlayerMarketInfo();



    @ManyToOne // Many players correspond to one team (foreign key)
    @JoinColumn(name = "team_id")
    private Team team;
}
