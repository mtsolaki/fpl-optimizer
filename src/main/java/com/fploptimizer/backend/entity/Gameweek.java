package com.fploptimizer.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "gameweeks")
@Getter
@Setter
@NoArgsConstructor
public class Gameweek {

    @Id
    private int id;

    private String name;

    private Instant deadlineTime;

    private Boolean finished;

    private Boolean isCurrent;
    private Boolean isNext;

}
