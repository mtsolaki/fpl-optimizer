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

@Component
public class LineupDegradationEngine {

    //Player from the bench provides 60% than the main player
    private static final double BENCH_REPLACEMENT_FACTOR = 0.60;

    // GKP absence +10% xGCon, DEF absence +5% xGCon
    private static final double GKP_ABSENCE_PENALTY = 0.10;
    private static final double DEF_ABSENCE_PENALTY = 0.05;

    // Clamping Bounds
    private static final double MIN_ATTACK_DEGRADATION = 0.70;
    private static final double MAX_ATTACK_DEGRADATION = 1.00;
    private static final double MIN_DEFENSE_DEGRADATION = 1.00;
    private static final double MAX_DEFENSE_DEGRADATION = 1.30;

    // Used to avoid divideByZero
    private static final double MIN_PLAYER_PRODUCTION = 0.05;

    public LineupDegradation calculateDegradation(List<Player> teamPlayers) {
        if (teamPlayers == null || teamPlayers.isEmpty()) {
            return new LineupDegradation(1.0, 1.0);
        }

        //Select Starters
        List<Player> baselinePlayers = findBaselineStarters(teamPlayers);

        // Calculate I_att
        double attackDegradation = calculateAttackDegradation(baselinePlayers);

        // Calculate I_deff
        double defenseDegradation = calculateDefenseDegradation(baselinePlayers);

        return new LineupDegradation(attackDegradation, defenseDegradation);

    }

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
     * Calculates I_deff based on missing GKP and DEFs
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
     * Available Possibility in [0.0, 1.0].
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




        return 1.0; // Θεωρείται διαθέσιμος αν δεν υπάρχει ένδειξη τραυματισμού/τιμωρίας
    }
    private int getPlayerStarts(Player player) {
        PlayerPerformance perf = player.getPerformance();
        if (perf != null && perf.getStarts() != null) {
            return perf.getStarts();
        }
        return 0;
    }

    private void addTop(List<Player> target, List<Player> source, int n) {
        for (int i = 0; i < Math.min(n, source.size()); i++) {
            target.add(source.get(i));
        }
    }
}
