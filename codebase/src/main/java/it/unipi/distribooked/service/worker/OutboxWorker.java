package it.unipi.distribooked.service.worker;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.model.enums.TaskStatus;
import it.unipi.distribooked.repository.mongo.OutboxRepository;
import it.unipi.distribooked.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically processes tasks in the Outbox collection.
 *
 * This worker component is responsible for fetching tasks from the outbox repository based on their status,
 * processing them using the OutboxService, and handling edge cases like stuck tasks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final OutboxService outboxService; // Service to handle task processing
    private final OutboxRepository outboxRepository; // Repository to interact with the outbox tasks in the database

    private Logger logger = LoggerFactory.getLogger(OutboxWorker.class);

    /**
     * Processes outbox tasks periodically.
     *
     * This method is triggered every 5 seconds to:
     * 1. Process tasks in the PENDING state.
     * 2. Process tasks in the RETRY_SCHEDULED state.
     * 3. Identify and reset "stuck" tasks (those stuck in progress for over 5 minutes).
     */
    @Scheduled(fixedDelay = 5000) // Runs every 5 seconds
    public void processOutboxTasks() {
        LocalDateTime now = LocalDateTime.now();

        // logger.info("Processing outbox tasks at {}", now);

        // Fetch and process tasks in the PENDING state
        List<OutboxTask> pendingTasks = outboxRepository.findProcessableTasks(TaskStatus.PENDING, now);
        pendingTasks.forEach(outboxService::processTask);

        // Fetch and process tasks in the RETRY_SCHEDULED state
        List<OutboxTask> retryScheduledTasks = outboxRepository.findProcessableTasks(TaskStatus.RETRY_SCHEDULED, now);
        retryScheduledTasks.forEach(outboxService::processTask);

        // Handle tasks that are "stuck" (in progress for more than 5 minutes)
        LocalDateTime stuckThreshold = now.minusMinutes(5);
        List<OutboxTask> stuckTasks = outboxRepository.findStuckTasks(stuckThreshold);
        stuckTasks.forEach(this::resetStuckTask);

        // logger.info("Finished processing outbox tasks at {}", LocalDateTime.now());
    }

    /**
     * Resets stuck tasks to a retriable state.
     *
     * This method identifies tasks that have been in progress for too long (over 5 minutes) and resets their status
     * to RETRY_SCHEDULED, allowing them to be retried after a short delay.
     *
     * @param task The task identified as "stuck."
     */
    private void resetStuckTask(OutboxTask task) {
        log.warn("Found stuck task {}, resetting status to RETRY_SCHEDULED", task.getId());
        task.setStatus(TaskStatus.RETRY_SCHEDULED); // Reset the status for retry
        task.setNextRetryAt(LocalDateTime.now().plusMinutes(1)); // Schedule retry in 1 minute
        outboxRepository.save(task); // Persist changes to the database
    }
}