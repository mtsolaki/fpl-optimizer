package com.fploptimizer.backend.service.sync;

import com.fploptimizer.backend.dto.live.LiveElementDto;
import com.fploptimizer.backend.dto.live.LiveExplainDto;
import com.fploptimizer.backend.dto.live.LiveGameweekResponseDto;
import com.fploptimizer.backend.dto.live.LiveStatsDto;
import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.GameweekRepository;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveGameweekSyncServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PlayerGameweekLogRepository logRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private FixtureRepository fixtureRepository;

    @Mock
    private GameweekRepository gameweekRepository;

    @InjectMocks
    private LiveGameweekSyncService liveGameweekSyncService;

    private Gameweek mockGameweek;
    private Player mockPlayer1;
    private Player mockPlayer2;
    private Fixture mockFixture;

    @BeforeEach
    void setUp() {
        mockGameweek = new Gameweek();
        mockGameweek.setId(7);

        Team homeTeam = new Team();
        homeTeam.setId(1);
        homeTeam.setName("Arsenal");

        Team awayTeam = new Team();
        awayTeam.setId(2);
        awayTeam.setName("Chelsea");

        mockFixture = new Fixture();
        mockFixture.setId(100);
        mockFixture.setGameweek(7);
        mockFixture.setHomeTeam(homeTeam);
        mockFixture.setAwayTeam(awayTeam);

        mockPlayer1 = new Player();
        mockPlayer1.setId(10);
        mockPlayer1.setCode(1000);
        mockPlayer1.setWebName("Player 1");
        mockPlayer1.setTeam(homeTeam);

        mockPlayer2 = new Player();
        mockPlayer2.setId(20);
        mockPlayer2.setCode(2000);
        mockPlayer2.setWebName("Player 2");
        mockPlayer2.setTeam(awayTeam);
    }

    @Test
    @DisplayName("Should sync gameweek using batch preloading without executing N+1 queries")
    void shouldSyncGameweekInBatchesWithoutNPlusOneQueries() {
        // Arrange
        int gwId = 7;
        String season = "2026-2027";

        LiveElementDto element1 = new LiveElementDto();
        element1.setId(10);
        LiveExplainDto explain1 = new LiveExplainDto();
        explain1.setFixtureId(100);
        element1.setExplain(List.of(explain1));
        LiveStatsDto stats1 = new LiveStatsDto();
        stats1.setTotalPoints(8);
        stats1.setMinutes(90);
        stats1.setStarts(1);
        element1.setStats(stats1);

        LiveElementDto element2 = new LiveElementDto();
        element2.setId(20);
        LiveExplainDto explain2 = new LiveExplainDto();
        explain2.setFixtureId(100);
        element2.setExplain(List.of(explain2));
        LiveStatsDto stats2 = new LiveStatsDto();
        stats2.setTotalPoints(2);
        stats2.setMinutes(60);
        stats2.setStarts(1);
        element2.setStats(stats2);

        LiveGameweekResponseDto apiResponse = new LiveGameweekResponseDto();
        apiResponse.setElements(List.of(element1, element2));

        when(restTemplate.getForObject(anyString(), eq(LiveGameweekResponseDto.class))).thenReturn(apiResponse);
        when(gameweekRepository.findById(gwId)).thenReturn(Optional.of(mockGameweek));

        // Existing log for Player 1
        PlayerGameweekLog existingLog1 = new PlayerGameweekLog();
        existingLog1.setPlayer(mockPlayer1);
        existingLog1.setFixture(mockFixture);
        existingLog1.setGameweek(mockGameweek);
        existingLog1.setSeason(season);

        when(logRepository.findAllByGameweekIdAndSeason(gwId, season)).thenReturn(new ArrayList<>(List.of(existingLog1)));
        when(playerRepository.findAll()).thenReturn(List.of(mockPlayer1, mockPlayer2));
        when(fixtureRepository.findByGameweek(gwId)).thenReturn(List.of(mockFixture));

        // Act
        liveGameweekSyncService.syncEntireGameweek(gwId, season);

        // Assert
        // Verify batch queries executed exactly ONCE
        verify(logRepository, times(1)).findAllByGameweekIdAndSeason(gwId, season);
        verify(playerRepository, times(1)).findAll();
        verify(fixtureRepository, times(1)).findByGameweek(gwId);

        // Verify NO individual loop queries were made (0 times)
        verify(logRepository, never()).findByFplPlayerIdAndFixtureIdAndSeason(anyInt(), anyInt(), anyString());
        verify(playerRepository, never()).findByFplId(anyInt());
        verify(fixtureRepository, never()).findById(anyInt());

        // Verify batch save was called with both players
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlayerGameweekLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(logRepository, times(1)).saveAll(captor.capture());

        List<PlayerGameweekLog> savedLogs = captor.getValue();
        assertThat(savedLogs).hasSize(2);

        // Player 1 had existing log updated
        assertThat(savedLogs.get(0).getPlayer().getId()).isEqualTo(10);
        assertThat(savedLogs.get(0).getOutcome().getActualTotalPoints()).isEqualTo(8);

        // Player 2 had new log created and populated
        assertThat(savedLogs.get(1).getPlayer().getId()).isEqualTo(20);
        assertThat(savedLogs.get(1).getOutcome().getActualTotalPoints()).isEqualTo(2);
        assertThat(savedLogs.get(1).getIsHome()).isFalse(); // Player 2 is away
    }
}
