package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/*
    DTO's show the exact formation of the incoming JSON file.
    Used' between the DB and the JSON
 */
@Getter
@Setter
@NoArgsConstructor@JsonIgnoreProperties(ignoreUnknown = true)

public class GameweekDto {

    @JsonProperty("id")
    private int id;

    private String name;

    @JsonProperty("deadline_time")
    private Instant deadlineTime;

    private Boolean finished;

    @JsonProperty("is_current")
    private Boolean isCurrent;

    @JsonProperty("is_next")
    private Boolean isNext;
}
