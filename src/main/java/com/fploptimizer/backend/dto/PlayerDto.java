package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/*
    DTO's show the exact formation of the incoming JSON file.
    Used' between the DB and the JSON to take the exact data we need.

 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerDto {
    // --- Identification & Core Info ---
    @JsonProperty("id")
    private int id;

    private int code;
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

    @JsonProperty("penalties_order")
    private Integer penaltiesOrder;

    @JsonProperty("direct_freekicks_order")
    private Integer directFreekicksOrder;

    @JsonProperty("corners_and_indirect_freekicks_order")
    private Integer cornersAndIndirectFreekicksOrder;

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

    @JsonProperty("penalties_saved")
    private Integer penaltiesSaved;
    @JsonProperty("penalties_missed")
    private Integer penaltiesMissed;
    @JsonProperty("clearances_blocks_interceptions")
    private Integer cbi;

    private Integer recoveries;
    private Integer tackles;

    @JsonProperty("defensive_contribution")
    private Integer defensiveContribution;

    @JsonProperty("saves_per_90")
    private Double savesPer90;

    @JsonProperty("goals_conceded_per_90")
    private Double goalsConcededPer90;

    @JsonProperty("defensive_contribution_per_90")
    private Double defensiveContributionPer90;

    @JsonProperty("yellow_cards")
    private Integer yellowCards;

    @JsonProperty("red_cards")
    private Integer redCards;

    // --- Advanced Metrics ---

    @JsonProperty("form")
    private String form;

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

    private String influence;
    private String creativity;
    private String threat;

    @JsonProperty("ict_index")
    private String ictIndex;


    // --- MarketInfo ---
    @JsonProperty("now_cost")
    private Integer currentPrice;

    @JsonProperty("selected_by_percent")
    private String selectedByPercent;

    @JsonProperty("transfers_in")
    private Integer transfersIn;
    @JsonProperty("transfers_out")
    private Integer transfersOut;
    @JsonProperty("transfers_in_event")
    private Integer transfersInGameweek;
    @JsonProperty("transfers_out_event")
    private Integer transfersOutGameweek;

    private String news;

    @JsonProperty("chance_of_playing_this_round")
    private Integer chanceOfPlayingThisRound;
    @JsonProperty("chance_of_playing_next_round")
    private Integer chanceOfPlayingNextRound;

    @JsonProperty("ep_this")
    private String epThis;
    @JsonProperty("ep_next")
    private String epNext;




}
