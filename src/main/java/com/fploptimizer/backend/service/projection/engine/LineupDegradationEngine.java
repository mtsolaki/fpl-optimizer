package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.entity.player.PlayerAdvancedMetrics;
import com.fploptimizer.backend.entity.player.PlayerMarketInfo;
import com.fploptimizer.backend.entity.player.PlayerPerformance;
import com.fploptimizer.backend.service.projection.model.LineupDegradation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Computational engine evaluating team degradation factors caused by injuries,
 * suspensions, and player absences.
 * <p>
 * Simulates a realistic starting eleven from available squad members. It also assesses
 * expected attacking threat loss (xG + xA) and defensive vulnerability penalties.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
public class LineupDegradationEngine {

    // Assumed percentage of baseline production delivered by a replacement bench player
    private static final double BENCH_REPLACEMENT_FACTOR = 0.60;

    // Vulnerability penalties applied to expected goals conceded (xGC) for key absentees
    private static final double GKP_ABSENCE_PENALTY = 0.10;
    private static final double DEF_ABSENCE_PENALTY = 0.05;

    // Clamping boundaries for attack degradation factor (I_att in [0.70, 1.00])
    private static final double MIN_ATTACK_DEGRADATION = 0.70;
    private static final double MAX_ATTACK_DEGRADATION = 1.00;

    // Clamping boundaries for defense degradation factor (I_def in [1.00, 1.30])
    private static final double MIN_DEFENSE_DEGRADATION = 1.00;
    private static final double MAX_DEFENSE_DEGRADATION = 1.30;

    // Minimum positive offensive contribution threshold to prevent division by zero
    private static final double MIN_PLAYER_PRODUCTION = 0.05;

    // FPL status flags representing complete unavailability ("i" = injured, "s" = suspended, "u" = unavailable)
    private static final Set<String> UNAVAILABLE_STATUSES = Set.of("i", "s", "u");

    // Maximum allowed outfield players per position under valid FPL formation rules
    private static final Map<Position, Integer> MAX_OUTFIELD_SLOTS = Map.of(
            Position.DEF, 5,
            Position.MID, 5,
            Position.FWD, 3
    );

    /**
     * Calculates the attack and defense degradation multipliers for a given squad.
     * <p>
     * Identifies the baseline starting eleven based on historical starts. Then it evaluates
     * offensive efficiency under current availability and defensive exposure penalties.
     *
     * @param teamPlayers the complete roster of registered players for the team
     * @return a LineupDegradation record containing attack and defense adjustment multipliers
     */
    public LineupDegradation calculateDegradation(List<Player> teamPlayers) {
        if (teamPlayers == null || teamPlayers.isEmpty()) {
            return new LineupDegradation(1.0, 1.0);
        }

        // 1. Identify expected baseline starters
        List<Player> baselinePlayers = findBaselineStarters(teamPlayers);

        // 2. Calculate attacking production ratio (I_att)
        double attackDegradation = calculateAttackDegradation(baselinePlayers);

        // 3. Calculate defensive vulnerability inflation (I_def)
        double defenseDegradation = calculateDefenseDegradation(baselinePlayers);

        return new LineupDegradation(attackDegradation, defenseDegradation);
    }

    /**
     * Reconstructs the most probable starting eleven from a squad roster.
     * <p>
     * Groups players by position and sorts them by historical match starts descending.
     * It fills mandatory positional quotas (1 GKP, 3 DEF, 2 MID, 1 FWD) first, then allocates
     * the remaining 4 outfield slots under valid formation constraints.
     *
     * @param teamPlayers the complete squad player list
     * @return a list containing up to 11 predicted baseline starting players
     */
    public List<Player> findBaselineStarters(List<Player> teamPlayers) {
        Map<Position, List<Player>> byPosition = new EnumMap<>(Position.class);
        for (Position pos : Position.values()) {
            byPosition.put(pos, new ArrayList<>());
        }

        for (Player p : teamPlayers) {
            if (p.getPosition() != null) {
                byPosition.get(p.getPosition()).add(p);
            }
        }

        Comparator<Player> startsDesc = Comparator.comparingInt(this::getPlayerStarts).reversed();
        byPosition.values().forEach(list -> list.sort(startsDesc));

        List<Player> goalkeepers = byPosition.get(Position.GKP);
        List<Player> defenders = byPosition.get(Position.DEF);
        List<Player> midfielders = byPosition.get(Position.MID);
        List<Player> forwards = byPosition.get(Position.FWD);

        List<Player> starters = new ArrayList<>(11);

        // Main GKP and mandatory minimum positional quotas
        if (!goalkeepers.isEmpty()) {
            starters.add(goalkeepers.getFirst());
        }
        addTop(starters, defenders, 3);
        addTop(starters, midfielders, 2);
        addTop(starters, forwards, 1);

        // Remaining outfield candidates sorted by starts descending
        List<Player> remainingOutfield = new ArrayList<>();
        remainingOutfield.addAll(tailFrom(defenders, 3));
        remainingOutfield.addAll(tailFrom(midfielders, 2));
        remainingOutfield.addAll(tailFrom(forwards, 1));
        remainingOutfield.sort(startsDesc);

        Map<Position, Integer> slotCounts = new EnumMap<>(Position.class);
        slotCounts.put(Position.DEF, Math.min(defenders.size(), 3));
        slotCounts.put(Position.MID, Math.min(midfielders.size(), 2));
        slotCounts.put(Position.FWD, Math.min(forwards.size(), 1));

        for (Player p : remainingOutfield) {
            if (starters.size() >= 11) {
                break;
            }
            Position pos = p.getPosition();
            int currentCount = slotCounts.getOrDefault(pos, 0);
            if (currentCount < MAX_OUTFIELD_SLOTS.getOrDefault(pos, 0)) {
                starters.add(p);
                slotCounts.put(pos, currentCount + 1);
            }
        }

        return starters;
    }

