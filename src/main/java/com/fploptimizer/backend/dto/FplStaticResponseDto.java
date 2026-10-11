package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/*
    DTO's show the exact formation of the incoming JSON file.
    Used' between the DB and the JSON
 */
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
/*
    DTO's show the exact formation of the incoming JSON file.
    Used' between the DB and the JSON
 */
public class FplStaticResponseDto {

    @JsonProperty("teams")
    private List<TeamDto> teams = new ArrayList<>();

    @JsonProperty("elements")
    private List<PlayerDto> players = new ArrayList<>();

    @JsonProperty("events")
    private List<GameweekDto> gameweeks = new ArrayList<>();
}
