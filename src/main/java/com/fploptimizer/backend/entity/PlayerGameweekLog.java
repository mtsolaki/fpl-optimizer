package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_gameweek_logs",  uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_gw_season",
        columnNames = {"player_id","gameweek_id","season"})
})
@Getter
@Setter
@NoArgsConstructor
public class PlayerGameweekLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameweek_id", nullable = false)
    private Gameweek gameweek;

    @Column(nullable = false)
    private String season; // e.g. 2026-2027

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_team_id")
    private Team opponentTeam;

    private Boolean isHome;

    // --- Pre-Match Predictions  ---
    private Double projectedMinutes;
    private Double projectedXg;
    private Double projectedXa;
    private Double projectedCleanSheetProb;
    private Double projectedXp;

    private Double formAtGw;
    private Integer fixtureDifficulty;

    // --- Post-Match Outcomes  ---
    private Integer actualMinutes;
    private Integer actualGoals;
    private Integer actualAssists;
    private Integer actualCleanSheets;
    private Integer actualGoalsConceded;
    private Integer actualSaves;
    private Integer actualBonus;
    private Integer actualTotalPoints;

    private Double predictionDelta; // actualTotalPoints - projectedXp
}
