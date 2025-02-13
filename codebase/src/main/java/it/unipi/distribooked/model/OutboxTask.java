package it.unipi.distribooked.model;

import it.unipi.distribooked.model.enums.OutboxTaskType;
import it.unipi.distribooked.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a task in the Outbox collection. Each task corresponds to an asynchronous operation
 * that needs to be executed to ensure eventual consistency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox")
public class OutboxTask {

    @Id
    private ObjectId id;

    private OutboxTaskType type; // The type of the task (e.g., UPDATE_AUTHOR, TTL_EVENT)

    private Map<String, Object> payload; // Data specific to the task

    @Indexed // Indexing improves query performance when searching by status
    private TaskStatus status; // Status of the task (PENDING, IN_PROGRESS, COMPLETED, FAILED)

    private int retryCount; // Number of retry attempts

    private LocalDateTime createdAt; // Timestamp when the task was created

    private LocalDateTime updatedAt; // Timestamp when the task was last updated

    private LocalDateTime nextRetryAt; // Timestamp for the next retry attempt

    private String errorMessage; // Error message in case of failure

    private String stackTrace; // Stack trace for debugging failures

    @Version // Optimistic locking for concurrent updates
    private Long version;

    /**
     * Constructs a new OutboxTask with default settings.
     *
     * @param type    The type of the task.
     * @param payload The payload containing task details.
     */
    public OutboxTask(OutboxTaskType type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
        this.status = TaskStatus.PENDING;
        this.retryCount = 0;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.nextRetryAt = now;
    }

}
