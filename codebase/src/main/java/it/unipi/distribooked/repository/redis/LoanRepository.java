package it.unipi.distribooked.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.dto.LoanDTO;
import it.unipi.distribooked.dto.OverdueLoanDTO;
import it.unipi.distribooked.dto.ReservationDTO;
import it.unipi.distribooked.utils.LuaScriptLoader;
import it.unipi.distribooked.utils.RedisKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import  redis.clients.jedis.util.Pool;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for managing loans and reservations in Redis.
 * This class handles operations like retrieving loans, marking reservations as loans, and completing loans.
 */
@Slf4j
@Repository
public class LoanRepository {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

//    @Autowired
//    private Jedis jedis;

    @Autowired
    private Pool<Jedis> jedisPool;

    private String markAsLoanScriptSha;

    private String completeLoanScriptSha;

    private static final int LOAN_DURATION_SECONDS = 30 * 24 * 60 * 60; // 30 days

    // The SHA-1 hash in Redis is a unique identifier for a Lua script.
    // When a script is loaded into Redis using SCRIPT LOAD, Redis computes its SHA-1 hash.
    // This hash can then be used with EVALSHA to execute the script without resending its content,
    // improving performance by reducing network overhead and avoiding script recompilation.
    @PostConstruct
    public void loadScripts() {
        try(Jedis jedis = jedisPool.getResource()) {
            // Load the script for marking reservations as loans
            String markAsLoanScript = LuaScriptLoader.loadScript("mark_reservation_as_loan.lua");
            markAsLoanScriptSha = jedis.scriptLoad(markAsLoanScript);
            log.info("Loaded mark_reservation_as_loan.lua script with SHA: {}", markAsLoanScriptSha);

            // Load the script for completing loans
            String completeLoanScript = LuaScriptLoader.loadScript("complete_loan.lua");
            completeLoanScriptSha = jedis.scriptLoad(completeLoanScript);
            log.info("Loaded complete_loan.lua script with SHA: {}", completeLoanScriptSha);
        }
    }

