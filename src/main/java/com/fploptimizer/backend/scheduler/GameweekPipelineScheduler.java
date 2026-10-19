package com.fploptimizer.backend.scheduler;

import com.fploptimizer.backend.service.sync.FplDataSyncService;
import com.fploptimizer.backend.service.GameweekReconciliationService;
import com.fploptimizer.backend.service.projection.GameweekProjectionBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Orchestrator responsible for the automated lifecycle execution of the FPL data pipeline.
 *
 * <p>Coordinates recurring scheduled cron tasks and startup event listeners to guarantee that
 * data synchronization, match outcome settlements, and mathematical projections remain up to date.
 * The pipeline executes sequentially across three critical stages:
 * <ol>
 *   <li><b>Metadata Ingestion:</b> Pulls the latest static metadata and fixture statuses via {@link FplDataSyncService}.</li>
 *   <li><b>Historical Reconciliation:</b> Resolves and settles realized outcomes for finalized gameweeks via {@link GameweekReconciliationService}.</li>
 *   <li><b>Batch Projections:</b> Executes backfilling for missing completed gameweeks and generates forward-looking multi-gameweek projections via {@link GameweekProjectionBatchService}.</li>
 * </ol>
 * </p>
 *
 * @author Maria Tsolaki
 * @version 1.0
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
     * Application lifecycle hook executed immediately after the Spring application context has fully initialized.
     *
     * <p>Ensures immediate cold-start synchronization, verifying that any historical gameweeks
     * lacking projections or outcomes are reconciled without waiting for the first cron cycle.</p>
     *
     * @see ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void executeOnStartup() {
        log.info("Application initialized. Running initial pipeline synchronization...");
        executePipeline();
    }

    /**
     * Recurring cron job executed at the beginning of every hour (e.g., 12:00, 13:00, 14:00).
     *
     * <p>Maintains near real-time synchronization with official FPL price changes, injury reports,
     * fixture status updates, and completed match results.</p>
     */
    @Scheduled(cron = "0 0 * * * *")
    public void executeScheduledPipeline() {
        log.info("Starting hourly pipeline execution...");
        executePipeline();
    }

    /**
     * Orchestrates the linear, multiphase execution of the data pipeline.
     *
     * <p>Executes within an isolated exception-handling boundary to prevent uncaught runtime
     * exceptions from terminating the Spring scheduler daemon thread.</p>
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

            // 3a. Historical backfill (runs ONLY for completed GWs missing projections)
            projectionBatchService.runProjectionsForCompletedGameweeks();

            // 3b. Forward-looking multi-gameweek projections (next 5 gameweeks)
            projectionBatchService.runProjectionsForUpcomingGameweeks(5);

            log.info("Pipeline executed successfully.");
        } catch (Exception e) {
            log.error("Pipeline execution failed: {}", e.getMessage(), e);
        }
    }
}