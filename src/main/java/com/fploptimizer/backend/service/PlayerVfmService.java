package com.fploptimizer.backend.service;

import com.fploptimizer.backend.dto.advisor.VfmPlayerDto;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerVfmService {

    private final PlayerGameweekLogRepository playerGameweekLogRepository;

    public List<VfmPlayerDto> getTopVfmPlayers(
            int gameweek,
            Position position,
            Double maxPrice,
            double minMinutes,
            int limit
    ) {
        List<PlayerGameweekLog> logs = playerGameweekLogRepository.findAllByGameweekIdWithPlayer(gameweek);

        return logs.stream()
                // 1. Basic checks for projection and price
                .filter(logItem -> logItem.getProjection() != null
                        && logItem.getProjection().getProjectedXp() != null
                        && logItem.getProjection().getProjectedMinutes() != null)
                .filter(logItem -> {
                    Player p = logItem.getPlayer();
                    return p != null
                            && p.getMarketInfo() != null
                            && p.getMarketInfo().getCurrentPrice() != null
                            && p.getMarketInfo().getCurrentPrice() > 0.0;
                })
                // 2. Integrity filters (status & minutes)
                .filter(logItem -> {
                    Player p = logItem.getPlayer();
                    String status = p.getStatus() != null ? p.getStatus() : "a";
                    return "a".equalsIgnoreCase(status)
                            && logItem.getProjection().getProjectedMinutes() >= minMinutes;
                })
                // 3. Optional position filter
                .filter(logItem -> position == null || logItem.getPlayer().getPosition() == position)
                // 4. Optional max budget filter
                .filter(logItem -> maxPrice == null || logItem.getPlayer().getMarketInfo().getCurrentPrice() <= maxPrice)
                // 5. Mapping to VfmPlayerDto and Score Calculation
                .map(this::mapToVfmDto)
                // 6. Sort by decreasing VFM Score
                .sorted(Comparator.comparingDouble(VfmPlayerDto::vfmScore).reversed())
                .limit(limit)
                .toList();
    }

    private VfmPlayerDto mapToVfmDto(PlayerGameweekLog logItem) {
        Player p = logItem.getPlayer();
        double price = p.getMarketInfo().getCurrentPrice();
        double xp = logItem.getProjection().getProjectedXp();
        double minutes = logItem.getProjection().getProjectedMinutes();
        double vfm = Math.round((xp / price) * 1000.0) / 1000.0; // 3 δεκαδικά ψηφία

        return VfmPlayerDto.builder()
                .playerCode(p.getCode())
                .fplId(p.getId())
                .webName(p.getWebName())
                .teamShortName(p.getTeam() != null ? p.getTeam().getShortName() : null)
                .position(p.getPosition())
                .currentPrice(price)
                .projectedXp(xp)
                .projectedMinutes(minutes)
                .vfmScore(vfm)
                .status(p.getStatus())
                .news(p.getMarketInfo().getNews())
                .build();
    }
}
