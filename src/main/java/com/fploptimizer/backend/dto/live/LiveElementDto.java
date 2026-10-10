package com.fploptimizer.backend.dto.live;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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