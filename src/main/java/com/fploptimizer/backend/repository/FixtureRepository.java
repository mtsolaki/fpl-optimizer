package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture,Integer> {

    List<Fixture> findByGameweek(Integer gameweek);

    List<Fixture> findByFinishedTrueAndGameweekLessThan(Integer gameweek);

    Optional<Fixture> findById(int id);

}
