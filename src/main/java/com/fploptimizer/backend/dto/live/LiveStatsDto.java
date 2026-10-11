package com.fploptimizer.backend.dto.live;

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
public class LiveStatsDto {

    private Integer minutes;
    private Integer starts;

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

    @JsonProperty("defensive_contribution")
    private Integer defensiveContribution;

    @JsonProperty("yellow_cards")
    private Integer yellowCards;

    @JsonProperty("red_cards")
    private Integer redCards;

    @JsonProperty("penalties_missed")
    private Integer penaltiesMissed;

    @JsonProperty("penalties_saved")
    private Integer penaltiesSaved;

    @JsonProperty("expected_goals")
    private String expectedGoals;

    @JsonProperty("expected_assists")
    private String expectedAssists;

    @JsonProperty("expected_goals_conceded")
    private String expectedGoalsConceded;

    @JsonProperty("total_points")
    private Integer totalPoints;
}