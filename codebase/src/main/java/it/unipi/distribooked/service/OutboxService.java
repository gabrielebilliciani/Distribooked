package it.unipi.distribooked.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.exceptions.BusinessException;
import it.unipi.distribooked.exceptions.InvalidPayloadException;
import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.model.enums.OutboxTaskType;
import it.unipi.distribooked.model.enums.TaskStatus;
import it.unipi.distribooked.repository.mongo.OutboxRepository;
import it.unipi.distribooked.service.outbox.OutboxTaskProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException; // Import for optimistic locking exceptions
import org.springframework.retry.annotation.Backoff; // Import for retry annotations
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles execution of Outbox tasks.
 * Provides task creation, processing with retry mechanisms, error handling, and cleanup functionality.
 */
@Service
@Slf4j
public class OutboxService {

    @Autowired
    private OutboxRepository outboxRepository; // Repository for interacting with MongoDB

    @Autowired
    private Map<String, OutboxTaskProcessor> processors; // Map of task processors by task type

    @Autowired
    private ObjectMapper objectMapper; // ObjectMapper for serializing and deserializing payloads

    @Value("${outbox.max-retries:5}")
    private int maxRetries; // Maximum number of retries allowed for a task

    @Value("${outbox.initial-retry-delay:1}")
    private int initialRetryDelay; // Initial delay for retrying failed tasks (in seconds)

    @Value("${outbox.max-retry-delay:3600}") // 1 hour
    private int maxRetryDelay; // Maximum delay for retries (in seconds)

    private Logger logger = LoggerFactory.getLogger(OutboxService.class);

    // TODO
    // We might consider adding Circuit Breaker pattern to improve resilience
    // See Spring Cloud Circuit Breaker or Resilience4j for implementation options

    /**
     * Creates a new outbox task and saves it in the repository.
     *
     * @param type    Type of the task (e.g., notification, data sync).
     * @param payload Payload containing task-specific data.
     * @param <T>     Generic type for payload.
     */
    public <T> void createTask(OutboxTaskType type, T payload) {
        Assert.notNull(type, "Task type cannot be null");
        Assert.notNull(payload, "Task payload cannot be null");

        try {
            // Convert payload to a map for serialization
            Map<String, Object> payloadMap = objectMapper.convertValue(
                    payload,
                    new TypeReference<Map<String, Object>>() {}
            );
            // Create and save the task
            OutboxTask task = new OutboxTask(type, payloadMap);
            outboxRepository.save(task);
        } catch (IllegalArgumentException e) {
            throw new InvalidPayloadException("Failed to serialize payload", e);
        }
    }

    /**
     * Processes a task with retry logic for optimistic locking.
     *
     * @param task Task to process.
     */
    @Transactional
    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2) // Exponential backoff
    )
    public void processTask(OutboxTask task) {
        try {
            // Retrieve the latest version of the task
            OutboxTask freshTask = outboxRepository.findById(task.getId())
                    .orElseThrow(() -> new IllegalStateException("Task not found"));

            // Skip if the task is already completed
            if (freshTask.getStatus() == TaskStatus.COMPLETED) {
                log.info("Task {} already completed by another thread, skipping", task.getId());
                return;
            }

            // Mark task as in progress
            task.setStatus(TaskStatus.IN_PROGRESS);
            outboxRepository.save(task);

            // Retrieve the appropriate processor for the task type
            String processorKey = task.getType().name();
            log.info("Looking for processor with key: {}", processorKey);
            OutboxTaskProcessor processor = processors.get(processorKey);
            if (processor == null) {
                log.error("No processor found for key: {}. Available processors: {}",
                        processorKey, processors.keySet());
                throw new IllegalStateException("No processor found for type: " + processorKey);
            }

            logger.info("Processing task {} with type {}", task.getId(), task.getType());

            // Process the task
            processor.process(task);

            // Mark task as completed
            task.setStatus(TaskStatus.COMPLETED);
            outboxRepository.save(task);

        } catch (Exception e) {
            // Handle any errors that occur during processing
            handleTaskError(task, e);
        }
    }

    /**
     * Handles errors during task processing, including retries and status updates.
     *
     * @param task Task that encountered an error.
     * @param e    Exception that occurred.
     */
    private void handleTaskError(OutboxTask task, Exception e) {
        task.setErrorMessage(e.getMessage()); // Log the error message
        task.setStackTrace(ExceptionUtils.getStackTrace(e)); // Log the stack trace
        task.setRetryCount(task.getRetryCount() + 1); // Increment retry count
        task.setUpdatedAt(LocalDateTime.now()); // Update timestamp

        if (e instanceof BusinessException) {
            // Mark as failed if it's a business error
            task.setStatus(TaskStatus.FAILED);
            log.error("Business error in task {}: {}", task.getId(), e.getMessage());
        } else if (task.getRetryCount() >= maxRetries) {
            // Mark as failed after max retries
            task.setStatus(TaskStatus.FAILED);
            log.error("Task {} failed after {} retries: {}", task.getId(), maxRetries, e.getMessage());
        } else {
            // Schedule retry with exponential backoff
            Duration backoff = calculateBackoff(task.getRetryCount());
            task.setNextRetryAt(LocalDateTime.now().plus(backoff));
            task.setStatus(TaskStatus.RETRY_SCHEDULED);
            log.warn("Task {} failed, retry {}/{} scheduled in {} seconds: {}",
                    task.getId(), task.getRetryCount(), maxRetries,
                    backoff.getSeconds(), e.getMessage());
        }

        outboxRepository.save(task); // Persist task updates
    }

    /**
     * Calculates exponential backoff with jitter for retries.
     *
     * @param retryCount Current retry count.
     * @return Duration for the next retry.
     */
    private Duration calculateBackoff(int retryCount) {
        long delay = (long) (initialRetryDelay * Math.pow(2, retryCount));
        delay = Math.min(delay, maxRetryDelay); // Cap the delay to maxRetryDelay
        long jitter = Math.max(1, delay / 10); // make sure jitter is at least 1 second
        delay += ThreadLocalRandom.current().nextLong(jitter); // add the jitter
        return Duration.ofSeconds(delay);
    }


    /**
     * Periodically cleans up old completed tasks.
     * Deletes tasks older than 7 days.
     */
    @Scheduled(cron = "${outbox.cleanup.cron:0 0 * * * *}") // Default: run every hour
    public void cleanupOldTasks() {
        log.info("Starting cleanup of old outbox tasks");
        LocalDateTime threshold = LocalDateTime.now().minus(Duration.ofDays(7)); // 7-day threshold
        int deleted = outboxRepository.deleteByStatusAndCreatedAtBefore(
                TaskStatus.COMPLETED, threshold);
        log.info("Deleted {} completed tasks older than {}", deleted, threshold);
    }
}
