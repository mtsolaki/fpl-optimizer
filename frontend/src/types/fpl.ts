/**
 * TypeScript domain definitions and API contract interfaces for the FPL Optimizer frontend.
 *
 * Models squad lineups, individual player performance projections, market efficiency
 * metrics (Value-For-Money), and model backtesting reports.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */

/**
 * Valid tactical position designations in Fantasy Premier League.
 */
export type TacticalPosition = 'GKP' | 'DEF' | 'MID' | 'FWD';

/**
 * Detailed representation of an individual player within an optimized matchday squad.
 */
export interface LineupPlayer {
    /** Unique official Fantasy Premier League player identifier. */
    fplId: number;
    /** Short display name shown on kits and leaderboards (e.g., "Haaland"). */
    webName: string;
    /** Three-letter short code of the player's club (e.g., "MCI", "ARS"). */
    teamShortName: string;
    /** Tactical pitch position. */
    position: TacticalPosition;
    /** Current market price in tenths of millions (e.g., 140 for £14.0m) or millions. */
    currentPrice: number;
    /** Mathematical expected points (xP) projected by the Poisson model. */
    projectedXp: number;
    /** Indicates whether the player is designated as the primary team captain (2x points). */
    isCaptain: boolean;
    /** Indicates whether the player is designated as vice-captain. */
    isViceCaptain: boolean;
    /** Substitute bench hierarchy index: 0 for backup goalkeeper, 1 to 3 for outfield substitutes. */
    benchOrder: number;
    /** Official FPL player availability status ('a' = available, 'd' = doubtful, 'i' = injured, 's' = suspended). */
    status: string;
    /** Medical update or suspension news text. */
    news?: string;
    /** Opponent club short name for the upcoming fixture. */
    opponentShortName?: string;
    /** Indicates whether the upcoming match is played at home venue. */
    isHome?: boolean;
    /** Official fixture difficulty rating (FDR 1 to 5). */
    difficulty?: number;
}

/**
 * Complete response payload returned by the team lineup advisor endpoint.
 */
export interface UserLineupResponse {
    /** Unique FPL manager team ID. */
    managerId: number;
    /** Target gameweek number evaluated by the optimization run. */
    gameweek: number;
    /** Selected optimal tactical formation structure (e.g., "3-4-3", "4-3-3"). */
    formation: string;
    /** Cumulative expected points (xP) of the starting eleven including captaincy bonus. */
    totalProjectedXp: number;
    /** Total squad financial valuation in millions. */
    teamValue: number;
    /** Remaining money in the bank (ITB) in millions. */
    bank: number;
    /** Designated starting captain. */
    captain: LineupPlayer;
    /** Designated starting vice-captain. */
    viceCaptain: LineupPlayer;
    /** Starting eleven players arranged by tactical line. */
    starters: LineupPlayer[];
    /** Reserve players arranged in substitution priority order. */
    bench: LineupPlayer[];
}

/**
 * Player record formatted for the Value-For-Money (VFM) transfer target market.
 */
export interface VfmPlayer {
    /** Unique official FPL player identifier. */
    fplId: number;
    /** Short display name. */
    webName: string;
    /** Club short name. */
    teamShortName: string;
    /** Tactical pitch position. */
    position: TacticalPosition;
    /** Current market price. */
    currentPrice: number;
    /** Mathematical expected points (xP) projected for the next fixture. */
    projectedXp: number;
    /** Efficiency score measured as points yield per million (£m) invested. */
    vfmScore: number;
    /** Availability status flag. */
    status?: string;
}

/**
 * Query filter criteria for searching the Value-For-Money market.
 */
export interface VfmFilters {
    /** Optional tactical position filter ('ALL', 'GKP', 'DEF', 'MID', 'FWD'). */
    position?: string;
    /** Optional upper price ceiling boundary. */
    maxPrice?: number;
}

/**
 * Analytical report comparing model projections with realized matchday outcomes.
 */
export interface PlayerGameweekReport {
    /** Unique external FPL player identifier. */
    fplId: number;
    /** FPL photo/code identifier. */
    playerCode: number;
    /** Display name. */
    webName: string;
    /** Club short name. */
    teamShortName: string;
    /** Tactical pitch position. */
    position: TacticalPosition;
    /** Pre-match expected points projection (xP). */
    projectedXp: number;
    /** Projected playing time in minutes. */
    projectedMinutes?: number | null;
    /** Realized actual fantasy points awarded after match completion. */
    actualPoints?: number | null;
    /** Realized actual minutes played on pitch. */
    actualMinutes?: number | null;
}