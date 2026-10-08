package com.fploptimizer.backend.entity;

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

    private String firstName;
    private String lastName;
    private Integer position;

    private Integer currentPrice;

    private Double form;
    private Double epNext;
    private Double expectedGoals;
    private Double expectedAssists;

    private String status;

    private Integer chanceOfPlayingNextRound;

    @ManyToOne // Many players correspond to one team (foreign key)
    @JoinColumn(name = "team_id")
    private Team team;
}
