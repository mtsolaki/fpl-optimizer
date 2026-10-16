package com.fploptimizer.backend.service;

import com.fploptimizer.backend.client.FplApiClient;
import com.fploptimizer.backend.dto.FplUserPicksResponse;
import com.fploptimizer.backend.dto.advisor.LineupPlayerDto;
import com.fploptimizer.backend.dto.advisor.UserLineupResponseDto;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserLineupOptimizerService {

    private final PlayerRepository playerRepository;
    private final PlayerGameweekLogRepository playerGameweekLogRepository;
    private final FplApiClient fplApiClient;

    private static final int[][] VALID_FORMATIONS = {
            {3, 5, 2}, {3, 4, 3}, {4, 4, 2}, {4, 3, 3},
            {4, 5, 1}, {5, 3, 2}, {5, 4, 1}, {5, 2, 3}
    };

    /*
        Main method. Calls the client, fetches the data, tests formations, orders bench and selects captains
     */
    public UserLineupResponseDto optimizeUserLineup(int teamId, int gameweek) {
        // 1. Fetch picks from FPL API
        FplUserPicksResponse picksResponse = resolveLatestAvailablePicks(teamId, gameweek); 

        // 2. Enrich 15 players with projections and stats
        List<SquadCandidate> squad = fetchAndEnrichSquad(picksResponse, gameweek);
        if (squad.size() < 15) {
            log.warn("Team {} has only {} enriched players instead of 15", teamId, squad.size());
        }

        // 3. Group and sort by position (Higher xP first)
        Map<Position, List<SquadCandidate>> byPos = groupAndSortByPosition(squad);

        // Safe recover of gkps without the danger of IndexOutOfBoundsException
        List<SquadCandidate> gkps = byPos.getOrDefault(Position.GKP, List.of());
        SquadCandidate starterGkp = !gkps.isEmpty() ? gkps.get(0) : null;
        SquadCandidate benchGkp = gkps.size() > 1 ? gkps.get(1) : null;

        String bestFormation = "3-4-3";
        double maxTotalXp = -1.0;
        List<SquadCandidate> bestStarters = new ArrayList<>();
        List<SquadCandidate> bestBench = new ArrayList<>();

        // 4. Test of 8 valid formation
        for (int[] formation : VALID_FORMATIONS) {
            int needDef = formation[0];
            int needMid = formation[1];
            int needFwd = formation[2];

            List<SquadCandidate> currentStarters = new ArrayList<>();
            List<SquadCandidate> currentBench = new ArrayList<>();

            // Add gkps
            if (starterGkp != null) {
                currentStarters.add(starterGkp);
            }
            if (benchGkp != null) {
                currentBench.add(benchGkp);
            }

            partitionBySlots(byPos.get(Position.DEF), needDef, currentStarters, currentBench);
            partitionBySlots(byPos.get(Position.MID), needMid, currentStarters, currentBench);
            partitionBySlots(byPos.get(Position.FWD), needFwd, currentStarters, currentBench);

            double sumXp = currentStarters.stream()
                    .mapToDouble(SquadCandidate::projectedXp)
                    .sum();

            if (currentStarters.size() == 11 && sumXp > maxTotalXp) {
                maxTotalXp = sumXp;
                bestFormation = String.format("%d-%d-%d", needDef, needMid, needFwd);
                bestStarters = currentStarters;
                bestBench = currentBench;
            }

        }
        // Fallback if no formation didn't complete 11
        if (bestStarters.isEmpty()) {
            log.warn("Could not form a standard 11-player lineup for team {}. Using available squad.", teamId);
            bestStarters = new ArrayList<>(squad);
        }

        // 5. Sort best 11 by position (GKP -> DEF -> MID -> FWD)
        bestStarters.sort(Comparator.comparing(c -> c.player().getPosition()));

        // 6. Sort bench
        List<SquadCandidate> outfieldBench = bestBench.stream()
                .filter(c -> !c.equals(benchGkp))
                .sorted(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed())
                .toList();

        List<SquadCandidate> sortedBench = new ArrayList<>();
        if (benchGkp != null) {
            sortedBench.add(benchGkp);
        }
        sortedBench.addAll(outfieldBench);

        // 7. Select Captain & Vice-Captain with Safety Lock
        CaptaincyPick captaincy = selectCaptains(bestStarters);
        SquadCandidate captain = captaincy.captain();
        SquadCandidate viceCaptain = captaincy.viceCaptain();

        // Total xP considers double captain points (+ 1x xP)
        double totalXpWithCaptain = (maxTotalXp > 0 ? maxTotalXp : 0.0)
                + (captain != null ? captain.projectedXp() : 0.0);

        // 8.  DTOs creation for response
        List<LineupPlayerDto> starterDtos = bestStarters.stream()
                .map(c -> toLineupPlayerDto(c, c.equals(captain), c.equals(viceCaptain), null))
                .toList();

        List<LineupPlayerDto> benchDtos = new ArrayList<>();
        benchDtos.add(toLineupPlayerDto(sortedBench.get(0), false, false, 0)); // GK Sub
        for (int i = 1; i < sortedBench.size(); i++) {
            benchDtos.add(toLineupPlayerDto(sortedBench.get(i), false, false, i)); // Outfield Subs 1, 2, 3
        }

        FplUserPicksResponse.EntryHistory history = picksResponse.entryHistory();

        return UserLineupResponseDto.builder()
                .teamId(teamId)
                .gameweek(gameweek)
                .formation(bestFormation)
                .totalProjectedXp(totalXpWithCaptain)
                .captain(toLineupPlayerDto(captain, true, false, null))
                .viceCaptain(toLineupPlayerDto(viceCaptain, false, true, null))
                .starters(starterDtos)
                .bench(benchDtos)
                .bank(history != null ? history.getBankInMillions() : 0.0)
                .teamValue(history != null ? history.getValueInMillions() : 100.0)
                .build();
    }

    /*
        Mapping method from Squad Candidate to LineupPlayerDto
     */
    private LineupPlayerDto toLineupPlayerDto(
            SquadCandidate candidate,
            boolean isCaptain,
            boolean isViceCaptain,
            Integer benchOrder
    ) {
        Player p = candidate.player();
        Double price = (p.getMarketInfo() != null && p.getMarketInfo().getCurrentPrice() != null)
                ? p.getMarketInfo().getCurrentPrice()
                : null;
        String teamShort = (p.getTeam() != null) ? p.getTeam().getShortName() : null;

        return LineupPlayerDto.builder()
                .playerCode(p.getCode())
                .fplId(p.getId())
                .webName(p.getWebName())
                .teamShortName(teamShort)
                .position(p.getPosition())
                .currentPrice(price)
                .projectedXp(candidate.projectedXp())
                .projectedMinutes(candidate.projectedMinutes())
                .status(candidate.status())
                .news(candidate.news())
                .isCaptain(isCaptain)
                .isViceCaptain(isViceCaptain)
                .benchOrder(benchOrder)
                .build();
    }
    /*
        Connects API with the DB.
        Makes the list of 15 SquadCandidates.
     */
    private List<SquadCandidate> fetchAndEnrichSquad(
            FplUserPicksResponse picksResponse,
            int gameweek
    ) {
        List<Integer> fplIds = picksResponse.picks().stream()
                .map(FplUserPicksResponse.PickItem::elementId)
                .toList();

        // 1. Batch load of players
        List<Player> players = playerRepository.findByFplIdIn(fplIds);
        Map<Integer, Player> playerByFplId = players.stream()
                .collect(Collectors.toMap(Player::getId, p -> p));

        // 2. Batch load projections
        List<Integer> playerCodes = players.stream().map(Player::getCode).toList();
        Map<Integer, PlayerGameweekLog> logsByCode = playerGameweekLogRepository
                .findByPlayerCodeInAndGameweekId(playerCodes, gameweek)
                .stream()
                .collect(Collectors.toMap(log -> log.getPlayer().getCode(), log -> log));

        // 3. Composition
        List<SquadCandidate> candidates = new ArrayList<>();
        for (FplUserPicksResponse.PickItem pick : picksResponse.picks()) {
            Player player = playerByFplId.get(pick.elementId());
            if (player == null) {
                log.warn("Player with FPL ID {} not found in local DB", pick.elementId());
                continue;
            }

            PlayerGameweekLog logItem = logsByCode.get(player.getCode());
            double xp = (logItem != null && logItem.getProjection().getProjectedXp() != null)
                    ? logItem.getProjection().getProjectedXp() : 0.0;
            double minutes = (logItem != null && logItem.getProjection().getProjectedMinutes() != null)
                    ? logItem.getProjection().getProjectedMinutes() : 0.0;

            candidates.add(new SquadCandidate(
                    player,
                    xp,
                    minutes,
                    player.getStatus() != null ? player.getStatus() : "a",
                    player.getMarketInfo().getNews()
            ));
        }

        return candidates;
    }
    /*
        Selects Captain and Vice-Captain from the best eleven
     */
    private CaptaincyPick selectCaptains(List<SquadCandidate> starters) {
        // 1. Filter with Safety Lock
        List<SquadCandidate> safeCandidates = starters.stream()
                .filter(c -> "a".equalsIgnoreCase(c.status()) && c.projectedMinutes() >= 70.0)
                .sorted(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed())
                .toList();

        SquadCandidate captain;
        SquadCandidate viceCaptain;

        if (safeCandidates.size() >= 2) {
            captain = safeCandidates.get(0);
            viceCaptain = safeCandidates.get(1);
        } else if (safeCandidates.size() == 1) {
            captain = safeCandidates.get(0);
            // VC is selected from the rest starters with the bigger xP
            viceCaptain = starters.stream()
                    .filter(c -> !c.equals(captain))
                    .max(Comparator.comparingDouble(SquadCandidate::projectedXp))
                    .orElse(captain);
        } else {
            // Fallback: simple sort of all the starters by xP
            List<SquadCandidate> sortedAll = starters.stream()
                    .sorted(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed())
                    .toList();
            captain = sortedAll.get(0);
            viceCaptain = sortedAll.size() > 1 ? sortedAll.get(1) : captain;
        }

        return new CaptaincyPick(captain, viceCaptain);
    }

    private record CaptaincyPick(SquadCandidate captain, SquadCandidate viceCaptain) {}

    /*
        Separates players in a specific positon in starters and bench
     */
    private void partitionBySlots(
            List<SquadCandidate> positionCandidates,
            int starterSlots,
            List<SquadCandidate> startersTarget,
            List<SquadCandidate> benchTarget
    ) {
        for (int i = 0; i < positionCandidates.size(); i++) {
            if (i < starterSlots) {
                startersTarget.add(positionCandidates.get(i));
            } else {
                benchTarget.add(positionCandidates.get(i));
            }
        }
    }

    /*
        Split 15 candidates by position and set the order by xP
     */
    private Map<Position, List<SquadCandidate>> groupAndSortByPosition(List<SquadCandidate> squad) {
        Map<Position, List<SquadCandidate>> byPosition = squad.stream()
                .collect(Collectors.groupingBy(c -> c.player().getPosition()));

        // Sort per position. Higher xP first
        for (List<SquadCandidate> list : byPosition.values()) {
            list.sort(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed());
        }

        return byPosition;
    }

    // Necessary info for a player for a specific fixture
    private record SquadCandidate(
            Player player,
            double projectedXp,
            double projectedMinutes,
            String status,
            String news
    ) {}

    private FplUserPicksResponse resolveLatestAvailablePicks(int teamId, int requestedGameweek) {
        // Try from requested gameweek backwords till GW 1
        for (int gw = requestedGameweek; gw >= 1; gw--) {
            Optional<FplUserPicksResponse> picksOpt = fplApiClient.getUserPicks(teamId, gw);
            if (picksOpt.isPresent()) {
                if (gw != requestedGameweek) {
                    log.info("Picks for GW {} not open yet. Falling back to squad from GW {}", requestedGameweek, gw);
                }
                return picksOpt.get();
            }
        }
        // Only when team id doesn't exist throw exception
        throw new NoSuchElementException("No picks found for teamId " + teamId + " in any gameweek.");
    }
}
