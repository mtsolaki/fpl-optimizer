package com.fploptimizer.backend.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/*
    This class corresponds to PostgreSQL table "Fixtures"
 */
@Entity
@Table (name = "fixtures")
@Getter
@Setter
@NoArgsConstructor
public class Fixture {

    @Id // Primary key of the table. Taken directly from fpl API - NOT a generated value
    private int  id;

    private Integer gameweek;
    private LocalDateTime kickoffTime;

    private Integer homeTeamDifficulty;
    private Integer awayTeamDifficulty;

    /*
    Connection of two teams to the same match
     */

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;


    private Boolean finished;

    private Boolean started;

    private Integer homeTeamScore;

    private Integer awayTeamScore;
}
