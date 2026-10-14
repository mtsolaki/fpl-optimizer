package com.fploptimizer.backend.service.projection.engine;

import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import com.fploptimizer.backend.entity.Position;
import com.fploptimizer.backend.service.projection.model.PlayerMinutesResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;



@Component
public class PlayerMinutesEngine {

    private static final double DEFAULT_STARTER_MINUTES = 82.0;
    private static final double DEFAULT_SUB_MINUTES = 20.0;

    // Probability of 60+ mins if he is a starter
    private static final double PRIOR_60_MIN_GKP = 0.98;
    private static final double PRIOR_60_MIN_DEF = 0.92;
    private static final double PRIOR_60_MIN_MID = 0.82;
    private static final double PRIOR_60_MIN_FWD = 0.78;

    // Βάρος Bayesian Shrinkage (pseudo-starts για σταθερότητα σε μικρό δείγμα)
    private static final double PSEUDO_STARTS_WEIGHT = 3.0;

    public PlayerMinutesResult calculateMinutes (Player player, List<PlayerGameweekLog> gameweekLogs)
    {
        if (player == null) return new PlayerMinutesResult(0.0, 0,0.0);

        double availability = resolveAvailability(player);
        if (availability == 0.0){
            return new PlayerMinutesResult(0.0, 0,0.0);
        }

        double startProbability = calculateStartProbability(player, gameweekLogs);
        double finalStartProbability = Math.clamp(startProbability * availability, 0.0, 1.0);

        double plus60Probability = calculateSixtyPlusGivenStart(player, gameweekLogs);
        double finalPlus60Probability = Math.clamp(finalStartProbability * plus60Probability, 0.0, 1.0);

        boolean isGoalkeeper = ( player.getPosition() == Position.GKP);
        double subAppearanceProbability = isGoalkeeper ? 0.01 : 0.35;

        double starterMinutes = finalStartProbability *DEFAULT_STARTER_MINUTES;
        double benchMinutes = (1.0 - finalStartProbability) *subAppearanceProbability * DEFAULT_SUB_MINUTES;

        double rawProjectedMinutes = starterMinutes + benchMinutes;

        double projectedMinutes = Math.clamp(rawProjectedMinutes * availability, 0.0, 90.0);

        return new PlayerMinutesResult(
                projectedMinutes,
                finalStartProbability,
                finalPlus60Probability
        );



    }

    private double calculateSixtyPlusGivenStart(Player player, List<PlayerGameweekLog> gameweekLogs){
        if (player == null){
            return 0.0;
        }
        double prior = getPosition60MinPrior(player.getPosition());

        if(gameweekLogs == null || gameweekLogs.isEmpty()){
            return prior;
        }

        int startsCount = 0;
        int startsWith60PlusCount = 0;

        for (PlayerGameweekLog log : gameweekLogs) {
            if (log != null && log.getOutcome() != null && Boolean.TRUE.equals(log.getOutcome().getWasStarter())) {
                    startsCount++;

                    Integer minutes = log.getOutcome().getActualMinutes();
                    if (minutes != null && minutes >= 60) {
                        startsWith60PlusCount++;
                    }
                }

        }

        // Bayesian Shrinkage Formula
        double smoothedProb = (startsWith60PlusCount + (PSEUDO_STARTS_WEIGHT * prior))
                / (startsCount + PSEUDO_STARTS_WEIGHT);

        return Math.clamp(smoothedProb, 0.0, 1.0);
    }

    private double getPosition60MinPrior(Position position) {
        if (position == null) {
            return PRIOR_60_MIN_MID;
        }
        return switch (position) {
            case GKP -> PRIOR_60_MIN_GKP; // 0.98
            case DEF -> PRIOR_60_MIN_DEF; // 0.92
            case MID -> PRIOR_60_MIN_MID; // 0.82
            case FWD -> PRIOR_60_MIN_FWD; // 0.78
        };
    }
    /*
     Checks if a player is injured.
     If not returns the fpl-APIs chance of playing clamped in [0.0,1.0]
     */
    private double resolveAvailability(Player player){

        if (player == null || player.getStatus() == null) {
            return 0.0;
        }

        String status = player.getStatus();

        if (status.equals("a") || status.equals("d") || status.equals("u")){
            Integer chanceOfPlaying = player.getMarketInfo().getChanceOfPlayingThisRound();
            if ( chanceOfPlaying == null)
            {
                return 1.0;
            }
            return chanceOfPlaying / 100.00;
        }
        return 0.0;
    }

