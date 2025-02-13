package it.unipi.distribooked.service.worker;

import it.unipi.distribooked.service.OutboxService;
import it.unipi.distribooked.utils.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import it.unipi.distribooked.model.enums.OutboxTaskType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredLoanWorker {
    private static final String ZSET_KEY = RedisKey.LOAN_ZSET_EXPIRATION.getKey();

    @Autowired
    private final RedisTemplate<String, Object> redisTemplate;
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 5000)
    public void processExpiredLoans() {
        long currentTime = Instant.now().getEpochSecond();

        Set<Object> expiredEntries = redisTemplate.opsForZSet().rangeByScore(ZSET_KEY, 0, currentTime);

        if (expiredEntries == null || expiredEntries.isEmpty()) {
            return;
        }

        log.info("Processing {} expired loans", expiredEntries.size());

        for (Object entry : expiredEntries) {
            try {
                String entryStr = entry.toString();
                String[] parts = entryStr.split(":");
                String userId = parts[1];
                String bookId = parts[3];
                String libraryId = parts[5];

                HashMap<String, Object> payload = new HashMap<>();
                payload.put("userId", userId);
                payload.put("bookId", bookId);
                payload.put("libraryId", libraryId);
                payload.put("eventType", "EXPIRED_LOAN");
                payload.put("timestamp", currentTime);

                outboxService.createTask(OutboxTaskType.MARK_LOAN_OVERDUE, payload);

                log.info("Created Outbox Task for expired loan: {}", entryStr);

                redisTemplate.opsForZSet().remove(ZSET_KEY, entry);
                log.info("Removed expired loan {} from ZSet", entryStr);

            } catch (Exception e) {
                log.error("Error processing expired loan {}: {}", entry, e.getMessage(), e);
            }
        }
    }
}
