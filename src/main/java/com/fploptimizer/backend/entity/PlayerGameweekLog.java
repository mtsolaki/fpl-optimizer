package com.fploptimizer.backend.entity;

import com.fploptimizer.backend.entity.player.PlayerGameweekOutcome;
import com.fploptimizer.backend.entity.player.PlayerGameweekProjection;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
    Postgres Table "player_gameweek_logs"

    Used to store the snapshot for each fixture.
    What was going on,
    what we predicted,
    what actually happened
 */
@Entity
@Table(name = "player_gameweek_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_fixture_season",
                columnNames = {"player_code", "fixture_id", "season"})
})
@Getter
@Setter
@NoArgsConstructor
public class PlayerGameweekLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Connect with Universal Player Code
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_code", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameweek_id", nullable = false)
    private Gameweek gameweek;


    @Column(nullable = false)
    private String season; // π.χ. "2026-2027"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_team_id")
    private Team opponentTeam;

    private Boolean isHome;
    private Double formAtGw;
    private Integer fixtureDifficulty;
    private String status;

    private Integer penaltiesOrder;
    private Integer directFreeKicksOrder;
    private Integer cornersAndIndirectFreeKicksOrder;


    // 1. Snapshot αγοράς / τιμής / διαθεσιμότητας πριν το ματς
    @Embedded
    private PlayerMarketInfo marketInfo = new PlayerMarketInfo();

    // 2. Expected Metrics (Υπολογισμένα πριν το ματς)
    @Embedded
    private PlayerGameweekProjection projection = new PlayerGameweekProjection();

    // 3. Actual Outcomes (Αποτελέσματα μετά το ματς)
    @Embedded
    private PlayerGameweekOutcome outcome = new PlayerGameweekOutcome();
}