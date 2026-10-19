import type { UserLineupResponse, VfmPlayer, VfmFilters, PlayerGameweekReport } from '../types/fpl';

/** Base URL for backend advisor and decision-support endpoints. */
const API_BASE_URL = 'http://localhost:8080/api/v1/advisor';

/** Base URL for model evaluation and historical analytics endpoints. */
const ANALYTICS_BASE_URL = 'http://localhost:8080/api/v1/analytics';

/**
 * Fetches the mathematically optimized matchday squad for a given FPL manager ID.
 *
 * Communicates with the Spring Boot backend to retrieve the optimal formation,
 * starting eleven, bench ordering, and captaincy designation for the next upcoming gameweek.
 *
 * @param teamId - The unique FPL entry identifier of the manager squad.
 * @returns Promise resolving to the complete UserLineupResponse payload.
 * @throws Error if network transport fails or the backend returns an error status code.
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
 * Fetches the top-performing players ranked by Value For Money (VFM) efficiency.
 *
 * @param filters - Optional query constraints including tactical position and maximum price cap.
 * @returns Promise resolving to an array of ranked VfmPlayer records.
 * @throws Error if the remote server fails to respond.
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

/**
 * Fetches aggregated model projections and realized matchday outcomes for backtesting and auditing.
 *
 * @param gameweek - Optional gameweek number; if omitted, defaults to the upcoming round.
 * @returns Promise resolving to an array of PlayerGameweekReport records.
 * @throws Error if the analytics endpoint returns an error.
 */
export const fetchGameweekAnalytics = async (gameweek?: number): Promise<PlayerGameweekReport[]> => {
    const url = gameweek
        ? `${ANALYTICS_BASE_URL}/gameweek?gameweek=${gameweek}`
        : `${ANALYTICS_BASE_URL}/gameweek`;

    const response = await fetch(url);
    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(errorBody || `Failed to fetch analytics report (${response.status})`);
    }
    return response.json();
};