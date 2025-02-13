package it.unipi.distribooked.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.exceptions.ReservationConflictException;
import it.unipi.distribooked.model.Reservation;
import it.unipi.distribooked.utils.LuaScriptLoader;
import it.unipi.distribooked.utils.RedisKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class ReservationRepository {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

//    @Autowired
//    private Jedis jedis;

    @Autowired
    private Pool<Jedis> jedisPool;

    private String reserveBookScriptSha;

    private String cancelBookReservationScriptSha;

    // The SHA-1 hash in Redis is a unique identifier for a Lua script.
    // When a script is loaded into Redis using SCRIPT LOAD, Redis computes its SHA-1 hash.
    // This hash can then be used with EVALSHA to execute the script without resending its content,
    // improving performance by reducing network overhead and avoiding script recompilation.
    @PostConstruct
    public void loadScripts() {
        try(Jedis jedis = jedisPool.getResource()) {
            // Load the script for reserving books
            String reserveBookScript = LuaScriptLoader.loadScript("reserve_book.lua"); // Load Lua script from file
            reserveBookScriptSha = jedis.scriptLoad(reserveBookScript); // Load Lua script into Redis
            log.info("Loaded reserve_book.lua script with SHA: {}", reserveBookScriptSha);

            // Load the script for canceling reservations
            String cancelBookReservationScript = LuaScriptLoader.loadScript("cancel_book_reservation.lua");
            cancelBookReservationScriptSha = jedis.scriptLoad(cancelBookReservationScript);
            log.info("Loaded cancel_book_reservation.lua script with SHA: {}", cancelBookReservationScriptSha);
        }
    }

    private static final int MAX_USER_RESERVATIONS = 5;
    private static final long RESERVATION_EXPIRY_DAYS = 3;

    /**
     * Reserves a book for a user in a specific library using a Lua script.
     *
     * @param userId    The ID of the user making the reservation
     * @param bookId    The ID of the book to be reserved
     * @param libraryId The ID of the library where the book is located
     * @return A Reservation object containing reservation details
     * @throws IllegalStateException if reservation fails
     */
    public Reservation reserveBook(String userId, String bookId, String libraryId, String bookTitle, String libraryName) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return executeReservation(jedis, userId, bookId, libraryId, bookTitle, libraryName);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                loadScripts();
                return executeReservation(jedis, userId, bookId, libraryId, bookTitle, libraryName);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON result: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse reservation result from Redis", e);
        }
    }

    private Reservation executeReservation(Jedis jedis, String userId, String bookId, String libraryId,
                                           String bookTitle, String libraryName) throws JsonProcessingException {
        // Construct Redis keys dynamically
        String availabilityKey = RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId);
        String userResLoansKey = RedisKey.USER_ACTIVITY.getKey(userId);
        String libraryResKey = RedisKey.LIBRARY_RESERVATIONS.getKey(libraryId);
        String userReservationField = RedisKey.USER_HASH_ENTRY.getKey(libraryId, bookId);
        String libraryReservationField = RedisKey.LIBRARY_HASH_ENTRY.getKey(userId, bookId);
        String expirationZSetKey = RedisKey.RESERVATION_ZSET_EXPIRATION.getKey();
        String zsetMember = RedisKey.ZSET_ENTRY.getKey(userId, bookId, libraryId);

        // Current timestamp and expiry calculation
        long currentTime = System.currentTimeMillis();
        long reservationExpiry = TimeUnit.DAYS.toSeconds(RESERVATION_EXPIRY_DAYS);

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(
                reserveBookScriptSha,
                List.of(
                        availabilityKey,
                        userResLoansKey,
                        libraryResKey,
                        expirationZSetKey,
                        userReservationField,
                        libraryReservationField
                ),
                List.of(
                        String.valueOf(MAX_USER_RESERVATIONS),
                        userId,
                        bookId,
                        libraryId,
                        String.valueOf(currentTime), // time in seconds
                        String.valueOf(reservationExpiry), // expiry in seconds
                        bookTitle,
                        libraryName,
                        zsetMember
                )
        );

        // Handle script result
        if (result instanceof List<?> listResult && listResult.isEmpty()) {
            log.warn("Reservation script returned an empty result");
            throw new IllegalStateException("Unexpected reservation result: empty response");
        } else if (result instanceof String jsonResult) {
            // Parse JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.readValue(jsonResult, Map.class);

            log.info("Reservation result: {}", resultMap);

            // Check for error
            if (resultMap.containsKey("err")) {
                String errorMessage = (String) resultMap.get("err");
                log.warn("Reservation failed: {}", errorMessage);
                throw new ReservationConflictException(errorMessage);
            }

            // Create and return Reservation object
            return new Reservation(
                    null,
                    new ObjectId(userId),
                    new ObjectId(bookId),
                    new ObjectId(libraryId),
                    LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(((Number) resultMap.get("reservationTime")).longValue()),
                            ZoneOffset.UTC
                    ),
                    LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(((Number) resultMap.get("expiryTime")).longValue()),
                            ZoneOffset.UTC
                    )
            );
        }

        log.error("Error reserving book {} for user {}", bookId, userId);
        throw new IllegalStateException("Unexpected reservation result");
    }

    /**
     * Cancels a reservation for a book.
     *
     * @param userId    The ID of the user.
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     * @return True if the cancellation is successful, false otherwise.
     */
    public boolean cancelReservation(String userId, String bookId, String libraryId) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return executeCancellation(jedis, userId, bookId, libraryId);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                loadScripts();
                return executeCancellation(jedis, userId, bookId, libraryId);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON result: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse cancellation result from Redis", e);
        }
    }

    private boolean executeCancellation(Jedis jedis, String userId, String bookId, String libraryId) throws JsonProcessingException {
        // Construct Redis keys dynamically
        String availabilityKey = RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId);
        String userResLoansKey = RedisKey.USER_ACTIVITY.getKey(userId);
        String libraryResKey = RedisKey.LIBRARY_RESERVATIONS.getKey(libraryId);
        String userReservationField = RedisKey.USER_HASH_ENTRY.getKey(libraryId, bookId);
        String libraryReservationField = RedisKey.LIBRARY_HASH_ENTRY.getKey(userId, bookId);
        String expirationZSetKey = RedisKey.RESERVATION_ZSET_EXPIRATION.getKey();
        String zsetMember = RedisKey.ZSET_ENTRY.getKey(userId, bookId, libraryId);

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(
                cancelBookReservationScriptSha,
                List.of(
                        availabilityKey,
                        userResLoansKey,
                        libraryResKey,
                        expirationZSetKey,
                        userReservationField,
                        libraryReservationField
                ),
                List.of(
                        userId,
                        bookId,
                        libraryId,
                        zsetMember
                )
        );

        // Handle script result
        if (result instanceof String jsonResult) {
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.readValue(jsonResult, Map.class);

            log.info("Cancellation result: {}", resultMap);

            // Check for errors
            if (resultMap.containsKey("err")) {
                String errorMessage = (String) resultMap.get("err");
                log.warn("Cancellation failed: {}", errorMessage);
                return false;
            }

            log.info("Cancellation successful for user {}, book {}, library {}", userId, bookId, libraryId);
            return true;
        }

        log.error("Unexpected result during cancellation for user {}, book {}, library {}", userId, bookId, libraryId);
        return false;
    }
}
