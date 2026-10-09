package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "algorithm_weights",
        uniqueConstraints = {@UniqueConstraint(
        name = "uk_pos_gw_season",
        columnNames = {"position", "gameweek_id", "season"})
        })
@Getter
@Setter
@NoArgsConstructor
public class AlgorithmWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameweek_id", nullable = false)
    private Gameweek gameweek;

    @Column(nullable = false)
    private String season;

    // --- Data
    private Double baseMinutesWeight;
    private Double xgWeight;
    private Double xaWeight;
    private Double cleanSheetWeight;
    private Double formWeight;
    private Double fdrWeight;

    //
    private Double learningRate;

    private LocalDateTime createdAt = LocalDateTime.now();
}