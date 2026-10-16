package com.fploptimizer.backend.service;

import com.fploptimizer.backend.client.FplApiClient;
import com.fploptimizer.backend.dto.FplUserPicksResponse;
import com.fploptimizer.backend.dto.advisor.UserLineupResponseDto;
import com.fploptimizer.backend.entity.*;
import com.fploptimizer.backend.entity.player.PlayerGameweekProjection;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLineupOptimizerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerGameweekLogRepository playerGameweekLogRepository;

    @Mock
    private FplApiClient fplApiClient;

    @InjectMocks
    private UserLineupOptimizerService lineupOptimizerService;

    private List<FplUserPicksResponse.PickItem> mockPicks;
    private List<Player> mockPlayers;
    private List<PlayerGameweekLog> mockLogs;

    @BeforeEach
    void setUp() {
        mockPicks = new ArrayList<>();
        mockPlayers = new ArrayList<>();
        mockLogs = new ArrayList<>();

        // 2 GKP
        createMockPlayer(1, 101, "GKP 1", Position.GKP, 5.0, 4.0, 82.0, "a");
        createMockPlayer(2, 102, "GKP 2", Position.GKP, 4.0, 1.0, 0.0, "a");

        // 5 DEF (xP: 5.0, 4.0, 3.0, 2.0, 1.0)
        for (int i = 1; i <= 5; i++) {
            createMockPlayer(2 + i, 200 + i, "DEF " + i, Position.DEF, 5.0, 6.0 - i, 80.0, "a");
        }

        // 5 MID (xP: 6.0, 5.0, 4.0, 3.0, 2.0)
        for (int i = 1; i <= 5; i++) {
            createMockPlayer(7 + i, 300 + i, "MID " + i, Position.MID, 7.0, 7.0 - i, 80.0, "a");
        }

        // 3 FWD
        // FWD High-Risk: High xP (9.0) but low minutes played (45.0) and status 'd' (αμφίβολος)
        createMockPlayer(13, 401, "FWD High-Risk", Position.FWD, 12.0, 9.0, 45.0, "d");
        // FWD Safe:  8.0 xP, 82 minutes and status 'a' (should be Captain due to Safety Lock)
        createMockPlayer(14, 402, "FWD Safe", Position.FWD, 8.0, 8.0, 82.0, "a");
        // FWD Bench
        createMockPlayer(15, 403, "FWD Bench", Position.FWD, 5.0, 2.0, 60.0, "a");
    }

    private void createMockPlayer(int fplId, int code, String name, Position pos,
                                  double price, double xp, double minutes, String status) {
        mockPicks.add(new FplUserPicksResponse.PickItem(fplId, mockPicks.size() + 1, 1, false, false));

        PlayerMarketInfo marketInfo = new PlayerMarketInfo();
        marketInfo.setCurrentPrice(price);
        marketInfo.setNews("");

        Team team = new Team();
        team.setShortName("TEST");

        Player player = new Player();
        player.setId(fplId);
        player.setCode(code);
        player.setWebName(name);
        player.setPosition(pos);
        player.setStatus(status);
        player.setMarketInfo(marketInfo);
        player.setTeam(team);
        mockPlayers.add(player);

        PlayerGameweekProjection projection = new PlayerGameweekProjection();
        projection.setProjectedXp(xp);
        projection.setProjectedMinutes(minutes);


        Gameweek gw = new Gameweek();
        gw.setId(7);

        PlayerGameweekLog log = new PlayerGameweekLog();
        log.setPlayer(player);
        log.setGameweek(gw);
        log.setProjection(projection);
        mockLogs.add(log);
    }

    @Test
    @DisplayName("Should select optimal lineup, sort bench correctly, and enforce captaincy safety lock")
    void shouldOptimizeLineupWithSafetyLock() {
        FplUserPicksResponse.EntryHistory history = new FplUserPicksResponse.EntryHistory(
                1,      // Integer
                0,      // Integer
                0,      // Integer
                1000L,  // Long
                5L,     // Long
                0,      // Integer
                0       // Integer
        );

        FplUserPicksResponse picksResponse = new FplUserPicksResponse(history, mockPicks);

        when(fplApiClient.getUserPicks(eq(123), eq(7))).thenReturn(Optional.of(picksResponse));
        when(playerRepository.findByFplIdIn(anyList())).thenReturn(mockPlayers);
        when(playerGameweekLogRepository.findByPlayerCodeInAndGameweekId(anyList(), eq(7))).thenReturn(mockLogs);

        UserLineupResponseDto result = lineupOptimizerService.optimizeUserLineup(123, 7);

        assertThat(result).isNotNull();
        assertThat(result.starters()).hasSize(11);
        assertThat(result.bench()).hasSize(4);

        //  Captaincy Safety Lock:

        assertThat(result.captain()).isNotNull();
        assertThat(result.captain().webName()).isEqualTo("FWD Safe");
        assertThat(result.captain().isCaptain()).isTrue();

        // Confirmation Vice-Captain:

        assertThat(result.viceCaptain()).isNotNull();
        assertThat(result.viceCaptain().webName()).isEqualTo("MID 1");
        assertThat(result.viceCaptain().isViceCaptain()).isTrue();

        // Bench Confirmation:
        assertThat(result.bench().get(0).position()).isEqualTo(Position.GKP);
        assertThat(result.bench().get(0).webName()).isEqualTo("GKP 2");
        assertThat(result.bench().get(0).benchOrder()).isEqualTo(0);

        // Slots 1-3: Ordered by decreasing xP.
        assertThat(result.bench().get(1).benchOrder()).isEqualTo(1);
        assertThat(result.bench().get(2).benchOrder()).isEqualTo(2);
        assertThat(result.bench().get(3).benchOrder()).isEqualTo(3);
        assertThat(result.bench().get(1).projectedXp()).isGreaterThanOrEqualTo(result.bench().get(2).projectedXp());
        assertThat(result.bench().get(2).projectedXp()).isGreaterThanOrEqualTo(result.bench().get(3).projectedXp());
    }
}