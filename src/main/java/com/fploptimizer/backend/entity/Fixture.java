package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table (name = "fixtures")
@Getter
@Setter
@NoArgsConstructor
public class Fixture {

    @Id
    private int  id;

    private Integer gameweek;
    private LocalDateTime dateTime;

    private Integer homeTeamDifficulty;
    private Integer awayTeamDifficulty;

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;
}
