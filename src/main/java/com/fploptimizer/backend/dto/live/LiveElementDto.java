package com.fploptimizer.backend.dto.live;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
/*
    DTO's show the exact formation of the incoming JSON file.
    Used' between the DB and the JSON
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveElementDto {

    @JsonProperty("id")
    private int id; // Seasonal player ID

    @JsonProperty("stats")
    private LiveStatsDto stats;

    @JsonProperty("explain")
    private List<LiveExplainDto> explain;
}