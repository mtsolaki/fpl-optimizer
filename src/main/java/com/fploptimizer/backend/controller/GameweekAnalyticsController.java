package com.fploptimizer.backend.controller;

import com.fploptimizer.backend.dto.PlayerGameweekReportDto;
import com.fploptimizer.backend.service.GameweekAnalyticsService;
import com.fploptimizer.backend.service.GameweekService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * REST controller providing model evaluation, backtesting, and analytics endpoints.
 *
 * <p>Exposes APIs to compare statistical model projections (expected points, expected minutes)
 * against realized match outcomes (actual points, minutes played) across historical and
 * upcoming Fantasy Premier League (FPL) gameweeks.</p>
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows calls from your React development server
public class GameweekAnalyticsController {

    private final GameweekAnalyticsService analyticsService;
    private final GameweekService gameweekService;

    /**
     * Retrieves aggregated player projections and realized match outcomes for a target gameweek.
     *
     * <p>If the {@code gameweek} request parameter is omitted, the endpoint automatically resolves
     * to the upcoming active gameweek via {@link GameweekService#getNextGameweekId()}. For completed
     * gameweeks, both projections and actual performance data are returned, facilitating
     * model accuracy and error variance analysis.</p>
     *
     * <b>Example Requests:</b>
     * <pre>{@code
     * GET /api/v1/analytics/gameweek?gameweek=5
     * GET /api/v1/analytics/gameweek
     * }</pre>
     *
     * @param gameweek the optional gameweek ID to evaluate; if {@code null}, defaults to the upcoming gameweek
     * @return a {@link ResponseEntity} containing a list of {@link PlayerGameweekReportDto} records
     *         ordered by projected expected points (xP) in descending order
     */
    @GetMapping("/gameweek")
    public ResponseEntity<List<PlayerGameweekReportDto>> getGameweekReport(
            @RequestParam(required = false) Integer gameweek) {

        int targetGw = (gameweek != null) ? gameweek : gameweekService.getNextGameweekId();
        List<PlayerGameweekReportDto> reports = analyticsService.getGameweekPlayerReports(targetGw);

        return ResponseEntity.ok(reports);
    }
}