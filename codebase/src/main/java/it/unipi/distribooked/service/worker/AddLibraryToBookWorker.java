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
public class AddLibraryToBookWorker {
    private static final String STREAM_KEY = RedisKey.ADD_LIBRARY_STREAM.getKey();
    private static final String CONSUMER_GROUP = "add-library-group";
    private static final String CONSUMER_NAME = "worker-1";

    private final RedisTemplate<String, Object> redisTemplate;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            // create the consumer group if it does not exist already
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP);
        } catch (Exception e) {
            log.warn("Consumer group might already exist: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000) // execute every 5 seconds
    public void processAddLibraryToBook() {
        try {
            // read messages from the stream
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(10), // read up to 10 messages at a time
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                try {
                    // extract the payload from the message
                    String data = record.getValue().get("data").toString();
                    Map<String, Object> payload = objectMapper.readValue(data, Map.class);

                    // convert the initialValue to an integer
                    if (payload.containsKey("initialValue")) {
                        payload.put("initialValue", Integer.parseInt(payload.get("initialValue").toString()));
                    }

                    // create a new outbox task for the MongoDB update
                    outboxService.createTask(OutboxTaskType.ADD_LIBRARY_TO_BOOK, payload);

                    // acknowledge the message
                    redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, record.getId());

                    log.info("Created outbox task for add library to book record {}", record.getId());

                } catch (Exception e) {
                    log.error("Error processing add library to book record {}: {}",
                            record.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error in add library to book worker: {}", e.getMessage(), e);
        }
    }
}