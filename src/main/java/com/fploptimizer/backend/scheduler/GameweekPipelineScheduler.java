package com.fploptimizer.backend.scheduler;

import com.fploptimizer.backend.service.FplDataSyncService;
import com.fploptimizer.backend.service.GameweekReconciliationService;
import com.fploptimizer.backend.service.GameweekProjectionBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the recurring pipeline lifecycle:
 * 1. Synchronizes latest FPL static data and fixtures.
 * 2. Reconciles missing outcomes for finished gameweeks.
 * 3. Triggers model projections based on fresh data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameweekPipelineScheduler {

    private final FplDataSyncService fplDataSyncService;
    private final GameweekReconciliationService reconciliationService;
    private final GameweekProjectionBatchService projectionBatchService;

    @Value("${fpl.current-season:2026-2027}")
    private String currentSeason;

    /**
     * Executes immediately once the application context is fully initialized.
     * Guarantees that historical gameweeks (GW1-GW6) are backfilled on first startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void executeOnStartup() {
        log.info("Application initialized. Running initial pipeline synchronization...");
        executePipeline();
    }

    /**
     * Runs every hour at the top of the hour (e.g. 12:00, 13:00, 14:00).
     */
    @Scheduled(cron = "0 0 * * * *")
    public void executeScheduledPipeline() {
        log.info("Starting hourly pipeline execution...");
        executePipeline();
    }

    /**
     * The sequential lifecycle flow.
     */
    private void executePipeline() {
        try {
            // Step 1: Update static metadata (injuries, price changes, fixture scores, gameweek status)
            log.info("Pipeline Step 1: Synchronizing FPL static data and fixtures...");
            fplDataSyncService.fetchStaticData();

            // Step 2: Reconcile outcomes for any finished gameweeks lacking settled data
            log.info("Pipeline Step 2: Reconciling gameweek outcomes...");
            reconciliationService.reconcileMissingGameweekOutcomes(currentSeason);

            // Step 3: Projection Pipeline Trigger
             log.info("Pipeline Step 3: Triggering model projections...");

            projectionBatchService.runProjectionsForUpcomingGameweek();

            log.info("Pipeline executed successfully.");
        } catch (Exception e) {
            log.error("Pipeline execution failed: {}", e.getMessage(), e);
        }
    }
}