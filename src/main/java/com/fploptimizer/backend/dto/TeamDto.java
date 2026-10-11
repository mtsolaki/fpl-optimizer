package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TeamDto {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("short_name")
    private String shortName;

    @JsonProperty("strength_overall_home")
    private Integer strengthOverallHome;

    @JsonProperty("strength_overall_away")
    private Integer strengthOverallAway;
}
