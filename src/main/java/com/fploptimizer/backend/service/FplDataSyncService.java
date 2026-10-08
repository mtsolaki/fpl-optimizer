    package com.fploptimizer.backend.service;


    import com.fploptimizer.backend.dto.FplStaticResponseDto;
    import jakarta.annotation.PostConstruct;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import org.springframework.web.client.RestTemplate;

    @Service
    @RequiredArgsConstructor
    public class FplDataSyncService {

        private final RestTemplate restTemplate;

        @PostConstruct
        public void fetchStaticData()
        {
            String url = "https://fantasy.premierleague.com/api/bootstrap-static/";

            FplStaticResponseDto response =  restTemplate.getForObject(url, FplStaticResponseDto.class);

            System.out.println("Found " + response.getPlayers().size() + " players");

        }


    }
