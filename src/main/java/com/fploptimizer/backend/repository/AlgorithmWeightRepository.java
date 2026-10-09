package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.AlgorithmWeight;
import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlgorithmWeightRepository extends JpaRepository<AlgorithmWeight, Integer> {

    boolean existsByGameweekAndSeason(Gameweek gameweek, String season);

    Optional<AlgorithmWeight> findByPositionAndGameweekAndSeason(Position position, Gameweek gameweek, String season);

    List<AlgorithmWeight> findByGameweekAndSeason(Gameweek gameweek, String season);


}
