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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service providing mathematical squad lineup optimization and tactical advisory for FPL squads.
 * <p>
 * Fetches user picks from the official FPL API and enriches the 15-player squad with projected stats.
 * Evaluates all 8 valid FPL formations to maximize starting expected points (xP), orders the bench,
 * and selects optimal captain and vice-captain designations.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserLineupOptimizerService {

    private final PlayerRepository playerRepository;
    private final PlayerGameweekLogRepository playerGameweekLogRepository;
    private final FplApiClient fplApiClient;

    /**
     * All 8 legitimate tactical formations permitted under official Fantasy Premier League rules:
     * Format: {Defenders, Midfielders, Forwards}.
     */
    private static final int[][] VALID_FORMATIONS = {
            {3, 5, 2}, {3, 4, 3}, {4, 4, 2}, {4, 3, 3},
            {4, 5, 1}, {5, 3, 2}, {5, 4, 1}, {5, 2, 3}
    };

    /**
     * Calculates the optimal starting eleven, bench ordering, and captaincy designation for a manager squad.
     * <p>
     * Retrieves current squad picks, enriches each player with projection data, and evaluates each valid
     * formation to identify the highest expected points lineup. Applies safety locks for captain and
     * vice-captain selection.
     *
     * @param teamId   unique FPL entry identifier of the manager squad
     * @param gameweek target gameweek round number
     * @return a complete UserLineupResponseDto detailing the optimized lineup, bench hierarchy, and expected points
     */
    public UserLineupResponseDto optimizeUserLineup(int teamId, int gameweek) {
        // 1. Fetch squad picks from official FPL API
        FplUserPicksResponse picksResponse = resolveLatestAvailablePicks(teamId, gameweek);

        // 2. Enrich squad players with projections and stats
        List<SquadCandidate> squad = fetchAndEnrichSquad(picksResponse, gameweek);
        if (squad.size() < 15) {
            log.warn("Team {} has only {} enriched players instead of 15", teamId, squad.size());
        }

        // 3. Group and rank players within each position by descending projected xP
        Map<Position, List<SquadCandidate>> byPos = groupAndSortByPosition(squad);

        // Safe extraction of backup goalkeeper
        List<SquadCandidate> gkps = byPos.getOrDefault(Position.GKP, List.of());
        SquadCandidate benchGkp = gkps.size() > 1 ? gkps.get(1) : null;

        String bestFormation = "3-4-3";
        double maxTotalXp = -1.0;
        List<SquadCandidate> bestStarters = new ArrayList<>();
        List<SquadCandidate> bestBench = new ArrayList<>();

        // 4. Test each of the 8 legal tactical formations
        for (int[] formation : VALID_FORMATIONS) {
            int needDef = formation[0];
            int needMid = formation[1];
            int needFwd = formation[2];

            List<SquadCandidate> currentStarters = new ArrayList<>();
            List<SquadCandidate> currentBench = new ArrayList<>();

            partitionBySlots(byPos.get(Position.GKP), 1, currentStarters, currentBench);
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

        // Fallback if no valid formation could form a full 11
        if (bestStarters.isEmpty()) {
            log.warn("Could not form a standard 11-player lineup for team {}. Using available squad.", teamId);
            bestStarters = new ArrayList<>(squad);
        }

        // 5. Order starting eleven tactically by position (GKP -> DEF -> MID -> FWD)
        bestStarters.sort(Comparator.comparing(c -> c.player().getPosition()));

        // 6. Order substitute bench: Goalkeeper backup always first, outfield subs ranked by descending xP
        List<SquadCandidate> outfieldBench = bestBench.stream()
                .filter(c -> !c.equals(benchGkp))
                .sorted(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed())
                .toList();

        // 7. Select Captain & Vice-Captain using the safety lock algorithm
        CaptaincyPick captaincy = selectCaptains(bestStarters);
        SquadCandidate captain = captaincy.captain();
        SquadCandidate viceCaptain = captaincy.viceCaptain();

        // Total xP includes doubled captain points (+ 1x captain xP)
        double totalXpWithCaptain = (maxTotalXp > 0 ? maxTotalXp : 0.0)
                + (captain != null ? captain.projectedXp() : 0.0);

        // 8. Map candidates to presentation DTOs
        List<LineupPlayerDto> starterDtos = bestStarters.stream()
                .map(c -> toLineupPlayerDto(c, Objects.equals(c, captain), Objects.equals(c, viceCaptain), null))
                .toList();

        List<LineupPlayerDto> benchDtos = new ArrayList<>();
        if (benchGkp != null) {
            benchDtos.add(toLineupPlayerDto(benchGkp, false, false, 0)); // GK Substitute (Order 0)
        }
        for (int i = 0; i < outfieldBench.size(); i++) {
            benchDtos.add(toLineupPlayerDto(outfieldBench.get(i), false, false, i + 1)); // Outfield Subs (Orders 1, 2, 3)
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

    /**
     * Maps an internal SquadCandidate record into a LineupPlayerDto.
     *
     * @param candidate     the candidate player record
     * @param isCaptain     true if designated as team captain
     * @param isViceCaptain true if designated as vice-captain
     * @param benchOrder    bench priority index (0 for sub GK, 1-3 for outfield bench, null for starters)
     * @return populated LineupPlayerDto, or null if candidate is null
     */
    private LineupPlayerDto toLineupPlayerDto(
            SquadCandidate candidate,
            boolean isCaptain,
            boolean isViceCaptain,
            Integer benchOrder
    ) {
        if (candidate == null) {
            return null;
        }

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

    /**
     * Loads squad members from local storage and pairs them with projections for the target round.
     * <p>
     * Performs bulk lookups across player entities and gameweek logs to avoid N+1 database queries
     * and aggregates projections across multiple fixtures during Double Gameweeks.
     *
     * @param picksResponse raw picks payload from the FPL API
     * @param gameweek      target gameweek ID
     * @return list of enriched SquadCandidate records
     */
    private List<SquadCandidate> fetchAndEnrichSquad(
            FplUserPicksResponse picksResponse,
            int gameweek
    ) {
        if (picksResponse == null || picksResponse.picks() == null) {
            return List.of();
        }

        List<Integer> fplIds = picksResponse.picks().stream()
                .map(FplUserPicksResponse.PickItem::elementId)
                .toList();

        // 1. Batch load player entities
        List<Player> players = playerRepository.findByFplIdIn(fplIds);
        Map<Integer, Player> playerByFplId = players.stream()
                .collect(Collectors.toMap(Player::getId, p -> p, (existing, _) -> existing));

        // 2. Batch load gameweek projections (safely summing multiple fixtures in Double Gameweeks)
        List<Integer> playerCodes = players.stream().map(Player::getCode).toList();
        Map<Integer, RoundProjection> projectionsByCode = playerGameweekLogRepository
                .findByPlayerCodeInAndGameweekId(playerCodes, gameweek)
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getPlayer().getCode(),
                        RoundProjection::fromLog,
                        RoundProjection::add
                ));

        // 3. Assemble candidate records
        List<SquadCandidate> candidates = new ArrayList<>();
        for (FplUserPicksResponse.PickItem pick : picksResponse.picks()) {
            Player player = playerByFplId.get(pick.elementId());
            if (player == null) {
                log.warn("Player with FPL ID {} not found in local DB", pick.elementId());
                continue;
            }

            RoundProjection projection = projectionsByCode.getOrDefault(player.getCode(), RoundProjection.EMPTY);
            String news = player.getMarketInfo() != null ? player.getMarketInfo().getNews() : null;

            candidates.add(new SquadCandidate(
                    player,
                    projection.xp(),
                    projection.minutes(),
                    player.getStatus() != null ? player.getStatus() : "a",
                    news
            ));
        }

        return candidates;
    }

    /**
     * Selects captain and vice-captain using a safety-lock algorithm.
     * <p>
     * Requires candidates to be fully available (status 'a') with at least 70 expected minutes
     * to eliminate rotation vulnerabilities from doubled armbands.
     *
     * @param starters the chosen starting eleven
     * @return CaptaincyPick record with designated captain and vice-captain
     */
    private CaptaincyPick selectCaptains(List<SquadCandidate> starters) {
        if (starters == null || starters.isEmpty()) {
            return new CaptaincyPick(null, null);
        }

        // Filter candidates through the safety lock criteria
        List<SquadCandidate> safeCandidates = starters.stream()
                .filter(c -> "a".equalsIgnoreCase(c.status()) && c.projectedMinutes() >= 70.0)
                .sorted(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed())
                .toList();

        SquadCandidate captain;
        SquadCandidate viceCaptain;

        if (safeCandidates.size() >= 2) {
            captain = safeCandidates.getFirst();
            viceCaptain = safeCandidates.get(1);
        } else if (safeCandidates.size() == 1) {
            captain = safeCandidates.getFirst();
            viceCaptain = starters.stream()
                    .filter(c -> !c.equals(captain))
                    .max(Comparator.comparingDouble(SquadCandidate::projectedXp))
                    .orElse(captain);
        } else {
            // Fallback: rank all starters purely by projected xP
            List<SquadCandidate> sortedAll = starters.stream()
                    .sorted(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed())
                    .toList();
            captain = sortedAll.getFirst();
            viceCaptain = sortedAll.size() > 1 ? sortedAll.get(1) : captain;
        }

        return new CaptaincyPick(captain, viceCaptain);
    }

    private record CaptaincyPick(SquadCandidate captain, SquadCandidate viceCaptain) {
    }

    private record RoundProjection(double xp, double minutes) {
        static final RoundProjection EMPTY = new RoundProjection(0.0, 0.0);

        static RoundProjection fromLog(PlayerGameweekLog entry) {
            if (entry == null || entry.getProjection() == null) {
                return EMPTY;
            }
            double xp = entry.getProjection().getProjectedXp() != null
                    ? entry.getProjection().getProjectedXp() : 0.0;
            double minutes = entry.getProjection().getProjectedMinutes() != null
                    ? entry.getProjection().getProjectedMinutes() : 0.0;
            return new RoundProjection(xp, minutes);
        }

        RoundProjection add(RoundProjection other) {
            return new RoundProjection(this.xp + other.xp, this.minutes + other.minutes);
        }
    }

    /**
     * Splits positional candidates into starting slots and bench slots based on formation needs.
     *
     * @param positionCandidates candidates for a single tactical position
     * @param starterSlots       number of starting slots allocated by the formation
     * @param startersTarget     target list receiving starters
     * @param benchTarget        target list receiving bench players
     */
    private void partitionBySlots(
            List<SquadCandidate> positionCandidates,
            int starterSlots,
            List<SquadCandidate> startersTarget,
            List<SquadCandidate> benchTarget
    ) {
        if (positionCandidates == null) {
            return;
        }
        for (int i = 0; i < positionCandidates.size(); i++) {
            if (i < starterSlots) {
                startersTarget.add(positionCandidates.get(i));
            } else {
                benchTarget.add(positionCandidates.get(i));
            }
        }
    }

    /**
     * Groups squad candidates by tactical pitch position and orders each group by descending projected xP.
     *
     * @param squad full squad candidates list
     * @return map grouping candidates by Position, sorted by xP descending
     */
    private Map<Position, List<SquadCandidate>> groupAndSortByPosition(List<SquadCandidate> squad) {
        Map<Position, List<SquadCandidate>> byPosition = squad.stream()
                .collect(Collectors.groupingBy(c -> c.player().getPosition()));

        for (List<SquadCandidate> list : byPosition.values()) {
            list.sort(Comparator.comparingDouble(SquadCandidate::projectedXp).reversed());
        }

        return byPosition;
    }

    /**
     * Internal container storing candidate player details and projection metrics during lineup evaluation.
     */
    private record SquadCandidate(
            Player player,
            double projectedXp,
            double projectedMinutes,
            String status,
            String news
    ) {
    }

    /**
     * Retrieves squad picks from the official FPL API, falling back to earlier rounds if future gameweeks are not yet open.
     *
     * @param teamId            unique FPL team ID
     * @param requestedGameweek target gameweek round
     * @return active FplUserPicksResponse
     * @throws NoSuchElementException if no historical or active picks exist for the team ID
     */
    private FplUserPicksResponse resolveLatestAvailablePicks(int teamId, int requestedGameweek) {
        for (int gw = requestedGameweek; gw >= 1; gw--) {
            Optional<FplUserPicksResponse> picksOpt = fplApiClient.getUserPicks(teamId, gw);
            if (picksOpt.isPresent()) {
                if (gw != requestedGameweek) {
                    log.info("Picks for GW {} not open yet. Falling back to squad from GW {}", requestedGameweek, gw);
                }
                return picksOpt.get();
            }
        }
        throw new NoSuchElementException("No picks found for teamId " + teamId + " in any gameweek.");
    }
}