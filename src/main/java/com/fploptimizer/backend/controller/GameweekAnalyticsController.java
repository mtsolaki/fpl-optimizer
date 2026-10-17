package com.fploptimizer.backend.controller;

import com.fploptimizer.backend.dto.PlayerGameweekReportDto;
import com.fploptimizer.backend.service.GameweekAnalyticsService;
import com.fploptimizer.backend.service.GameweekService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows calls from your React development server
public class GameweekAnalyticsController {

    private final GameweekAnalyticsService analyticsService;
    private final GameweekService gameweekService;

    /**
     * Endpoint to fetch player predictions and realized outcomes for a specified gameweek.
     * If no gameweek parameter is provided, it defaults to the upcoming gameweek.
     *
     * @param gameweek optional gameweek ID
     * @return list of player performance records
     */
    @GetMapping("/gameweek")
    public ResponseEntity<List<PlayerGameweekReportDto>> getGameweekReport(
            @RequestParam(required = false) Integer gameweek) {

        int targetGw = (gameweek != null) ? gameweek : gameweekService.getNextGameweekId();
        List<PlayerGameweekReportDto> reports = analyticsService.getGameweekPlayerReports(targetGw);

        return ResponseEntity.ok(reports);
    }
}