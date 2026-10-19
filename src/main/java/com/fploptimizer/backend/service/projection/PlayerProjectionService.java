package com.fploptimizer.backend.service.projection;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.entity.player.PlayerGameweekProjection;
import com.fploptimizer.backend.service.projection.engine.*;
import com.fploptimizer.backend.service.projection.model.*;
import com.fploptimizer.backend.service.projection.util.PoissonMathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade service orchestrating all specialized projection engines to generate comprehensive player projections.
 * Coordinates execution flow across minutes estimation, attacking threats, defensive metrics,
 * disciplinary risks, and bonus point modeling, synthesizing all probabilistic components into
 * a final expected points (xP) projection according to official FPL scoring rules.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class PlayerProjectionService {

    private final PlayerMinutesEngine minutesEngine;
    private final PlayerAttackEngine attackEngine;
    private final PlayerDefenceEngine defenceEngine;
    private final PlayerCardsEngine cardsEngine;
    private final PlayerBonusEngine bonusEngine;

    /**
     * Executes the end-to-end projection workflow for an individual player in an upcoming match.
     * Evaluates on-pitch time first; if projected minutes are zero, short-circuits the pipeline
     * with zeroed metrics. Otherwise, evaluates offensive, defensive, disciplinary, and bonus engines,
     * applying official FPL scoring rules to determine expected points (xP).
     *
     * @param player         the player entity being projected
     * @param teamProjection team-level fixture context and scoring rates
     * @param historicalLogs prior gameweek logs used for empirical performance weighting
     * @return a populated PlayerGameweekProjection entity containing granular metrics and final xP
     */
    public PlayerGameweekProjection calculatePlayerProjection(
            Player player,
            TeamStrengthProjection teamProjection,
            List<PlayerGameweekLog> historicalLogs
    ) {
        PlayerGameweekProjection projection = new PlayerGameweekProjection();

        // 1. Team Context
        if (teamProjection != null) {
            projection.setProjectedTeamGoals(teamProjection.expectedGoals());
            projection.setProjectedOpponentGoals(teamProjection.expectedGoalsConceded());
        }

        if (player == null) {
            projection.setProjectedXp(0.0);
            return projection;
        }

        // 2. Step 1: Minutes Projection
        PlayerMinutesResult minutesResult = minutesEngine.calculateMinutes(player, historicalLogs);
        double minutes = minutesResult.projectedMinutes();
        double plus60Prob = minutesResult.sixtyPlusProbability();
        double startProb = minutesResult.startProbability();

        projection.setProjectedMinutes(minutes);

        // Short-circuit if player has no expected playing time
        if (minutes <= 0.0) {
            projection.setProjectedXg(0.0);
            projection.setProjectedXa(0.0);
            projection.setProjectedCleanSheetProb(0.0);
            projection.setProjectedGoalsConceded(0.0);
            projection.setProjectedDefensiveContribution(0.0);
            projection.setProjectedDefConProb(0.0);
            projection.setProjectedSaves(0.0);
            projection.setProjectedYellowCardsProb(0.0);
            projection.setProjectedRedCardsProb(0.0);
            projection.setProjectedBonus(0.0);
            projection.setProjectedXp(0.0);
            return projection;
        }

        // 3. Step 2: Attack, Defence, and Cards Projections
        PlayerAttackResult attackResult = attackEngine.calculatePlayerAttack(
                player, minutes, teamProjection, historicalLogs);

        PlayerDefenceResult defenceResult = defenceEngine.calculatePlayerDefence(
                player, minutes, plus60Prob, teamProjection, historicalLogs);

        PlayerCardsResult cardsResult = cardsEngine.calculatePlayerCards(
                player, minutes, historicalLogs);

        // 4. Step 3: Bonus Projection (Event-driven + Logs Blending)
        double projectedBonus = bonusEngine.calculatePlayerBonus(
                player, minutes, attackResult, defenceResult, cardsResult, historicalLogs);

        // 5. Store Engine Outputs into Projection Entity
        double xg = attackResult.expectedGoals();
        double xa = attackResult.expectedAssists();
        double csProb = defenceResult.cleanSheetProbability();
        double xgc = defenceResult.expectedGoalsConceded();
        double gcPenalty = defenceResult.expectedConcededGoalsDeduction();
        double saves = defenceResult.projectedSaves();
        double muDef = defenceResult.projectedDefCon();
        double defConProb = defenceResult.projectedDefConProb();
        double yellowCards = cardsResult.projectedYellowCards();
        double redCards = cardsResult.projectedRedCards();

        projection.setProjectedXg(xg);
        projection.setProjectedXa(xa);
        projection.setProjectedCleanSheetProb(csProb);
        projection.setProjectedGoalsConceded(xgc);
        projection.setProjectedDefensiveContribution(muDef);
        projection.setProjectedDefConProb(defConProb);
        projection.setProjectedSaves(saves);
        projection.setProjectedYellowCardsProb(yellowCards);
        projection.setProjectedRedCardsProb(redCards);
        projection.setProjectedBonus(projectedBonus);

        // 6. FPL Scoring Model (Expected Points - xP)
        double appearancePts = (1.0 * startProb) + (1.0 * plus60Prob);

        Position pos = player.getPosition();
        double goalPtsMultiplier = switch (pos) {
            case FWD -> 4.0;
            case MID -> 5.0;
            case DEF, GKP -> 6.0;
            case null -> 4.0;
        };
        double attackingPts = (xg * goalPtsMultiplier) + (xa * 3.0);

        double cleanSheetPts = switch (pos) {
            case DEF, GKP -> csProb * 4.0;
            case MID -> csProb * 1.0;
            case FWD -> 0.0;
            case null -> 0.0;
        };

        double concededPenaltyPts = (pos == Position.DEF || pos == Position.GKP) ? gcPenalty : 0.0;
        double savePts = (pos == Position.GKP) ? PoissonMathUtils.calculateExpectedSavePoints(saves) : 0.0;
        double defConPts = defConProb * 2.0;
        double cardsDeduction = (yellowCards * 1.0) + (redCards * 3.0);

        double totalXp = appearancePts
                + attackingPts
                + cleanSheetPts
                - concededPenaltyPts
                + savePts
                + defConPts
                - cardsDeduction
                + projectedBonus;

        projection.setProjectedXp(Math.max(0.0, totalXp));

        return projection;
    }
}