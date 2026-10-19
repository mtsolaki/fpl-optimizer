package com.fploptimizer.backend.service.sync;

import com.fploptimizer.backend.client.FplApiClient;
import com.fploptimizer.backend.dto.*;
import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.Team;
import com.fploptimizer.backend.mapper.PlayerMapper;
import com.fploptimizer.backend.repository.FixtureRepository;
import com.fploptimizer.backend.repository.GameweekRepository;
import com.fploptimizer.backend.repository.PlayerRepository;
import com.fploptimizer.backend.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingestion service responsible for synchronizing static domain metadata from the official FPL API into the database.
 *
 * Coordinates scheduled ingestion runs to persist clubs, player rosters with enriched attributes,
 * gameweek tournament states, and the 380-fixture season schedule.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FplDataSyncService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final FixtureRepository fixtureRepository;
    private final PlayerMapper playerMapper;
    private final GameweekRepository gameweekRepository;
    private final FplApiClient fplApiClient;

    /**
     * Triggers the full static ingestion cycle.
     *
     * Ingests and batch-persists teams, players, gameweek schedule states, and fixtures
     * sequentially to respect database relational constraints.
     */
    public void fetchStaticData() {
        FplStaticResponseDto response = fplApiClient.getStaticData();

        if (response == null) {
            log.warn("Failed to fetch data from FPL API: response was null");
            return;
        }

        syncTeams(response);
        syncPlayers(response);
        syncGameweeks(response);
        syncFixtures();
    }

    /**
     * Ingests and upserts Premier League team records.
     *
     * Maps home and away overall strength indices used by downstream fixture projection models.
     *
     * @param response the static metadata payload containing the teams collection
     */
    public void syncTeams(FplStaticResponseDto response) {
        if (response == null || response.getTeams() == null) {
            return;
        }
        List<Team> teamsToSave = new ArrayList<>();

        for (TeamDto dto : response.getTeams()) {
            Team team = new Team();
            team.setId(dto.getId());
            team.setName(dto.getName());
            team.setShortName(dto.getShortName());
            team.setStrengthOverallHome(dto.getStrengthOverallHome());
            team.setStrengthOverallAway(dto.getStrengthOverallAway());

            teamsToSave.add(team);
        }

        teamRepository.saveAll(teamsToSave);
    }

    /**
     * Ingests and upserts player entities and their enriched underlying statistical metrics.
     *
     * Resolves team lazy references and utilizes PlayerMapper to construct target entities.
     *
     * @param response the static metadata payload containing the player elements collection
     */
    public void syncPlayers(FplStaticResponseDto response) {
        if (response == null || response.getPlayers() == null) {
            return;
        }

        List<Player> playersToSave = new ArrayList<>();

        for (PlayerDto dto : response.getPlayers()) {
            Team team = teamRepository.getReferenceById(dto.getTeamId());
            Player player = playerMapper.toEntity(dto, team);
            playersToSave.add(player);
        }

        playerRepository.saveAll(playersToSave);
        log.info("Successfully synced and saved {} players with enriched stats", playersToSave.size());
    }

    /**
     * Ingests and upserts the 38 gameweek tournament events.
     *
     * Captures transfer deadline timestamps, completion flags, and active gameweek statuses.
     *
     * @param response the static metadata payload containing the events/gameweeks collection
     */
    public void syncGameweeks(FplStaticResponseDto response) {
        if (response == null || response.getGameweeks() == null) {
            return;
        }
        List<Gameweek> gameweeksToSave = new ArrayList<>();

        for (GameweekDto dto : response.getGameweeks()) {
            Gameweek gameweek = new Gameweek();

            gameweek.setId(dto.getId());
            gameweek.setName(dto.getName());
            gameweek.setDeadlineTime(dto.getDeadlineTime());
            gameweek.setFinished(dto.getFinished());
            gameweek.setIsCurrent(dto.getIsCurrent());
            gameweek.setIsNext(dto.getIsNext());

            gameweeksToSave.add(gameweek);
        }
        gameweekRepository.saveAll(gameweeksToSave);
    }

    /**
     * Ingests and upserts all 380 Premier League season fixtures.
     *
     * Captures kickoff times, fixture difficulty ratings (FDR), match completion states,
     * and final scores.
     */
    public void syncFixtures() {
        FixtureDto[] fixtureDtos = fplApiClient.getFixtures();

        if (fixtureDtos == null) {
            return;
        }

        List<Fixture> fixturesToSave = new ArrayList<>();
        for (FixtureDto dto : fixtureDtos) {
            Fixture fixture = new Fixture();

            fixture.setId(dto.getId());
            fixture.setGameweek(dto.getGameweek());
            fixture.setKickoffTime(dto.getKickoffTime());
            fixture.setHomeTeamDifficulty(dto.getHomeTeamDifficulty());
            fixture.setAwayTeamDifficulty(dto.getAwayTeamDifficulty());
            fixture.setFinished(dto.getFinished());
            fixture.setStarted(dto.getStarted());
            fixture.setHomeTeamScore(dto.getHomeTeamScore());
            fixture.setAwayTeamScore(dto.getAwayTeamScore());

            fixture.setHomeTeam(teamRepository.getReferenceById(dto.getHomeTeamId()));
            fixture.setAwayTeam(teamRepository.getReferenceById(dto.getAwayTeamId()));

            fixturesToSave.add(fixture);
        }

        fixtureRepository.saveAll(fixturesToSave);
    }
}