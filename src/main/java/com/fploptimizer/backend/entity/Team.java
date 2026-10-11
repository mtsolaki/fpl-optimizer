package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
/*
    This class corresponds to PostgreSQL table "Teams"
 */
@Entity
@Table(name ="teams")
@Getter
@Setter
@NoArgsConstructor

public class Team {

    @Id // Primary key of the table. Taken directly from fpl API - NOT a generated value
    private int id;

    private String name;

    private String shortName;

    /*

     */
    private Integer strengthOverallHome;
    private Integer strengthOverallAway;


    @OneToMany(mappedBy = "team") // One team has many players (not an extra column to the table)
    private List<Player> players;

}
