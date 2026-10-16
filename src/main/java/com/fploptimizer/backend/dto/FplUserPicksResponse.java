package com.fploptimizer.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FplUserPicksResponse(
        @JsonProperty("entry_history")
        EntryHistory entryHistory,

        @JsonProperty("picks")
        List<PickItem> picks
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EntryHistory(
            @JsonProperty("event") Integer event,
            @JsonProperty("points") Integer points,
            @JsonProperty("total_points") Integer totalPoints,
            @JsonProperty("bank") Long bankTenths,      // Fpl in tenths (π.χ. 12 = £1.2m)
            @JsonProperty("value") Long valueTenths,    // e.g. 1005 = £100.5m
            @JsonProperty("event_transfers") Integer eventTransfers,
            @JsonProperty("event_transfers_cost") Integer eventTransfersCost
    ) {
        public Double getBankInMillions() {
            return bankTenths != null ? bankTenths / 10.0 : 0.0;
        }

        public Double getValueInMillions() {
            return valueTenths != null ? valueTenths / 10.0 : 100.0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PickItem(
            @JsonProperty("element") Integer elementId, // FPL player ID
            @JsonProperty("position") Integer position, // 1 to 15 in roster
            @JsonProperty("multiplier") Integer multiplier,
            @JsonProperty("is_captain") Boolean isCaptain,
            @JsonProperty("is_vice_captain") Boolean isViceCaptain
    ) {}
}