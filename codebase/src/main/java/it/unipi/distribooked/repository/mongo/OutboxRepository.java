package it.unipi.distribooked.repository.mongo;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.model.enums.TaskStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for accessing the Outbox collection.
 */
@Repository
public interface OutboxRepository extends MongoRepository<OutboxTask, ObjectId> {

    /**
     * Finds Outbox tasks that are ready to be processed.
     *
     * This query searches for documents in the Outbox collection that meet the following criteria:
     *  - `status`: Matches the provided `TaskStatus` (e.g., PENDING, RETRY_SCHEDULED).
     *  - `nextRetryAt`: Is less than or equal to the current time (`LocalDateTime now`).
     *
     * This essentially retrieves tasks that are either waiting for their first attempt
     * or are scheduled for retry after a previous failure.
     *
     * @param status The task status to search for (e.g., PENDING, RETRY_SCHEDULED).
     * @param now The current date and time.
     * @return A list of OutboxTask objects that match the criteria.
     */
    @Query("{ 'status': ?0, 'nextRetryAt': { $lte: ?1 } }")
    List<OutboxTask> findProcessableTasks(TaskStatus status, LocalDateTime now);

    /**
     * Finds "stuck" Outbox tasks that haven't been updated for a certain period.
     *
     * This query searches for documents in the Outbox collection that meet the following criteria:
     *  - `status`: Matches 'IN_PROGRESS'.
     *  - `updatedAt`: Is less than or equal to a threshold (`LocalDateTime threshold`).
     *
     * This helps identify tasks that might be stuck due to errors or long-running operations.
     * The `threshold` parameter allows you to define how long a task can be in progress
     * before it's considered stuck.
     *
     * @param threshold The threshold date and time to identify stuck tasks.
     * @return A list of OutboxTask objects that are stuck (in progress for too long).
     */
    @Query("{ 'status': 'IN_PROGRESS', 'updatedAt': { $lte: ?0 } }")
    List<OutboxTask> findStuckTasks(LocalDateTime threshold);

    int deleteByStatusAndCreatedAtBefore(TaskStatus status, LocalDateTime threshold);
}