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

/**
 * Service calculating Value-For-Money (VFM) efficiency metrics across registered FPL players.
 * Ranks market options based on points generated per unit of investment (projected xP / cost in £M),
 * applying safety and playing-time filters to isolate budget enablers and high-efficiency differentials.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerVfmService {

    private final PlayerGameweekLogRepository playerGameweekLogRepository;

    /**
     * Resolves the top-ranked players by Value For Money (VFM) efficiency for a given gameweek.
     * Filters out injured, suspended, and rotation-risk players below the playing minutes threshold,
     * applies optional position and price ceiling constraints, and returns results sorted by descending VFM ratio.
     *
     * @param gameweek   target gameweek identifier
     * @param position   optional tactical position filter (null allows all positions)
     * @param maxPrice   optional maximum price ceiling in millions (null disables price ceiling)
     * @param minMinutes minimum expected playing minutes required for qualification
     * @param limit      maximum number of top players to return
     * @return ordered list of top VfmPlayerDto candidates
     */
    public List<VfmPlayerDto> getTopVfmPlayers(
            int gameweek,
            Position position,
            Double maxPrice,
            double minMinutes,
            int limit
    ) {
        List<PlayerGameweekLog> logs = playerGameweekLogRepository.findAllByGameweekIdWithPlayer(gameweek);

        return logs.stream()
                // 1. Basic checks for projection and price availability
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
                // 2. Health integrity and playing-time filters
                .filter(logItem -> {
                    Player p = logItem.getPlayer();
                    String status = p.getStatus() != null ? p.getStatus() : "a";
                    return "a".equalsIgnoreCase(status)
                            && logItem.getProjection().getProjectedMinutes() >= minMinutes;
                })
                // 3. Optional position filter
                .filter(logItem -> position == null || logItem.getPlayer().getPosition() == position)
                // 4. Optional price ceiling filter
                .filter(logItem -> maxPrice == null || logItem.getPlayer().getMarketInfo().getCurrentPrice() <= maxPrice)
                // 5. Map to DTO and compute efficiency ratio
                .map(this::mapToVfmDto)
                // 6. Sort by descending VFM score
                .sorted(Comparator.comparingDouble(VfmPlayerDto::vfmScore).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Maps a PlayerGameweekLog to a VfmPlayerDto, computing the VFM score rounded to 3 decimal places.
     *
     * @param logItem the source log item containing player projection and market info
     * @return mapped VfmPlayerDto record
     */
    private VfmPlayerDto mapToVfmDto(PlayerGameweekLog logItem) {
        Player p = logItem.getPlayer();
        double price = p.getMarketInfo().getCurrentPrice();
        double xp = logItem.getProjection().getProjectedXp();
        double minutes = logItem.getProjection().getProjectedMinutes();
        double vfm = Math.round((xp / price) * 1000.0) / 1000.0; // 3 decimal places

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