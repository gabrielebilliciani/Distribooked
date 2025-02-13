package it.unipi.distribooked.service.worker;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.model.enums.OutboxTaskType;
import it.unipi.distribooked.service.OutboxService;
import it.unipi.distribooked.utils.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.HashMap;

/**
 * Worker to process expired reservations from ZSet and create Outbox Tasks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredReservationWorker {

    private static final String ZSET_KEY = RedisKey.RESERVATION_ZSET_EXPIRATION.getKey(); // Key for the ZSet

    @Autowired
    private final RedisTemplate<String, Object> redisTemplate; // RedisTemplate bean

    private final OutboxService outboxService; // Service to handle Outbox Task creation

    /**
     * Scheduled method to process expired reservations every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000) // Runs every 5 seconds
    public void processExpiredReservations() {
        long currentTime = Instant.now().getEpochSecond();

        // Retrieve all expired entries from the ZSet
        Set<Object> expiredEntries = redisTemplate.opsForZSet().rangeByScore(ZSET_KEY, 0, currentTime);

        if (expiredEntries == null || expiredEntries.isEmpty()) {
            // log.debug("No expired reservations to process");
            return;
        }

        log.info("Processing {} expired reservations", expiredEntries.size());

        for (Object entry : expiredEntries) {
            try {
                // Parse the entry to extract userId, bookId, and libraryId
                String entryStr = entry.toString();
                String[] parts = entryStr.split(":");
                String userId = parts[1];
                String bookId = parts[3];
                String libraryId = parts[5];

                // Prepare the task payload
                HashMap<String, Object> payload = new HashMap<>();
                payload.put("userId", userId);
                payload.put("bookId", bookId);
                payload.put("libraryId", libraryId);
                payload.put("eventType", "EXPIRED_RESERVATION");
                payload.put("timestamp", currentTime);

                // Create an Outbox Task
                outboxService.createTask(OutboxTaskType.INCREMENT_REDIS_AVAILABILITY, payload);

                log.info("Created Outbox Task for expired reservation: {}", entryStr);

                // Remove the entry from the ZSet only after the task is created successfully
                redisTemplate.opsForZSet().remove(ZSET_KEY, entry);
                log.info("Removed expired reservation {} from ZSet", entryStr);

            } catch (Exception e) {
                log.error("Error processing expired reservation {}: {}", entry, e.getMessage(), e);
            }
        }
    }
}
