package com.fploptimizer.backend.controller;

import com.fploptimizer.backend.dto.advisor.UserLineupResponseDto;
import com.fploptimizer.backend.dto.advisor.VfmPlayerDto;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.service.GameweekService;
import com.fploptimizer.backend.service.PlayerVfmService;
import com.fploptimizer.backend.service.UserLineupOptimizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserTeamAdvisorController {


    private final UserLineupOptimizerService userLineupOptimizerService;
    private final PlayerVfmService playerVfmService;
    private final GameweekService gameweekService;
    /**
     * Returns best eleven, formation, bench and captains
     * for a specific fpl team and gameweek
     *
     * Call example:
     * GET /api/v1/advisor/lineup/123456?gameweek=7
     */
    @GetMapping("/lineup/{teamId}")
    public ResponseEntity<UserLineupResponseDto> getOptimizedLineup(
            @PathVariable int teamId,
            @RequestParam(required = false) Integer gameweek
    ) {
        int targetGw = (gameweek != null) ? gameweek : gameweekService.getNextGameweekId();

        log.info("Received lineup optimization request for teamId: {}, gameweek: {}", teamId, targetGw);
        UserLineupResponseDto response = userLineupOptimizerService.optimizeUserLineup(teamId, targetGw);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns most vfm players per million cost.
     *
     * Examples:
     * GET /api/v1/advisor/vfm?gameweek=7
     * GET /api/v1/advisor/vfm?gameweek=7&position=MID&maxPrice=7.5&limit=10
     */
    @GetMapping("/vfm")
    public ResponseEntity<List<VfmPlayerDto>> getTopVfmPlayers(
            @RequestParam(required = false) Integer gameweek,
            @RequestParam(required = false) Position position,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "60.0") double minMinutes,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int targetGw = (gameweek != null) ? gameweek : gameweekService.getNextGameweekId();
        log.info("Fetching VFM players for GW {}, position: {}, maxPrice: {}", targetGw, position, maxPrice);
        List<VfmPlayerDto> result = playerVfmService.getTopVfmPlayers(targetGw, position, maxPrice, minMinutes, limit);
        return ResponseEntity.ok(result);
    }
}
