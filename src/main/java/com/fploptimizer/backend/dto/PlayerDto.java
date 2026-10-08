package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlayerDto {

    @JsonProperty("id")
    private int id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("second_name")
    private String lastName;

    @JsonProperty("element_type")
    private Integer position;


    @JsonProperty("now_cost")
    private Integer currentPrice;

    @JsonProperty("form")
    private Double form;
    @JsonProperty("ep_next")
    private Double epNext;

    @JsonProperty("expected_goals")
    private Double expectedGoals;
    @JsonProperty("expected_assists")
    private Double expectedAssists;


    @JsonProperty("chance_of_playing_next_round")
    private Integer chanceOfPlayingNextRound;

    @JsonProperty("status")
    private String status;

    @JsonProperty("team")
    private Integer teamId;




}
