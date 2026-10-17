import type { UserLineupResponse, VfmPlayer, VfmFilters, PlayerGameweekReport } from '../types/fpl';

const API_BASE_URL = 'http://localhost:8080/api/v1/advisor';

/**
 * Fetch optimized lineup for a given Manager ID.
 * Backend automatically calculates and applies the upcoming Gameweek.
 */
export const fetchLineup = async (teamId: number): Promise<UserLineupResponse> => {
    const response = await fetch(`${API_BASE_URL}/lineup/${teamId}`);
    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(errorBody || `Optimization failed with status ${response.status}`);
    }
    return response.json();
};

/**
 * Fetch VFM players list for the upcoming Gameweek.
 */
export const fetchVfmPlayers = async (filters?: VfmFilters): Promise<VfmPlayer[]> => {
    const queryParams = new URLSearchParams();
    if (filters?.position && filters.position !== 'ALL') {
        queryParams.append('position', filters.position);
    }
    if (filters?.maxPrice) {
        queryParams.append('maxPrice', filters.maxPrice.toString());
    }
    queryParams.append('limit', '50');

    const url = `${API_BASE_URL}/vfm?${queryParams.toString()}`;
    const response = await fetch(url);
    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(errorBody || `Failed to fetch VFM data (${response.status})`);
    }
    return response.json();
};

export const fetchGameweekAnalytics = async (gameweek?: number): Promise<PlayerGameweekReport[]> => {
    const url = gameweek
        ? `http://localhost:8080/api/v1/analytics/gameweek?gameweek=${gameweek}`
        : `http://localhost:8080/api/v1/analytics/gameweek`;

    const response = await fetch(url);
    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(errorBody || `Failed to fetch analytics report (${response.status})`);
    }
    return response.json();
};