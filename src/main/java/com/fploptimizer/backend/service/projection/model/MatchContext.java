package com.fploptimizer.backend.service.projection.model;

public record MatchContext(
        int fixtureId,
        TeamStrengthProjection homeProjection,
        TeamStrengthProjection awayProjection,
        double attackMultiplierHome,
        double attackMultiplierAway
) {
    public TeamStrengthProjection getProjectionForTeam(int teamId)
    {

        if (teamId == homeProjection.teamId()){
            return homeProjection;
        }else if (teamId == awayProjection.teamId()){
            return awayProjection;
        }else {
            throw new IllegalArgumentException("Team ID " + teamId + " does not belong to fixture " + fixtureId);
        }

    }

    public TeamStrengthProjection getOpponentProjectionForTeam(int teamId)
    {
        if (teamId == homeProjection.teamId()){
            return awayProjection;
        }else if (teamId == awayProjection.teamId()){
            return homeProjection;
        }else {
            throw new IllegalArgumentException("Team ID " + teamId + " does not belong to fixture " + fixtureId);
        }
    }

    public double getExpectedGoalsForTeam(int teamId)
    {
        if (teamId == homeProjection.teamId()){
            return homeProjection.lambda();
        } else if (teamId == awayProjection.teamId()){
            return awayProjection.lambda();
        }else {
            throw new IllegalArgumentException("Team ID " + teamId + " does not belong to fixture " + fixtureId);
        }
    }

    public double getExpectedGoalsAgainstTeam(int teamId)
    {
        if (teamId == homeProjection.teamId()){
            return awayProjection.lambda();
        }else if (teamId == awayProjection.teamId()){
            return homeProjection.lambda();
        }else {
            throw new IllegalArgumentException("Team ID " + teamId + " does not belong to fixture " + fixtureId);
        }
    }
    public double getTeamCleanSheetProbability(int teamId)
    {
        if (teamId == homeProjection.teamId()){
            return homeProjection.cleanSheetProbability();
        } else if (teamId == awayProjection.teamId()){
            return awayProjection.cleanSheetProbability();
        }else {
            throw new IllegalArgumentException("Team ID " + teamId + " does not belong to fixture " + fixtureId);
        }
    }
    public double getAttackMultiplierForTeam(int teamId){
        if (teamId == homeProjection.teamId()){
            return attackMultiplierHome;
        }else if (teamId == awayProjection.teamId()){
            return attackMultiplierAway;
        }else {
            throw new IllegalArgumentException("Team ID " + teamId + " does not belong to fixture " + fixtureId);
        }
    }
}
