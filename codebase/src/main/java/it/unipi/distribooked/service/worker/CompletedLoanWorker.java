package it.unipi.distribooked.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.model.enums.OutboxTaskType;
import it.unipi.distribooked.service.OutboxService;
import it.unipi.distribooked.utils.RedisKey;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompletedLoanWorker {

    private static final String STREAM_KEY = RedisKey.COMPLETED_LOANS_STREAM.getKey();
    private static final String CONSUMER_GROUP = "completed-loans-group";
    private static final String CONSUMER_NAME = "worker-1";

    @Autowired
    private final RedisTemplate<String, Object> redisTemplate;
    private final OutboxService outboxService;

    @PostConstruct
    public void init() {
        try {
            // Create consumer group if not exists
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP);
        } catch (Exception e) {
            log.warn("Consumer group might already exist: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processCompletedLoans() {
        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(10),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return;
            }

            log.info("Processing {} completed loans", records.size());

            for (MapRecord<String, Object, Object> record : records) {
                try {
                    String data = record.getValue().get("data").toString();
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> payload = mapper.readValue(data, Map.class);

                    // Create outbox task
                    outboxService.createTask(OutboxTaskType.ADD_READ_BOOK, payload);

                    // Acknowledge message
                    redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());

                    log.info("Created outbox task for completed loan record {}", record.getId());

                } catch (Exception e) {
                    log.error("Error processing completed loan record {}: {}", record.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error in completed loans worker: {}", e.getMessage(), e);
        }
    }
}