    /**
     * Retrieves all loans for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of all book IDs currently on loan in the library.
     */
    public List<LoanDTO> getAllLoans(String libraryId) {
        String loansKey = RedisKey.LIBRARY_LOANS.getKey(libraryId);
        try {
            Boolean exists = redisTemplate.hasKey(loansKey);
            if (exists == null || !exists) {
                log.info("No loans found for library {}", libraryId);
                return Collections.emptyList();
            }

            Map<Object, Object> loansMap = redisTemplate.opsForHash().entries(loansKey);
            if (loansMap.isEmpty()) {
                log.info("Empty loans hash for library {}", libraryId);
                return Collections.emptyList();
            }

            return loansMap.entrySet().stream()
                    .map(entry -> {
                        String key = entry.getKey().toString();
                        String value = entry.getValue().toString();
                        try {
                            String[] parts = key.split(":");
                            if (parts.length >= 4) {
                                String userId = parts[1];
                                String bookId = parts[3];

                                // Parse the loan timestamp from the value
                                long loanedAtMillis = Long.parseLong(value);
                                LocalDateTime loanedAt = LocalDateTime.ofInstant(
                                        Instant.ofEpochMilli(loanedAtMillis),
                                        ZoneId.systemDefault());

                                // Get the remaining TTL for this specific field
                                Long remainingTTL = redisTemplate.execute((RedisConnection connection) -> {
                                    Object result = connection.execute("HPTTL",
                                            loansKey.getBytes(),
                                            "FIELDS".getBytes(),
                                            "1".getBytes(),
                                            key.getBytes());

                                    if (result instanceof List<?> list && !list.isEmpty()) {
                                        Object ttlValue = list.get(0);
                                        if (ttlValue instanceof Number) {
                                            return ((Number) ttlValue).longValue();
                                        }
                                    }
                                    return null;
                                });

                                // Calculate due date based on remaining TTL
                                LocalDateTime dueDate;
                                if (remainingTTL != null && remainingTTL > 0) {
                                    dueDate = LocalDateTime.now()
                                            .plus(remainingTTL, ChronoUnit.MILLIS);
                                } else {
                                    log.warn("No valid TTL found for loan key {} in library {}",
                                            key, libraryId);
                                    return null;
                                }

                                return new LoanDTO(
                                        userId,
                                        bookId,
                                        libraryId,
                                        loanedAt,
                                        dueDate
                                );
                            }
                            log.warn("Invalid key format for loan in library {}: {}",
                                    libraryId, key);
                            return null;
                        } catch (Exception e) {
                            log.error("Error processing loan entry in library {}: {} - {}",
                                    libraryId, key, e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error retrieving loans for library {}: {}",
                    libraryId, e.getMessage());
            throw new IllegalStateException(
                    "Error retrieving loans for library ID: " + libraryId, e);
        }
    }

    /**
     * Retrieves all reservations for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of all book IDs currently reserved in the library.
     */
    public List<ReservationDTO> getAllReservations(String libraryId) {
        String reservationsKey = RedisKey.LIBRARY_RESERVATIONS.getKey(libraryId);
        try {
            Boolean exists = redisTemplate.hasKey(reservationsKey);
            if (exists == null || !exists) {
                log.info("No reservations found for library {}", libraryId);
                return Collections.emptyList();
            }

            Map<Object, Object> reservationsMap = redisTemplate.opsForHash().entries(reservationsKey);
            if (reservationsMap.isEmpty()) {
                log.info("Empty reservations hash for library {}", libraryId);
                return Collections.emptyList();
            }

            return reservationsMap.entrySet().stream()
                    .map(entry -> {
                        String key = entry.getKey().toString();
                        String value = entry.getValue().toString();
                        try {
                            String[] parts = key.split(":");
                            if (parts.length >= 4) {
                                String userId = parts[1];
                                String bookId = parts[3];

                                // Parse the reservation timestamp from the value
                                long reservedAtMillis = Long.parseLong(value);
                                LocalDateTime reservedAt = LocalDateTime.ofInstant(
                                        Instant.ofEpochMilli(reservedAtMillis),
                                        ZoneId.systemDefault());

                                // Get the remaining TTL for this specific field
                                Long remainingTTL = redisTemplate.execute((RedisConnection connection) -> {
                                    Object result = connection.execute("HPTTL",
                                            reservationsKey.getBytes(),
                                            "FIELDS".getBytes(),
                                            "1".getBytes(),
                                            key.getBytes());

                                    if (result instanceof List<?> list && !list.isEmpty()) {
                                        Object ttlValue = list.get(0);
                                        if (ttlValue instanceof Number) {
                                            return ((Number) ttlValue).longValue();
                                        }
                                    }
                                    return null;
                                });

                                // Calculate expiration time based on remaining TTL
                                LocalDateTime expiresAt;
                                if (remainingTTL != null && remainingTTL > 0) {
                                    // Calculate expiration time by adding remaining TTL to current time
                                    expiresAt = LocalDateTime.now()
                                            .plus(remainingTTL, ChronoUnit.MILLIS);
                                } else {
                                    // If no TTL found or expired, log warning and skip
                                    log.warn("No valid TTL found for reservation key {} in library {}",
                                            key, libraryId);
                                    return null;
                                }

                                return new ReservationDTO(
                                        userId,
                                        bookId,
                                        libraryId,
                                        reservedAt,
                                        expiresAt
                                );
                            }
                            log.warn("Invalid key format for reservation in library {}: {}",
                                    libraryId, key);
                            return null;
                        } catch (Exception e) {
                            log.error("Error processing reservation entry in library {}: {} - {}",
                                    libraryId, key, e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error retrieving reservations for library {}: {}",
                    libraryId, e.getMessage());
            throw new IllegalStateException(
                    "Error retrieving reservations for library ID: " + libraryId, e);
        }
    }

    /**
     * Retrieves all overdue loans for a specific library.
     *
     * @param libraryId The ID of the library.
     * @return A list of overdue loans (keys of loans that are overdue).
     */
    public List<OverdueLoanDTO> getOverdueLoans(String libraryId) {
        String loansKey = RedisKey.LIBRARY_OVERDUE.getKey(libraryId);
        try {
            Boolean exists = redisTemplate.hasKey(loansKey);
            if (exists == null || !exists) {
                log.info("No loans found for library {}", libraryId);
                return Collections.emptyList();
            }

            Map<Object, Object> loansMap = redisTemplate.opsForHash().entries(loansKey);
            if (loansMap.isEmpty()) {
                log.info("Empty loans hash for library {}", libraryId);
                return Collections.emptyList();
            }

            List<OverdueLoanDTO> overdueLoans = new ArrayList<>();

            log.info("Found {} loans in library {}", loansMap.size(), libraryId);

            for (Map.Entry<Object, Object> entry : loansMap.entrySet()) {
                String key = entry.getKey().toString();
                try {
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        String userId = parts[1];
                        String bookId = parts[3];

                        // Get the TTL for this specific field
                        Long remainingTTL = redisTemplate.execute((RedisConnection connection) -> {
                            Object result = connection.execute("HPTTL",
                                    loansKey.getBytes(),
                                    "FIELDS".getBytes(),
                                    "1".getBytes(),
                                    key.getBytes());

                            if (result instanceof List<?> list && !list.isEmpty()) {
                                Object ttlValue = list.get(0);
                                if (ttlValue instanceof Number) {
                                    return ((Number) ttlValue).longValue();
                                }
                            }
                            return null;
                        });

                        // If TTL is null or <= 0, the loan is overdue
                        if (remainingTTL == null || remainingTTL <= 0) {
                            // Calculate the original due date from the loan value and loan duration
                            String value = entry.getValue().toString();
                            long loanedAtMillis = Long.parseLong(value);
                            LocalDateTime loanedAt = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(loanedAtMillis),
                                    ZoneId.systemDefault());

                            // Add loan duration (e.g., 30 days) to get the original due date
                            LocalDateTime dueDate = loanedAt.plus(30, ChronoUnit.DAYS); // Adjust duration as needed

                            overdueLoans.add(new OverdueLoanDTO(
                                    userId,
                                    bookId,
                                    libraryId,
                                    dueDate
                            ));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing overdue loan entry in library {}: {} - {}",
                            libraryId, key, e.getMessage(), e);
                    // Continue processing other entries
                }
            }

            return overdueLoans;

        } catch (Exception e) {
            log.error("Error retrieving overdue loans for library {}: {}",
                    libraryId, e.getMessage());
            throw new IllegalStateException(
                    "Error retrieving overdue loans for library ID: " + libraryId, e);
        }
    }


    /**
     * Converts a reservation to a loan for a user in a specific library.
     *
     * @param libraryId The ID of the library.
     * @param userId    The ID of the user.
     * @param bookId    The ID of the book.
     * @return True if the operation is successful, false otherwise.
     */
    public boolean markAsLoan(String libraryId, String userId, String bookId) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return executeMarkAsLoan(jedis, libraryId, userId, bookId);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                loadScripts();
                return executeMarkAsLoan(jedis, libraryId, userId, bookId);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON result: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse loan conversion result from Redis", e);
        }
    }

    private boolean executeMarkAsLoan(Jedis jedis, String libraryId, String userId, String bookId) throws JsonProcessingException {
        // Construct Redis keys dynamically
        List<String> keys = List.of(
                RedisKey.USER_ACTIVITY.getKey(userId),
                RedisKey.LIBRARY_RESERVATIONS.getKey(libraryId),
                RedisKey.LIBRARY_LOANS.getKey(libraryId),
                RedisKey.RESERVATION_ZSET_EXPIRATION.getKey(),
                RedisKey.LOAN_ZSET_EXPIRATION.getKey(),
                RedisKey.USER_HASH_ENTRY.getKey(libraryId, bookId),
                RedisKey.LIBRARY_HASH_ENTRY.getKey(userId, bookId)
        );

        // Prepare arguments for the script
        List<String> args = List.of(
                userId,
                bookId,
                libraryId,
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(LOAN_DURATION_SECONDS)
        );

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(markAsLoanScriptSha, keys, args);

        // Handle script result
        if (result instanceof String jsonResult) {
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.readValue(jsonResult, Map.class);

            log.info("Loan conversion result: {}", resultMap);

            // Check for errors
            if (resultMap.containsKey("err")) {
                String errorMessage = (String) resultMap.get("err");
                log.warn("Loan conversion failed: {}", errorMessage);
                throw new IllegalStateException(errorMessage);
            }

            log.info("Loan successfully registered for user {}, book {}, library {}", userId, bookId, libraryId);
            return true;
        }

        log.error("Unexpected result type from Lua script");
        throw new IllegalStateException("Unexpected result type from Lua script");
    }


    /**
     * Completes a loan by removing it and incrementing availability.
     *
     * @param libraryId The ID of the library.
     * @param userId    The ID of the user.
     * @param bookId    The ID of the book.
     * @return True if the operation is successful, false otherwise.
     */
    public boolean completeLoan(String libraryId, String userId, String bookId) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return executeCompleteLoan(jedis, libraryId, userId, bookId);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                loadScripts();
                return executeCompleteLoan(jedis, libraryId, userId, bookId);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON result: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse loan completion result from Redis", e);
        }
    }

    private boolean executeCompleteLoan(Jedis jedis, String libraryId, String userId, String bookId) throws JsonProcessingException {
        // Construct Redis keys dynamically
        List<String> keys = List.of(
                RedisKey.USER_ACTIVITY.getKey(userId),
                RedisKey.LIBRARY_LOANS.getKey(libraryId),
                RedisKey.LIBRARY_OVERDUE.getKey(libraryId),
                RedisKey.LOAN_ZSET_EXPIRATION.getKey(),
                RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId),
                RedisKey.USER_HASH_ENTRY.getKey(libraryId, bookId),
                RedisKey.LIBRARY_HASH_ENTRY.getKey(userId, bookId),
                RedisKey.COMPLETED_LOANS_STREAM.getKey()
        );

        // Prepare arguments for the script
        List<String> args = List.of(
                userId,
                bookId,
                libraryId
        );

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(completeLoanScriptSha, keys, args);

        // Handle script result
        if (result instanceof String jsonResult) {
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.readValue(jsonResult, Map.class);

            log.info("Loan completion result: {}", resultMap);

            // Check for errors
            if (resultMap.containsKey("err")) {
                log.warn("Loan completion failed: {}", resultMap.get("err"));
                return false;
            }

            log.info("Loan successfully completed for user {}, book {}, library {}", userId, bookId, libraryId);
            return true;
        }

        log.error("Unexpected result type from Lua script during loan completion");
        return false;
    }
}
