package com.fploptimizer.backend.client;

import com.fploptimizer.backend.dto.FixtureDto;
import com.fploptimizer.backend.dto.FplStaticResponseDto;
import com.fploptimizer.backend.dto.FplUserPicksResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
/**
 * HTTP client responsible for integrating with the official Fantasy Premier League (FPL) REST API.
 *
 * <p>Serves as the primary ingress component for retrieving core domain datasets, including
 * bootstrap-static platform data (players, teams, gameweek schedules), season fixtures,
 * and individual manager squad compositions (picks).</p>
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FplApiClient {

    private final RestTemplate restTemplate;
    /**
     * Base URL for the official Fantasy Premier League REST API endpoints.
     */
    private static final String BASE_URL = "https://fantasy.premierleague.com/api";

    /**
     * Fetches the core bootstrap-static metadata payload from the official FPL API.
     *
     * <p>This comprehensive dataset contains general game information, including active
     * gameweek states, all registered Premier League teams, and complete player rosters
     * with their associated cost and summary metrics.</p>
     *
     * <b>Endpoint:</b> {@code GET https://fantasy.premierleague.com/api/bootstrap-static/}
     *
     * @return an {@link FplStaticResponseDto} containing teams, elements (players), and events (gameweeks)
     * @throws org.springframework.web.client.RestClientException if network communication or deserialization fails
     */
    public FplStaticResponseDto getStaticData() {
        String url = BASE_URL + "/bootstrap-static/";
        log.info("Fetching FPL static data from {}", url);
        return restTemplate.getForObject(url, FplStaticResponseDto.class);
    }

    /**
     * Retrieves the complete season fixtures schedule and match status data.
     *
     * <p>Returns all 380 scheduled Premier League fixtures, including match difficulty ratings (FDR),
     * scheduled kickoff times, team pairings, and completed match scores.</p>
     *
     * <b>Endpoint:</b> {@code GET https://fantasy.premierleague.com/api/fixtures/}
     *
     * @return an array of {@link FixtureDto} representing all season fixtures
     * @throws org.springframework.web.client.RestClientException if the remote endpoint returns an error
     */
    public FixtureDto[] getFixtures() {
        String url = BASE_URL + "/fixtures/";
        log.info("Fetching FPL fixtures from {}", url);
        return restTemplate.getForObject(url, FixtureDto[].class);
    }

    /**
     * Retrieves the 15-player squad selection and active chip details for a given manager and gameweek.
     *
     * <p>Captures team selection details such as starting positions, bench order, captaincy/vice-captaincy
     * selections, and available in-the-bank finances. If the entry identifier does not exist or has not
     * submitted picks for the requested gameweek, a graceful {@link Optional#empty()} is returned.</p>
     *
     * <b>Endpoint:</b> {@code GET https://fantasy.premierleague.com/api/entry/{teamId}/event/{gameweek}/picks/}
     *
     * @param teamId   the unique FPL manager/entry identifier
     * @param gameweek the gameweek event ID for which to fetch squad picks
     * @return an {@link Optional} containing the {@link FplUserPicksResponse} if found, or {@link Optional#empty()}
     *         if the team or gameweek picks do not exist (HTTP 404)
     * @throws RuntimeException if an unexpected network or client error occurs during extraction
     */
    public Optional<FplUserPicksResponse> getUserPicks(int teamId, int gameweek) {
        String url = String.format("%s/entry/%d/event/%d/picks/", BASE_URL, teamId, gameweek);
        log.info("Fetching user picks for team {} on GW {}", teamId, gameweek);

        try {
            FplUserPicksResponse response = restTemplate.getForObject(url, FplUserPicksResponse.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound _) {
            log.warn("Team ID {} not found or gameweek {} has no picks", teamId, gameweek);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch user picks for team {}: {}", teamId, e.getMessage());
            throw new RuntimeException("Error fetching picks from FPL API", e);
        }
    }
}
