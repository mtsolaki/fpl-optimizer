package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor

public class FplStaticResponseDto {

    @JsonProperty("teams")
    private List<TeamDto> teams;

    @JsonProperty("elements")
    private List<PlayerDto> players;
}
