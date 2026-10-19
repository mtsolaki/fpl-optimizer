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
/**
 * REST controller exposing decision-support and advisory endpoints for Fantasy Premier League (FPL) managers.
 *
 * <p>Provides endpoints to calculate mathematically optimized starting lineups, formation selections,
 * bench prioritization, captaincy recommendations, and Value-For-Money (VFM) player rankings.</p>
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
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
     * Resolves the optimal starting eleven, formation structure, substitute bench hierarchy,
     * and captain/vice-captain designations for a specified FPL team and gameweek.
     *
     * <p>If no target gameweek is explicitly provided in the request parameters, the service
     * automatically defaults to the upcoming active gameweek.</p>
     *
     * <b>Example Request:</b>
     * <pre>{@code
     * GET /api/v1/advisor/lineup/123456?gameweek=7
     * }</pre>
     *
     * @param teamId   the unique FPL entry identifier of the manager squad
     * @param gameweek the target gameweek ID; if {@code null}, resolves to the upcoming gameweek
     * @return a {@link ResponseEntity} containing the {@link UserLineupResponseDto} with the recommended squad lineup
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
     * Retrieves the top-performing players ranked by Value For Money (VFM), measured as expected points
     * generated per million cost (Pts/£M).
     *
     * <p>Results can be filtered by target gameweek, outfield pitch position, maximum market price,
     * and minimum expected playing minutes threshold.</p>
     *
     * <b>Example Requests:</b>
     * <pre>{@code
     * GET /api/v1/advisor/vfm?gameweek=7
     * GET /api/v1/advisor/vfm?gameweek=7&position=MID&maxPrice=7.5&minMinutes=60.0&limit=10
     * }</pre>
     *
     * @param gameweek   the target gameweek ID; if {@code null}, defaults to the upcoming gameweek
     * @param position   the optional pitch position filter (e.g., {@code GKP}, {@code DEF}, {@code MID}, {@code FWD})
     * @param maxPrice   the maximum market price ceiling in millions (e.g., {@code 7.5}); {@code null} disables price capping
     * @param minMinutes the minimum projected minutes threshold required for inclusion (defaults to {@code 60.0})
     * @param limit      the maximum number of top-ranked players to return (defaults to {@code 20})
     * @return a {@link ResponseEntity} containing a list of {@link VfmPlayerDto} ordered by descending VFM efficiency score
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
