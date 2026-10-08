package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture,Integer> {
}
