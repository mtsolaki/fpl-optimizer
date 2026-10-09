package com.fploptimizer.backend.repository;

import com.fploptimizer.backend.entity.PlayerGameweekLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerGameweekLogRepository extends JpaRepository<PlayerGameweekLog, Integer> {

}
