package com.fploptimizer.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * Entry point and main configuration class for the FPL Optimizer Spring Boot backend application[cite: 26].
 * Bootstraps the application context, enables asynchronous scheduled cron tasks for data ingestion
 * and projection pipelines, and configures core shared infrastructure beans such as the HTTP client[cite: 26].
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
public class FplOptimizerApplication {

    /**
     * Main application entry point launching the Spring Boot execution lifecycle[cite: 26].
     * @param args runtime command-line arguments passed to the application[cite: 26]
     */
    public static void main(String[] args) {
        SpringApplication.run(FplOptimizerApplication.class, args);
    }

    /**
     * Exposes a reusable RestTemplate bean used across external HTTP client integrations[cite: 26].
     * Primarily utilized by components interacting with the official Fantasy Premier League REST API[cite: 26].
     *
     * @return a configured {@link RestTemplate} instance[cite: 26]
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}