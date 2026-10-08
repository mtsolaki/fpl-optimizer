package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name ="teams")
@Getter
@Setter
@NoArgsConstructor

public class Team {

    @Id
    private int id;

    private String name;

    private String shortName;

    private int attackHome;
    private int defenseHome;
    private int attackAway;
    private int defenseAway;

    @OneToMany(mappedBy = "team")
    private List<Player> players;

}
