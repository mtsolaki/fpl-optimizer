    package com.fploptimizer.backend.service;

    import com.fploptimizer.backend.dto.FixtureDto;
    import com.fploptimizer.backend.dto.FplStaticResponseDto;
    import com.fploptimizer.backend.dto.PlayerDto;
    import com.fploptimizer.backend.dto.TeamDto;
    import com.fploptimizer.backend.entity.Fixture;
    import com.fploptimizer.backend.entity.Player;
    import com.fploptimizer.backend.entity.Team;
    import com.fploptimizer.backend.repository.FixtureRepository;
    import com.fploptimizer.backend.repository.PlayerRepository;
    import com.fploptimizer.backend.repository.TeamRepository;
    import jakarta.annotation.PostConstruct;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.web.client.RestTemplate;

    import java.util.ArrayList;
    import java.util.List;

    @Service
    @RequiredArgsConstructor
    public class FplDataSyncService {

        private final RestTemplate restTemplate;
        private final TeamRepository teamRepository;
        private final PlayerRepository playerRepository;
        private final FixtureRepository fixtureRepository;


        @PostConstruct // This function is executed automatically once - when Spring Context is initialized
        public void fetchStaticData()
        {
            String url = "https://fantasy.premierleague.com/api/bootstrap-static/";


            // HTTP GET request to the FPL endpoint and deserializes JSON response to our DTO
            FplStaticResponseDto response =  restTemplate.getForObject(url, FplStaticResponseDto.class);

            syncTeams(response);
            syncPlayers(response);
            syncFixtures();


        }

        public void syncTeams(FplStaticResponseDto response){

            List<Team> teamsToSave = new ArrayList<>();

            // Entity mapping from DTO do JPA entity
            for (TeamDto dto : response.getTeams()) {

                Team team = new Team();

                team.setId(dto.getId());
                team.setName(dto.getName());
                team.setShortName(dto.getShortName());
                team.setAttackHome(dto.getAttackHome());
                team.setDefenseHome(dto.getDefenseHome());
                team.setAttackAway(dto.getAttackAway());
                team.setDefenseAway(dto.getDefenseAway());
                team.setStrengthOverallHome(dto.getStrengthOverallHome());
                team.setStrengthOverallAway(dto.getStrengthOverallAway());

                teamsToSave.add(team);

            }
            //  batch insert/update to DB
            teamRepository.saveAll(teamsToSave);
        }

        public void syncPlayers(FplStaticResponseDto response){

            List<Player> playersToSave = new ArrayList<>();



            for  (PlayerDto dto : response.getPlayers()) {
                Player player = new Player();

                player.setId(dto.getId());
                player.setFirstName(dto.getFirstName());
                player.setLastName(dto.getLastName());
                player.setPosition(dto.getPosition());
                player.setCurrentPrice(dto.getCurrentPrice());
                player.setEpNext(dto.getEpNext());
                player.setExpectedGoals(dto.getExpectedGoals());
                player.setExpectedAssists(dto.getExpectedAssists());
                player.setChanceOfPlayingNextRound(dto.getChanceOfPlayingNextRound());
                player.setStatus(dto.getStatus());
                Team teamRef = teamRepository.getReferenceById(dto.getTeamId());
                player.setTeam(teamRef);

                playersToSave.add(player);
            }
            playerRepository.saveAll(playersToSave);
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
