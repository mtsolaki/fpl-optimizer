package com.fploptimizer.backend.service;

import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.repository.GameweekRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameweekService {

    private final GameweekRepository gameweekRepository;

    /**
     * Resolves the ID of the upcoming gameweek (isNext = true).
     * Falls back to the current active gameweek if isNext is not set (e.g., end of season).
     *
     * @return the target gameweek ID
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
     * Retrieves the complete Gameweek entity for the upcoming gameweek.
     * Falls back to the current active gameweek if isNext is not present.
     *
     * @return the upcoming Gameweek entity
     * @throws NoSuchElementException if no gameweek record matches
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
     * @throws NoSuchElementException if no gameweek is marked as current
     */
    public int getCurrentGameweekId() {
        return gameweekRepository.findFirstByIsCurrentTrue()
                .map(Gameweek::getId)
                .orElseThrow(() -> new NoSuchElementException("No gameweek marked as current found in database."));
    }
}