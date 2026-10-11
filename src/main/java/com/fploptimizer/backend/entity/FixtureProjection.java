package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.*;
/*
    Corresponds to postgres table  "fixture_projections"
 */
@Entity
@Table(name = "fixture_projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixtureProjection {

    @Id
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "fixture_id")
    private Fixture fixture;

    private Integer gameweek;

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;


    // === Home team metrics ===
    private Double homeBaseAttack;
    private Double homeBaseDefense;
    private Double homeAttackDegradation;
    private Double homeDefenseDegradation;
    private Double homeMotivation;
    private Double homeFinalAttack;
    private Double homeFinalDefense;
    private Double homeLambda;
    private Double homeCleanSheetProb;

    // === Away team metrics ===
    private Double awayBaseAttack;
    private Double awayBaseDefense;
    private Double awayAttackDegradation;
    private Double awayDefenseDegradation;
    private Double awayMotivation;
    private Double awayFinalAttack;
    private Double awayFinalDefense;
    private Double awayLambda;
    private Double awayCleanSheetProb;

    // === Post-Match Evaluation ===
    private Double homeGoalsDelta;
    private Double awayGoalsDelta;
    private Boolean homeCsHit;
    private Boolean awayCsHit;




}
