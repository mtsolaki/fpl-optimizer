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
import java.util.Set;

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

    @Query("""
    SELECT log FROM PlayerGameweekLog log
    WHERE log.player.code IN :playerCodes
      AND log.gameweek.id = :gameweekId
""")
    List<PlayerGameweekLog> findByPlayerCodeInAndGameweekId(
            @Param("playerCodes") List<Integer> playerCodes,
            @Param("gameweekId") Integer gameweekId
    );

    Optional<List<PlayerGameweekLog>> findByPlayerId(Integer fplPlayerId);

    @Query("""
    SELECT DISTINCT l.gameweek.id
    FROM PlayerGameweekLog l
    WHERE l.outcome.actualTotalPoints IS NOT NULL
""")
    Set<Integer> findSettledGameweekIds();

    @Query("""
    SELECT l FROM PlayerGameweekLog l
    WHERE l.outcome.actualTotalPoints IS NOT NULL
      AND l.gameweek.id < :upcomingGwId
""")
    List<PlayerGameweekLog> findAllSettledLogsUpToGameweek(@Param("upcomingGwId") int upcomingGwId);

    @Query("""
    SELECT log FROM PlayerGameweekLog log
    JOIN FETCH log.player p
    WHERE log.gameweek.id = :gameweekId
      AND log.projection.projectedXp IS NOT NULL
""")
    List<PlayerGameweekLog> findAllByGameweekIdWithPlayer(@Param("gameweekId") int gameweekId);


}
