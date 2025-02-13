package it.unipi.distribooked.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.model.enums.OutboxTaskType;
import it.unipi.distribooked.service.OutboxService;
import it.unipi.distribooked.utils.RedisKey;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DecrementCopiesWorker {
    private static final String STREAM_KEY = RedisKey.DECREMENT_COPIES_STREAM.getKey();
    private static final String CONSUMER_GROUP = "decrement-copies-group";
    private static final String CONSUMER_NAME = "worker-1";

    private final RedisTemplate<String, Object> redisTemplate;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP);
        } catch (Exception e) {
            log.warn("Consumer group might already exist: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processDecrementCopies() {
        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(10),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                try {
                    String data = record.getValue().get("data").toString();
                    Map<String, Object> payload = objectMapper.readValue(data, Map.class);

                    // Create outbox task for MongoDB update
                    outboxService.createTask(OutboxTaskType.DECREMENT_BOOK_COPIES, payload);

                    // Acknowledge message
                    redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());

                    log.info("Created outbox task for decrement copies record {}", record.getId());

                } catch (Exception e) {
                    log.error("Error processing decrement copies record {}: {}",
                            record.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error in decrement copies worker: {}", e.getMessage(), e);
        }
    }
}
