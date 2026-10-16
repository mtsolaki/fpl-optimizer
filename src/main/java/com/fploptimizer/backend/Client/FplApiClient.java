package com.fploptimizer.backend.Client;

import com.fploptimizer.backend.dto.FixtureDto;
import com.fploptimizer.backend.dto.FplStaticResponseDto;
import com.fploptimizer.backend.dto.FplUserPicksResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FplApiClient {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://fantasy.premierleague.com/api";

    /**
    * 1. Static Data ( teams, players, gameweeks )
    */
    public FplStaticResponseDto getStaticData() {
        String url = BASE_URL + "/bootstrap-static/";
        log.info("Fetching FPL static data from {}", url);
        return restTemplate.getForObject(url, FplStaticResponseDto.class);
    }

    /**
     * 2. Season Fixtures
     */
    public FixtureDto[] getFixtures() {
        String url = BASE_URL + "/fixtures/";
        log.info("Fetching FPL fixtures from {}", url);
        return restTemplate.getForObject(url, FixtureDto[].class);
    }

    /**
     * 3. User 15 players and bank
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
