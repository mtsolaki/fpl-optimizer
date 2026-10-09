package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "algorithm_weights")
@Getter
@Setter
@NoArgsConstructor
public class AlgorithmWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String position; // "GKP", "DEF", "MID", "FWD"

    //
    private Double baseMinutesWeight;
    private Double xgWeight;
    private Double xaWeight;
    private Double cleanSheetWeight;
    private Double formWeight;
    private Double fdrWeight;

    //
    private Double learningRate;
}