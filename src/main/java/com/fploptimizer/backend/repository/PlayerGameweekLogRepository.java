package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Fixture;
import com.fploptimizer.backend.entity.Player;
import com.fploptimizer.backend.entity.PlayerGameweekLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerGameweekLogRepository extends JpaRepository<PlayerGameweekLog, Integer> {

    Optional<PlayerGameweekLog> findByPlayerAndFixtureAndSeason(Player player, Fixture fixture, String season);

    // Finds logs connectingIds
    @Query("""
        SELECT log FROM PlayerGameweekLog log
        WHERE log.player.id = :fplPlayerId
          AND log.fixture.id = :fixtureId
          AND log.season = :season
    """)
    Optional<PlayerGameweekLog> findByFplPlayerIdAndFixtureIdAndSeason(
            @Param("fplPlayerId") int fplPlayerId,
            @Param("fixtureId") int fixtureId,
            @Param("season") String season
    );

    Optional<List<PlayerGameweekLog>> findByPlayerId(Integer fplPlayerId);
}
