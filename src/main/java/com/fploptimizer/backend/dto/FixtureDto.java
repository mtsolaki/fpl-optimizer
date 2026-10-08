package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/*
    DTO's show the exact formation of the incoming JSON file.
    Used' between the DB and the JSON
 */
@Getter
@Setter
@NoArgsConstructor
public class FixtureDto {
    
    @JsonProperty("id")
    private int id;
    
    @JsonProperty("event")
    private Integer gameweek;

    @JsonProperty("kickoff_time")
    private LocalDateTime kickoffTime;

    @JsonProperty("team_h")
    private Integer homeTeamId;

    @JsonProperty("team_a")
    private Integer awayTeamId;

    @JsonProperty("team_h_difficulty")
    private Integer homeTeamDifficulty;

    @JsonProperty("team_a_difficulty")
    private Integer awayTeamDifficulty;

    @JsonProperty("finished")
    private Boolean finished;

    @JsonProperty("started")
    private Boolean started;

    @JsonProperty("team_h_score")
    private Integer homeTeamScore;

    @JsonProperty("team_a_score")
    private Integer awayTeamScore;
}
