package it.unipi.distribooked.repository.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.model.UserBookActivity;
import it.unipi.distribooked.utils.RedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
@Slf4j
public class RedisUserRepository {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Map<String, UserBookActivity> getUserBookActivities(String userId) {
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash(); // Hash operations for string keys
        Map<String, String> rawData = hashOps.entries(RedisKey.USER_ACTIVITY.getKey(userId)); // Get all entries in the hash

        Map<String, UserBookActivity> result = new HashMap<>(); // Map to store deserialized activities

        ObjectMapper objectMapper = new ObjectMapper();

        for (Map.Entry<String, String> entry : rawData.entrySet()) { // Deserialize each entry
            try {
                UserBookActivity activity = objectMapper.readValue(entry.getValue(), UserBookActivity.class);
                result.put(entry.getKey(), activity);
            } catch (Exception e) {
                log.error("Error deserializing book activity for key: {}", entry.getKey(), e);
            }
        }
        return result;
    }
}
