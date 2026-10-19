package com.fploptimizer.backend.controller;

import com.fploptimizer.backend.dto.advisor.LineupPlayerDto;
import com.fploptimizer.backend.dto.advisor.UserLineupResponseDto;
import com.fploptimizer.backend.dto.advisor.VfmPlayerDto;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.exception.GlobalExceptionHandler;
import com.fploptimizer.backend.service.PlayerVfmService;
import com.fploptimizer.backend.service.UserLineupOptimizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests verifying the REST endpoints of UserTeamAdvisorController using MockMvc in standalone mode.
 *
 * Validates HTTP status code mapping, payload serialization, query parameter handling,
 * and error translation via GlobalExceptionHandler for advisory and VFM recommendations.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class UserTeamAdvisorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserLineupOptimizerService userLineupOptimizerService;

    @Mock
    private PlayerVfmService playerVfmService;

    @InjectMocks
    private UserTeamAdvisorController userTeamAdvisorController;

    /**
     * Sets up the MockMvc instance in standalone mode, registering GlobalExceptionHandler.
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userTeamAdvisorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Verifies that GET /api/v1/advisor/lineup/{teamId} returns HTTP 200 OK
     * and a properly structured UserLineupResponseDto payload.
     *
     * @throws Exception if request dispatch fails
     */
    @Test
    @DisplayName("GET /api/v1/advisor/lineup/{teamId} should return 200 and valid JSON")
    void shouldReturnOptimizedLineup() throws Exception {
        LineupPlayerDto mockCaptain = LineupPlayerDto.builder()
                .fplId(411)
                .webName("Haaland")
                .position(Position.FWD)
                .isCaptain(true)
                .projectedXp(7.5)
                .build();

        UserLineupResponseDto mockResponse = UserLineupResponseDto.builder()
                .teamId(5153618)
                .gameweek(7)
                .formation("4-3-3")
                .totalProjectedXp(56.0)
                .captain(mockCaptain)
                .starters(List.of(mockCaptain))
                .bench(List.of())
                .bank(0.7)
                .teamValue(100.2)
                .build();

        when(userLineupOptimizerService.optimizeUserLineup(5153618, 7)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/advisor/lineup/5153618")
                        .param("gameweek", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(5153618))
                .andExpect(jsonPath("$.formation").value("4-3-3"))
                .andExpect(jsonPath("$.captain.webName").value("Haaland"))
                .andExpect(jsonPath("$.captain.isCaptain").value(true));
    }

    /**
     * Verifies that GET /api/v1/advisor/lineup/{teamId} maps NoSuchElementException
     * to HTTP 404 Not Found via the exception handler.
     *
     * @throws Exception if request dispatch fails
     */
    @Test
    @DisplayName("GET /api/v1/advisor/lineup/{teamId} should return 404 when team is not found")
    void shouldReturn404WhenTeamNotFound() throws Exception {
        when(userLineupOptimizerService.optimizeUserLineup(9999999, 7))
                .thenThrow(new NoSuchElementException("No picks found for teamId 9999999"));

        mockMvc.perform(get("/api/v1/advisor/lineup/9999999")
                        .param("gameweek", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    /**
     * Verifies that GET /api/v1/advisor/vfm returns HTTP 200 OK
     * and a list of top-ranked VfmPlayerDto records.
     *
     * @throws Exception if request dispatch fails
     */
    @Test
    @DisplayName("GET /api/v1/advisor/vfm should return 200 and list of top VFM players")
    void shouldReturnTopVfmPlayers() throws Exception {
        VfmPlayerDto mockVfmPlayer = VfmPlayerDto.builder()
                .fplId(115)
                .webName("De Cuyper")
                .position(Position.DEF)
                .currentPrice(5.1)
                .projectedXp(6.0)
                .vfmScore(1.176)
                .build();

        when(playerVfmService.getTopVfmPlayers(eq(7), any(), any(), anyDouble(), anyInt()))
                .thenReturn(List.of(mockVfmPlayer));

        mockMvc.perform(get("/api/v1/advisor/vfm")
                        .param("gameweek", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].webName").value("De Cuyper"))
                .andExpect(jsonPath("$[0].vfmScore").value(1.176));
    }
}