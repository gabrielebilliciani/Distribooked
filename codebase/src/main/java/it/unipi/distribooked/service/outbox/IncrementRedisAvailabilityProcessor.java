package it.unipi.distribooked.service.outbox;

import it.unipi.distribooked.model.OutboxTask;
import it.unipi.distribooked.service.notification.NotificationService;
import it.unipi.distribooked.utils.RedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("INCREMENT_REDIS_AVAILABILITY")
public class IncrementRedisAvailabilityProcessor implements OutboxTaskProcessor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false) // because I haven't implemented the NotificationService -- it's just a placeholder
    private NotificationService notificationService;

    @Override
    public void process(OutboxTask task) {
        // Retrieve the bookId and libraryId from the task payload
        String userId = (String) task.getPayload().get("userId");
        String bookId = (String) task.getPayload().get("bookId");
        String libraryId = (String) task.getPayload().get("libraryId");

        // Generate the Redis key for the book availability
        String availabilityKey = RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId);

        // Execute the increment operation
        redisTemplate.opsForValue().increment(availabilityKey);

        log.info("Incremented Redis availability for book {} in library {}", bookId, libraryId);

        // simulates the notification sending
        if (notificationService != null) {
            notificationService.sendLoanOverdueNotification(userId, bookId, libraryId);
        }

    }
}
