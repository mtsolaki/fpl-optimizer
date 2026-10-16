package com.fploptimizer.backend.dto.advisor;

import lombok.Builder;
import java.util.List;

@Builder
public record UserLineupResponseDto(
        Integer teamId,
        Integer gameweek,
        String formation,                // e.g. "3-5-2", "3-4-3"
        Double totalProjectedXp,         // sum of the 11 starters plus double points for captain
        LineupPlayerDto captain,
        LineupPlayerDto viceCaptain,
        List<LineupPlayerDto> starters,  // exactly 11 players ordered by:(GKP -> DEF -> MID -> FWD)
        List<LineupPlayerDto> bench,     // exactly 4 players in order priority
        Double bank,                     // available bank money in £m
        Double teamValue
) {}