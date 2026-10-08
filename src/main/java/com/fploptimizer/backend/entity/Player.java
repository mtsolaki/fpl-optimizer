package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor

public class Player {

    @Id
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

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}