    private double calculateStartProbability(Player player, List<PlayerGameweekLog> gameweekLogs)
    {
        if (gameweekLogs == null ||gameweekLogs.isEmpty())
        {
            return calculateColdStartProbability(player);
        }


        int matchesStarted = 0;


        List<PlayerGameweekLog> recentLogs = gameweekLogs.stream()
                .filter((PlayerGameweekLog log)-> log != null && log.getFixture() != null && log.getFixture().getGameweek() != null)
                .sorted(Comparator.comparing(
                        (PlayerGameweekLog log) -> log.getFixture().getGameweek()
                ).reversed())
                .limit(3)
                .toList();

        if (recentLogs.isEmpty())
        {
            return calculateColdStartProbability(player);
        }
        double totalMinutes = 0;
        for (PlayerGameweekLog log : recentLogs){
            if (log.getOutcome() !=  null && log.getOutcome().getActualMinutes() != null)
            {
                totalMinutes += log.getOutcome().getActualMinutes();
            }
        }
        double recentRatio = Math.clamp(totalMinutes / (recentLogs.size() * 90.0) , 0.0, 1.0);

        for ( PlayerGameweekLog log: gameweekLogs){

            if (log != null && log.getOutcome() != null && Boolean.TRUE.equals(log.getOutcome().getWasStarter()))
            {
                matchesStarted++;
            }
        }
        double totalMatches = gameweekLogs.size();
        double seasonRatio = matchesStarted / totalMatches;

        double recentWeight;
        if (isTalisman(player))
        {
            recentWeight =0.35;
        }else {
            recentWeight = 0.65;
        }

        double seasonWeight= 1.0 - recentWeight;

        return Math.clamp((recentWeight * recentRatio)+(seasonWeight * seasonRatio), 0.0, 1.0);
    }

    private double calculateColdStartProbability(Player player){

        if (player == null || player.getPerformance() == null) return 0.0;

        if (player.getPerformance().getStarts() != null && player.getPerformance().getStarts() > 0){
            return player.getPerformance().getStarts()/ 38.0;
        }

        if (player.getMarketInfo() == null || player.getPosition() == null) return 0.0;

        double probability;
        double ownership = player.getMarketInfo().getSelectedByPercent();
        double currentPrice = player.getMarketInfo().getCurrentPrice();

        if(player.getPosition() == Position.GKP){
            if(currentPrice >= 4.5 && ownership>= 2.0){
                probability = 0.95;
            } else {
                if ( ownership > 10.00){
                    probability = 0.75;
                }else {
                    probability = 0.05;
                }
            }
        } else if (player.getPosition() == Position.DEF){
            if (currentPrice >= 5.0 && ownership >= 10.0)
            {
                probability = 0.90;
            } else {
                if (currentPrice >=4.5){
                    if(ownership >= 15.0){
                        probability = 0.85;
                    }else {
                        probability = 0.70;
                    }
                } else{
                    if (ownership >= 20.0){
                        probability = 0.75;
                    } else if (ownership >= 8.0) {
                        probability = 0.45;
                    }else {
                        probability = 0.15;
                    }
                }
            }
        }else if (player.getPosition() == Position.MID || player.getPosition() == Position.FWD) {
            if (currentPrice >= 7.5) {
                probability = 0.90;
            } else if (currentPrice >= 5.5) {
                if (ownership >= 15.0) {
                    probability = 0.88;
                } else {
                    probability = 0.78;
                }
            } else {
                if (ownership >= 15.0) {
                    probability = 0.65;
                } else if (ownership >= 5.0) {
                    probability = 0.40;
                } else {
                    probability = 0.15;
                }
            }
        } else {
            probability = 0.5;
        }
        return probability;
    }

    private boolean isTalisman(Player player){

        if (player == null) return false;

        double xGoalsPer90 = 0.0;
        double xAssistsPer90 =  0.0;
        if(player.getAdvancedMetrics() != null) {

            if (player.getAdvancedMetrics().getExpectedGoalsPer90() == null) {
                xGoalsPer90 = 0.0;
            } else {
                xGoalsPer90 = player.getAdvancedMetrics().getExpectedGoalsPer90();
            }


            if (player.getAdvancedMetrics().getExpectedAssistsPer90() == null) {
                xAssistsPer90 = 0.0;
            } else {
                xAssistsPer90 = player.getAdvancedMetrics().getExpectedAssistsPer90();
            }
        }
            if (player.getPenaltiesOrder() != null) {
                return xGoalsPer90 + xAssistsPer90 > 0.5 || player.getPenaltiesOrder() == 1;
            }
            return xGoalsPer90 + xAssistsPer90 > 0.5 ;

    }



}
