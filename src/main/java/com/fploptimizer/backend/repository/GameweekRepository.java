package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Gameweek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameweekRepository extends JpaRepository<Gameweek, Integer> {


    Optional<Gameweek> findByIsNextTrue();
    Optional<Gameweek> findById(int id);
}
