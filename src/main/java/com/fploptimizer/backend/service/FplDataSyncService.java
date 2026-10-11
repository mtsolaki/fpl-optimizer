    package com.fploptimizer.backend.service;

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
    import jakarta.annotation.PostConstruct;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;
    import org.springframework.web.client.RestTemplate;

    import java.util.ArrayList;
    import java.util.List;
    /*
    Service used for getting the necessary data from JSONs to DBs
     */
    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class FplDataSyncService {

        private final RestTemplate restTemplate;
        private final TeamRepository teamRepository;
        private final PlayerRepository playerRepository;
        private final FixtureRepository fixtureRepository;
        private final PlayerMapper playerMapper;
        private final GameweekRepository gameweekRepository;

        @PostConstruct // This function is executed automatically once - when Spring Context is initialized
        public void fetchStaticData()
        {
            String url = "https://fantasy.premierleague.com/api/bootstrap-static/";


            // HTTP GET request to the FPL endpoint and deserializes JSON response to our DTO
            FplStaticResponseDto response =  restTemplate.getForObject(url, FplStaticResponseDto.class);

            if (response == null) {
                log.warn("Failed to fetch data from FPL API: response was null");
                return;
            }

            syncTeams(response);
            syncPlayers(response);
            syncGameweeks(response);
            syncFixtures();


        }

        public void syncTeams(FplStaticResponseDto response){

            if (response == null || response.getTeams() == null) {
                return;
            }
            List<Team> teamsToSave = new ArrayList<>();

            // Entity mapping from DTO do JPA entity
            for (TeamDto dto : response.getTeams()) {


                Team team = new Team();
                team.setId(dto.getId());
                team.setName(dto.getName());
                team.setShortName(dto.getShortName());
                team.setStrengthOverallHome(dto.getStrengthOverallHome());
                team.setStrengthOverallAway(dto.getStrengthOverallAway());

                teamsToSave.add(team);
            }
            //  batch insert/update to DB
            teamRepository.saveAll(teamsToSave);
        }

        public void syncPlayers(FplStaticResponseDto response){

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

        public void syncGameweeks(FplStaticResponseDto response){

            if (response == null || response.getGameweeks() == null) {
                return;
            }
            List<Gameweek> gameweeksToSave = new ArrayList<>();

            for(GameweekDto dto : response.getGameweeks()){
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

        public void syncFixtures(){
            String url = "https://fantasy.premierleague.com/api/fixtures/";
            FixtureDto[] fixtureDtos = restTemplate.getForObject(url, FixtureDto[].class);

            if(fixtureDtos == null)
            {
                return;
            }

            List<Fixture>  fixturesToSave = new ArrayList<>();
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
