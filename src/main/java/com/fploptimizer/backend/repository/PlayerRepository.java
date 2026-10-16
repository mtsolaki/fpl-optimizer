package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player,Integer> {

    Optional<Player> findById(int id);
    List<Player> findByTeamId(int teamId);

    @Query("SELECT p FROM Player p WHERE p.id IN :fplIds")
    List<Player> findByFplIdIn(@Param("fplIds") List<Integer> fplIds);

    @Query("SELECT p FROM Player p WHERE p.id = :fplId")
    Optional<Player> findByFplId(@Param("fplId") int fplId);
}
