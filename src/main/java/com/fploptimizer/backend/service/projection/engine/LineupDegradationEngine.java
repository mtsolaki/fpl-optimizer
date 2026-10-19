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
import java.util.List;
/**
 * Computational engine evaluating team degradation factors caused by injuries,
 * suspensions, and player unavailabilities.
 *
 * Simulates a realistic starting eleven from available squad members, assesses the expected
 * loss in attacking threat (xG + xA) when core creative players are absent, and computes
 * defensive vulnerability penalties when key goalkeepers or defenders miss out.
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

    /**
     * Calculates the attack and defense degradation multipliers for a given squad.
     *
     * Identifies the baseline starting eleven based on historical starting frequency,
     * computes the offensive efficiency ratio under current player availability, and evaluates
     * defensive exposure penalties.
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
     *
     * Groups players by pitch position, sorts them by historical match starts in descending order,
     * fills mandatory minimum positional quotas (1 GKP, 3 DEF, 2 MID, 1 FWD), and dynamically
     * allocates the remaining 4 outfield slots to the most frequently started players under valid
     * formation constraints.
     *
     * @param teamPlayers the complete squad player list
     * @return a list containing up to 11 predicted baseline starting players
     */
    public List<Player> findBaselineStarters(List<Player> teamPlayers) {

        List<Player> gks = new ArrayList<>();
        List<Player> defs = new ArrayList<>();
        List<Player> mids = new ArrayList<>();
        List<Player> fwds = new ArrayList<>();

        for (Player p : teamPlayers) {
            if (p.getPosition() == null) continue;
            switch (p.getPosition()) {
                case GKP -> gks.add(p);
                case DEF -> defs.add(p);
                case MID -> mids.add(p);
                case FWD -> fwds.add(p);
            }
        }

        Comparator<Player> startsDesc = Comparator.comparing(this::getPlayerStarts);

        gks.sort(startsDesc);
        defs.sort(startsDesc);
        mids.sort(startsDesc);
        fwds.sort(startsDesc);

        List<Player> starters = new ArrayList<>();

        // main GKP
        if (!gks.isEmpty()) starters.add(gks.getFirst());
        //minimum Baseline
        addTop(starters, defs, 3);
        addTop(starters, mids, 2);
        addTop(starters, fwds, 1);

        // rest
        List<Player> remainingOutfield = new ArrayList<>();
        if (defs.size() > 3) remainingOutfield.addAll(defs.subList(3, defs.size()));
        if (mids.size() > 2) remainingOutfield.addAll(mids.subList(2, mids.size()));
        if (fwds.size() > 1) remainingOutfield.addAll(fwds.subList(1, fwds.size()));

        remainingOutfield.sort(startsDesc);

        int currentDefs = Math.min(defs.size(), 3);
        int currentMids = Math.min(mids.size(), 2);
        int currentFwds = Math.min(fwds.size(), 1);

        for (Player p : remainingOutfield) {
            if (starters.size() >= 11) break;

            Position type = p.getPosition();
            if (type == Position.DEF && currentDefs < 5) {
                starters.add(p);
                currentDefs++;
            } else if (type == Position.MID && currentMids < 5) {
                starters.add(p);
                currentMids++;
            } else if (type == Position.FWD && currentFwds < 3) {
                starters.add(p);
                currentFwds++;
            }
        }

        return starters;
    }
    /**
     * Computes the attacking degradation multiplier by comparing expected offensive production
     * against ideal full-strength squad output.
     *
     * Evaluates midfielders and forwards based on combined expected goal involvements (xG90 + xA90).
     * Missing chance of playing scales down player contribution, assuming bench replacements
     * recover only a fraction (60%) of baseline output.
     *
     * @param starters the list of baseline starting players
     * @return the attack degradation multiplier clamped between [MIN_ATTACK_DEGRADATION, MAX_ATTACK_DEGRADATION]
     */
    public double calculateAttackDegradation(List<Player> starters) {
        double idealAttProduction = 0.0;
        double actualAttProduction = 0.0;

        for (Player p : starters) {
            if (p.getPosition() == null) continue;
            // Mid And Fwds focus in Attack
            if (p.getPosition() == Position.MID || p.getPosition() == Position.FWD) {
                double xg90 = 0.0;
                double xa90 = 0.0;

                PlayerAdvancedMetrics metrics = p.getAdvancedMetrics();
                if (metrics != null) {
                    xg90 = metrics.getExpectedGoalsPer90() != null ? metrics.getExpectedGoalsPer90() : 0.0;
                    xa90 = metrics.getExpectedAssistsPer90() != null ? metrics.getExpectedAssistsPer90() : 0.0;
                }

                double playerProduction = Math.max(MIN_PLAYER_PRODUCTION, xg90 + xa90);
                double cp = resolveAvailabilityChance(p);

                idealAttProduction += playerProduction;
                actualAttProduction += (cp * playerProduction)
                        + ((1.0 - cp) * playerProduction * BENCH_REPLACEMENT_FACTOR);
            }
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
     *
     * Each missing starting defender or goalkeeper inflates expected goals conceded (xGC).
     *
     * @param starters the list of baseline starting players
     * @return the defense degradation multiplier clamped between [MIN_DEFENSE_DEGRADATION, MAX_DEFENSE_DEGRADATION]
     */
    private double calculateDefenseDegradation(List<Player> starters) {
        double totalPenalty = 0.0;

        for (Player p : starters) {
            if (p.getPosition() == null) continue;
            double cp = resolveAvailabilityChance(p);
            double absenceWeight = 1.0 - cp;

            if (p.getPosition() == Position.GKP) { // GKP
                totalPenalty += absenceWeight * GKP_ABSENCE_PENALTY;
            } else if (p.getPosition() == Position.DEF) { // DEF
                totalPenalty += absenceWeight * DEF_ABSENCE_PENALTY;
            }
        }

        double rawFactor = 1.0 + totalPenalty;
        return Math.clamp(rawFactor, MIN_DEFENSE_DEGRADATION, MAX_DEFENSE_DEGRADATION);
    }

    /**
     * Resolves the probability of a player being available to play, expressed in range [0.0, 1.0].
     *
     * Takes into account explicit unavailability flags ('i' for injured, 's' for suspended,
     * 'u' for unavailable) and FPL market news chance-of-playing percentages.
     *
     * @param player the player entity being assessed
     * @return availability probability in range [0.0, 1.0]
     */
    private double resolveAvailabilityChance(Player player) {

        String status = player.getStatus();
        if (status != null) {
            // "i" = injured, "s" = suspended, "u" = unavailable
            if (status.equalsIgnoreCase("i") || status.equalsIgnoreCase("s") || status.equalsIgnoreCase("u")) {
                return 0.0;
            }
        }
        PlayerMarketInfo marketInfo = player.getMarketInfo();
        if (marketInfo != null && marketInfo.getChanceOfPlayingThisRound() != null) {
            return marketInfo.getChanceOfPlayingThisRound() / 100.0;
        }




        return 1.0;
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
