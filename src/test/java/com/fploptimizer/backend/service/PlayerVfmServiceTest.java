package com.fploptimizer.backend.service;

import com.fploptimizer.backend.dto.advisor.VfmPlayerDto;
import com.fploptimizer.backend.entity.*;
import com.fploptimizer.backend.entity.player.PlayerGameweekProjection;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerVfmServiceTest {

    @Mock
    private PlayerGameweekLogRepository playerGameweekLogRepository;

    @InjectMocks
    private PlayerVfmService playerVfmService;

    private List<PlayerGameweekLog> mockLogs;

    @BeforeEach
    void setUp() {
        mockLogs = new ArrayList<>();

        // 1. Good VFM Defender: £5.0m, 6.0 xP, 82 mins -> VFM = 1.200
        mockLogs.add(buildMockLog(1, 101, "Top DEF", Position.DEF, 5.0, 6.0, 82.0, "a"));

        // 2. Good VFM Midfielder: £6.0m, 6.6 xP, 80 mins -> VFM = 1.100
        mockLogs.add(buildMockLog(2, 102, "Good MID", Position.MID, 6.0, 6.6, 80.0, "a"));

        // 3. Expensive Attacker (low VFM): £15.0m, 7.5 xP, 82 mins -> VFM = 0.500
        mockLogs.add(buildMockLog(3, 103, "Premium FWD", Position.FWD, 15.0, 7.5, 82.0, "a"));

        // 4. Minutes Ban: £4.0m, 5.0 xP
        mockLogs.add(buildMockLog(4, 104, "Sub Player", Position.DEF, 4.0, 5.0, 30.0, "a"));

        // 5. Injury Ban: £4.5m, 5.0 xP, 80 mins but status 'i'
        mockLogs.add(buildMockLog(5, 105, "Injured Player", Position.DEF, 4.5, 5.0, 80.0, "i"));
    }

    private PlayerGameweekLog buildMockLog(int fplId, int code, String name, Position pos,
                                           double price, double xp, double minutes, String status) {
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

        PlayerGameweekProjection projection = new PlayerGameweekProjection();
        projection.setProjectedXp(xp);
        projection.setProjectedMinutes(minutes);

        Gameweek gw = new Gameweek();
        gw.setId(7);

        PlayerGameweekLog log = new PlayerGameweekLog();
        log.setPlayer(player);
        log.setGameweek(gw);
        log.setProjection(projection);

        return log;
    }

    @Test
    @DisplayName("Should filter out injured or low-minute players and rank by VFM score descending")
    void shouldReturnTopVfmPlayersOrderedByScore() {
        when(playerGameweekLogRepository.findAllByGameweekIdWithPlayer(7)).thenReturn(mockLogs);

        List<VfmPlayerDto> result = playerVfmService.getTopVfmPlayers(
                7,
                null,   // All positions
                null,   // Without position restriction
                60.0,   // Min minutes
                10      // Limit
        );

        // Only the 3 valid players
        assertThat(result).hasSize(3);

        // 1st: Top DEF (VFM = 1.2)
        assertThat(result.get(0).webName()).isEqualTo("Top DEF");
        assertThat(result.get(0).vfmScore()).isEqualTo(1.2);

        // 2nd: Good MID (VFM = 1.1)
        assertThat(result.get(1).webName()).isEqualTo("Good MID");
        assertThat(result.get(1).vfmScore()).isEqualTo(1.1);

        // 3rd: Premium FWD (VFM = 0.5)
        assertThat(result.get(2).webName()).isEqualTo("Premium FWD");
        assertThat(result.get(2).vfmScore()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Should correctly apply position and maxPrice filters")
    void shouldFilterByPositionAndMaxPrice() {
        when(playerGameweekLogRepository.findAllByGameweekIdWithPlayer(7)).thenReturn(mockLogs);

        // Filtered only for(MID) with price <= £7.0m
        List<VfmPlayerDto> result = playerVfmService.getTopVfmPlayers(
                7,
                Position.MID,
                7.0,
                60.0,
                10
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).webName()).isEqualTo("Good MID");
        assertThat(result.get(0).position()).isEqualTo(Position.MID);
        assertThat(result.get(0).currentPrice()).isLessThanOrEqualTo(7.0);
    }
}