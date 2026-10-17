package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Gameweek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameweekRepository extends JpaRepository<Gameweek, Integer> {


    Optional<Gameweek> findFirstByIsNextTrue();
    Optional<Gameweek> findFirstByIsCurrentTrue();
    Optional<Gameweek> findById(int id);

    /**
     * Retrieves all gameweeks that have completed, ordered chronologically.
     *
     * @return list of finished gameweeks
     */
    List<Gameweek> findAllByFinishedTrueOrderByIdAsc();
}
