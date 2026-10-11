package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.FixtureProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixtureProjectionRepository extends JpaRepository<FixtureProjection, Integer> {

    List<FixtureProjection> findByGameweek(Integer gameweek);
}
