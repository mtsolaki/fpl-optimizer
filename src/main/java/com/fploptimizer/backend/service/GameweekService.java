package com.fploptimizer.backend.service;

import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.repository.GameweekRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Service managing gameweek state resolution and schedule retrieval.
 *
 * Provides helper lookup methods to resolve active tournament contexts, including upcoming transfer
 * deadlines, currently running fixtures, and fallback gameweek transitions.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameweekService {

    private final GameweekRepository gameweekRepository;

    /**
     * Resolves the ID of the upcoming active gameweek (isNext = true).
     *
     * Falls back to the current active gameweek (isCurrent = true) if isNext is not set,
     * such as during the final round of the campaign.
     *
     * @return the target upcoming gameweek ID
     * @throws NoSuchElementException if neither next nor current gameweek exists in the database
     */
    public int getNextGameweekId() {
        return gameweekRepository.findFirstByIsNextTrue()
                .map(Gameweek::getId)
                .orElseGet(() -> {
                    log.warn("No gameweek found with isNext=true. Falling back to isCurrent=true.");
                    return gameweekRepository.findFirstByIsCurrentTrue()
                            .map(Gameweek::getId)
                            .orElseThrow(() -> new NoSuchElementException("No active or next gameweek found in database."));
                });
    }

    /**
     * Retrieves the complete Gameweek entity for the upcoming round.
     *
     * Falls back to the current active gameweek if isNext is not present.
     *
     * @return the upcoming Gameweek entity
     * @throws NoSuchElementException if no active gameweek record matches
     */
    public Gameweek getNextGameweek() {
        return gameweekRepository.findFirstByIsNextTrue()
                .orElseGet(() -> {
                    log.warn("No gameweek found with isNext=true. Falling back to isCurrent=true.");
                    return gameweekRepository.findFirstByIsCurrentTrue()
                            .orElseThrow(() -> new NoSuchElementException("No active or next gameweek found in database."));
                });
    }

    /**
     * Resolves the ID of the current in-progress gameweek (isCurrent = true).
     *
     * @return the current gameweek ID
     * @throws NoSuchElementException if no gameweek is currently marked as active
     */
    public int getCurrentGameweekId() {
        return gameweekRepository.findFirstByIsCurrentTrue()
                .map(Gameweek::getId)
                .orElseThrow(() -> new NoSuchElementException("No gameweek marked as current found in database."));
    }
}