package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/*
    DTO's show the exact formation of the incoming JSON file.
    Used' between the DB and the JSON
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerDto {
    // --- Identification & Core Info ---
    @JsonProperty("id")
    private int id;

    @JsonProperty("web_name")
    private String webName;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("second_name")
    private String lastName;

    @JsonProperty("element_type")
    private Integer position; // 1 = GKP, 2=DEF, 3=MID, 4=FWD
    @JsonProperty("status")
    private String status;
    @JsonProperty("team")
    private Integer teamId;

    // --- Performance Metrics ---
    private Integer minutes;
    private Integer starts;

    @JsonProperty("total_points")
    private Integer totalPoints;

    @JsonProperty("points_per_game")
    private String pointsPerGame;

    @JsonProperty("goals_scored")
    private Integer goalsScored;

    private Integer assists;

    @JsonProperty("clean_sheets")
    private Integer cleanSheets;

    @JsonProperty("goals_conceded")
    private Integer goalsConceded;

    private Integer saves;
    private Integer bonus;
    private Integer bps;
    // --- Advanced Metrics ---

    @JsonProperty("form")
    private Double form;

    @JsonProperty("expected_goals")
    private String expectedGoals;

    @JsonProperty("expected_assists")
    private String expectedAssists;

    @JsonProperty("expected_goal_involvements")
    private String expectedGoalInvolvements;

    @JsonProperty("expected_goals_conceded")
    private String expectedGoalsConceded;

    @JsonProperty("expected_goals_per_90")
    private String expectedGoalsPer90;

    @JsonProperty("expected_assists_per_90")
    private String expectedAssistsPer90;

    @JsonProperty("expected_goal_involvements_per_90")
    private String expectedGoalInvolvementsPer90;

    @JsonProperty("expected_goals_conceded_per_90")
    private String expectedGoalsConcededPer90;


    // --- MarketInfo ---
    @JsonProperty("now_cost")
    private Integer currentPrice;

    @JsonProperty("selected_by_percent")
    private String selectedByPercent;

    private String news;

    @JsonProperty("chance_of_playing_next_round")
    private Integer chanceOfPlayingNextRound;

    @JsonProperty("ep_next")
    private Double epNext;




}
