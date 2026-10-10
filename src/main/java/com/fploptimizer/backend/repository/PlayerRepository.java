package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player,Integer> {

    Optional<Player> findById(int id);
}
