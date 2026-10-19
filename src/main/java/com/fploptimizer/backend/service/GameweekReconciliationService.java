package com.fploptimizer.backend.service;

import com.fploptimizer.backend.entity.Gameweek;
import com.fploptimizer.backend.repository.GameweekRepository;
import com.fploptimizer.backend.repository.PlayerGameweekLogRepository;
import com.fploptimizer.backend.service.sync.LiveGameweekSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Service responsible for identifying completed gameweeks lacking settled outcomes
 * and orchestrating their sequential synchronization.
 *
 * Guarantees data completeness for model evaluation and historical feature generation by detecting
 * finished rounds that have not yet ingested actual match statistics.
 *
 * @author Maria Tsolaki
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameweekReconciliationService {

    private final GameweekRepository gameweekRepository;
    private final PlayerGameweekLogRepository logRepository;
    private final LiveGameweekSyncService liveGameweekSyncService;

    /**
     * Dynamically identifies finished gameweeks without stored outcomes in the database
     * and sequentially calls LiveGameweekSyncService for each missing round.
     *
     * Invoking the sync service via Spring Bean proxy ensures that each gameweek
     * executes within its own isolated transactional boundary.
     *
     * @param season the target season identifier (e.g., "2026-2027")
     */
    public void reconcileMissingGameweekOutcomes(String season) {
        log.info("Checking for finished gameweeks with missing outcomes...");

        // 1. Retrieve all gameweeks marked as finished in the database
        List<Gameweek> finishedGameweeks = gameweekRepository.findAll().stream()
                .filter(gw -> Boolean.TRUE.equals(gw.getFinished()))
                .toList();

        if (finishedGameweeks.isEmpty()) {
            log.info("No finished gameweeks found in the database.");
            return;
        }

        // 2. Fetch IDs of gameweeks that already have settled outcomes
        Set<Integer> settledGameweekIds = logRepository.findSettledGameweekIds();

        // 3. Identify missing gameweeks in chronological ascending order (e.g., GW1 -> GW2 -> ... -> GW6)
        List<Integer> missingGameweekIds = finishedGameweeks.stream()
                .map(Gameweek::getId)
                .filter(id -> !settledGameweekIds.contains(id))
                .sorted()
                .toList();

        if (missingGameweekIds.isEmpty()) {
            log.info("All finished gameweeks are already fully reconciled.");
            return;
        }

        log.info("Found {} gameweek(s) pending outcome reconciliation: {}",
                missingGameweekIds.size(), missingGameweekIds);

        // 4. Sequential execution - independent transaction per gameweek
        for (Integer gwId : missingGameweekIds) {
            try {
                log.info("Starting outcome sync for GW {}", gwId);
                liveGameweekSyncService.syncEntireGameweek(gwId, season);
                log.info("Successfully completed outcome sync for GW {}", gwId);
            } catch (Exception e) {
                log.error("Failed to sync outcomes for GW {}: {}", gwId, e.getMessage(), e);
                break;
            }
        }

        log.info("Gameweek reconciliation process completed.");
    }
}