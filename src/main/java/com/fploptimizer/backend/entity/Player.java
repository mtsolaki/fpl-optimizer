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

    @Id // Primary key of the table. Taken directly from fpl API - NOT a generated value
    private int id;

    private String webName;
    private String firstName;
    private String lastName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;
    private String status;

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