    /**
     * Computes the attacking degradation multiplier by comparing expected offensive production
     * against ideal full-strength squad output.
     * <p>
     * Evaluates midfielders and forwards based on combined expected goal involvements (xG90 + xA90).
     * Missing chance of playing scales down player contribution, assuming bench replacements
     * recover 60% of baseline output.
     *
     * @param starters the list of baseline starting players
     * @return the attack degradation multiplier clamped between [MIN_ATTACK_DEGRADATION, MAX_ATTACK_DEGRADATION]
     */
    public double calculateAttackDegradation(List<Player> starters) {
        double idealAttProduction = 0.0;
        double actualAttProduction = 0.0;

        for (Player p : starters) {
            Position pos = p.getPosition();
            if (pos != Position.MID && pos != Position.FWD) {
                continue;
            }

            double playerProduction = getOffensiveProductionPer90(p);
            double cp = resolveAvailabilityChance(p);

            idealAttProduction += playerProduction;
            actualAttProduction += (cp * playerProduction)
                    + ((1.0 - cp) * playerProduction * BENCH_REPLACEMENT_FACTOR);
        }

        if (idealAttProduction <= 0.0) {
            return 1.0;
        }
        double ratio = actualAttProduction / idealAttProduction;
        return Math.clamp(ratio, MIN_ATTACK_DEGRADATION, MAX_ATTACK_DEGRADATION);
    }

    /**
     * Calculates the defensive vulnerability multiplier (I_def) driven by unavailable
     * starting goalkeepers and defenders.
     * <p>
     * Each missing starting defender or goalkeeper inflates expected goals conceded (xGC).
     *
     * @param starters the list of baseline starting players
     * @return the defense degradation multiplier clamped between [MIN_DEFENSE_DEGRADATION, MAX_DEFENSE_DEGRADATION]
     */
    private double calculateDefenseDegradation(List<Player> starters) {
        double totalPenalty = 0.0;

        for (Player p : starters) {
            if (p.getPosition() == null) {
                continue;
            }
            double cp = resolveAvailabilityChance(p);
            double absenceWeight = 1.0 - cp;

            if (p.getPosition() == Position.GKP) {
                totalPenalty += absenceWeight * GKP_ABSENCE_PENALTY;
            } else if (p.getPosition() == Position.DEF) {
                totalPenalty += absenceWeight * DEF_ABSENCE_PENALTY;
            }
        }

        double rawFactor = 1.0 + totalPenalty;
        return Math.clamp(rawFactor, MIN_DEFENSE_DEGRADATION, MAX_DEFENSE_DEGRADATION);
    }

    /**
     * Resolves the probability of a player being available to play, expressed in range [0.0, 1.0].
     * <p>
     * Accounts for explicit unavailability flags ('i' for injured, 's' for suspended,
     * 'u' for unavailable) and FPL market news chance-of-playing percentages.
     *
     * @param player the player entity being assessed
     * @return availability probability in range [0.0, 1.0]
     */
    private double resolveAvailabilityChance(Player player) {
        String status = player.getStatus();
        if (status != null && UNAVAILABLE_STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
            return 0.0;
        }

        PlayerMarketInfo marketInfo = player.getMarketInfo();
        if (marketInfo != null && marketInfo.getChanceOfPlayingThisRound() != null) {
            return Math.clamp(marketInfo.getChanceOfPlayingThisRound() / 100.0, 0.0, 1.0);
        }

        return 1.0;
    }

    /**
     * Computes a player's combined expected goal involvement per 90 minutes (xG90 + xA90),
     * floored at {@link #MIN_PLAYER_PRODUCTION}.
     *
     * @param player the player entity
     * @return offensive production per 90 minutes, at least {@link #MIN_PLAYER_PRODUCTION}
     */
    private double getOffensiveProductionPer90(Player player) {
        PlayerAdvancedMetrics metrics = player.getAdvancedMetrics();
        if (metrics == null) {
            return MIN_PLAYER_PRODUCTION;
        }
        double xg90 = metrics.getExpectedGoalsPer90() != null ? metrics.getExpectedGoalsPer90() : 0.0;
        double xa90 = metrics.getExpectedAssistsPer90() != null ? metrics.getExpectedAssistsPer90() : 0.0;
        return Math.max(MIN_PLAYER_PRODUCTION, xg90 + xa90);
    }

    /**
     * Safely retrieves the number of games started by a player during the current campaign.
     *
     * @param player the player entity
     * @return number of starts, or 0 if performance metrics are unpopulated
     */
    private int getPlayerStarts(Player player) {
        PlayerPerformance perf = player.getPerformance();
        if (perf != null && perf.getStarts() != null) {
            return perf.getStarts();
        }
        return 0;
    }

    /**
     * Returns a view of the list starting at {@code fromIndex}, or an empty list if {@code fromIndex}
     * is beyond the list size.
     *
     * @param list      the source list
     * @param fromIndex the starting index (inclusive)
     * @return sublist from {@code fromIndex} to the end, or an empty list
     */
    private List<Player> tailFrom(List<Player> list, int fromIndex) {
        return list.size() > fromIndex ? list.subList(fromIndex, list.size()) : List.of();
    }

    /**
     * Appends up to n elements from the source list into the target list.
     *
     * @param target the destination list
     * @param source the source list
     * @param n      maximum number of elements to add
     */
    private void addTop(List<Player> target, List<Player> source, int n) {
        for (int i = 0; i < Math.min(n, source.size()); i++) {
            target.add(source.get(i));
        }
    }
}