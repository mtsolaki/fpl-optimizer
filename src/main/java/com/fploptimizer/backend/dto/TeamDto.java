package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class TeamDto {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("short_name")
    private String shortName;

    @JsonProperty("strengh_attack_home")
    private int attackHome;
    @JsonProperty("strengh_defence_home")
    private int defenseHome;
    @JsonProperty("strengh_attack_away")
    private int attackAway;
    @JsonProperty("strengh_defence_away")
    private int defenseAway;
}
