export interface LineupPlayer {
    fplId: number;
    webName: string;
    teamShortName: string;
    position: 'GKP' | 'DEF' | 'MID' | 'FWD';
    currentPrice: number;
    projectedXp: number;
    isCaptain: boolean;
    isViceCaptain: boolean;
    benchOrder: number;
    status: string;
    news?: string;
    opponentShortName?: string;
    isHome?: boolean;
    difficulty?: number;
}

export interface UserLineupResponse {
    managerId: number;
    gameweek: number;
    formation: string;
    totalProjectedXp: number;
    teamValue: number;
    bank: number;
    captain: LineupPlayer;
    viceCaptain: LineupPlayer;
    starters: LineupPlayer[];
    bench: LineupPlayer[];
}

export interface VfmPlayer {
    fplId: number;
    webName: string;
    teamShortName: string;
    position: 'GKP' | 'DEF' | 'MID' | 'FWD';
    currentPrice: number;
    projectedXp: number;
    vfmScore: number;
    status?: string;
}

export interface VfmFilters {
    position?: string;
    maxPrice?: number;
}

export interface PlayerGameweekReport {
    fplId: number;
    playerCode: number;
    webName: string;
    teamShortName: string;
    position: 'GKP' | 'DEF' | 'MID' | 'FWD';
    projectedXp: number;
    projectedMinutes?: number | null;
    actualPoints?: number | null;
    actualMinutes?: number | null;
}