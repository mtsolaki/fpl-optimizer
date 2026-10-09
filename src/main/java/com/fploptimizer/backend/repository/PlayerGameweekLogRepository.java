package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerGameweekLogRepository extends JpaRepository<PlayerGameweekLog, Integer> {
    Optional<PlayerGameweekLog> findByPlayerAndFixtureAndSeason(Player player, Fixture fixture, String season);
}
