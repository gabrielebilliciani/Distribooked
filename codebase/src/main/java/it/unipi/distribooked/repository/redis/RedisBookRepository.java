package it.unipi.distribooked.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.exceptions.CannotRemoveBookException;
import it.unipi.distribooked.exceptions.LibraryEntryAlreadyExistsException;
import it.unipi.distribooked.exceptions.NoAvailableCopiesException;
import it.unipi.distribooked.utils.LuaScriptLoader;
import it.unipi.distribooked.utils.RedisKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisNoScriptException;
import redis.clients.jedis.util.Pool;

import java.util.*;

@Slf4j
@Repository
public class RedisBookRepository {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

//    @Autowired
//    private Jedis jedis;

    @Autowired
    private Pool<Jedis> jedisPool;

    private String removeBookSha;
    private String decrementCopiesScriptSha;
    private String incrementCopiesScriptSha;
    private String addLibraryScriptSha;

    @PostConstruct
    public void loadScripts() {
        try(Jedis jedis = jedisPool.getResource()) {
            // Load the script for reserving books
            String reserveBookScript = LuaScriptLoader.loadScript("remove_library_entry.lua"); // Load Lua script from file
            removeBookSha = jedis.scriptLoad(reserveBookScript); // Load Lua script into Redis
            log.info("Loaded remove_book.lua script with SHA: {}", removeBookSha);

            String decrementCopiesScript = LuaScriptLoader.loadScript("decrement_book_copies.lua");
            decrementCopiesScriptSha = jedis.scriptLoad(decrementCopiesScript);
            log.info("Loaded decrement_book_copies.lua script with SHA: {}", decrementCopiesScriptSha);

            String incrementCopiesScript = LuaScriptLoader.loadScript("increment_book_copies.lua");
            incrementCopiesScriptSha = jedis.scriptLoad(incrementCopiesScript);
            log.info("Loaded increment_book_copies.lua script with SHA: {}", incrementCopiesScriptSha);

            String addLibraryScript = LuaScriptLoader.loadScript("add_library_entry.lua");
            addLibraryScriptSha = jedis.scriptLoad(addLibraryScript);
            log.info("Loaded add_library_entry.lua script with SHA: {}", addLibraryScriptSha);
        }
    }

    /**
     * Gets the availability of a specific book in a specific library.
     *
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     * @return The number of available copies in the library, or null if not found.
     */
    public Integer getBookAvailabilityInLibrary(String bookId, String libraryId) {
        String redisKey = RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId);

        String value = (String) redisTemplate.opsForValue().get(redisKey);
        return value != null ? Integer.parseInt(value.toString()) : null;
    }

    /**
     * Decrements the number of available copies of a book in a library.
     *
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     */
    public void decrementCopiesInLibrary(String bookId, String libraryId) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                executeDecrementCopies(jedis, bookId, libraryId);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                log.error("Script not loaded, attempting to reload");
                loadScripts();
                executeDecrementCopies(jedis, bookId, libraryId);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON response: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse Redis response", e);
        }
    }

    private void executeDecrementCopies(Jedis jedis, String bookId, String libraryId) throws JsonProcessingException {
        // Construct Redis keys dynamically
        List<String> keys = List.of(
                RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId),
                RedisKey.DECREMENT_COPIES_STREAM.getKey()
        );

        // Prepare arguments for the script
        List<String> args = List.of(
                bookId,
                libraryId,
                String.valueOf(System.currentTimeMillis())
        );

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(decrementCopiesScriptSha, keys, args);

        // Parse and handle the response
        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(result.toString(), Map.class);

        log.info("Decrement copies response: {}", response);

        if (response.containsKey("err")) {
            String errorMessage = response.get("err").toString();
            log.warn("No available copies: {}", errorMessage);
            throw new NoAvailableCopiesException(errorMessage);
        }

        log.info("Successfully decremented copies for book {} in library {}", bookId, libraryId);
    }


    /**
     * Increments the number of available copies of a book in a library.
     *
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     */
    public void incrementBookCopies(String bookId, String libraryId) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                executeIncrementCopies(jedis, bookId, libraryId);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                log.error("Script not loaded, attempting to reload");
                loadScripts();
                executeIncrementCopies(jedis, bookId, libraryId);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON response: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse Redis response", e);
        } catch (Exception e) {
            log.error("Error incrementing copies for book {} in library {}: {}", bookId, libraryId, e.getMessage(), e);
            throw e;
        }
    }

    private void executeIncrementCopies(Jedis jedis, String bookId, String libraryId) throws JsonProcessingException {
        // Construct Redis keys dynamically
        List<String> keys = List.of(
                RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId),
                RedisKey.INCREMENT_COPIES_STREAM.getKey()
        );

        // Prepare arguments for the script
        List<String> args = List.of(
                bookId,
                libraryId,
                String.valueOf(System.currentTimeMillis())
        );

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(incrementCopiesScriptSha, keys, args);

        // Parse and handle the response
        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(result.toString(), Map.class);

        log.info("Increment copies response: {}", response);

        if (response.containsKey("err")) {
            String errorMessage = response.get("err").toString();
            log.warn("Increment copies failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.debug("Incremented copies for book {} in library {} - new value: {}",
                bookId, libraryId, response.get("newValue"));
    }

    /**
     * Adds a library entry for a specific book if it does not already exist.
     *
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     * @param initialValue The initial availability value (default to 0 if null).
     */
    public void addLibraryToBookAvailability(String bookId, String libraryId, Integer initialValue) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                executeAddLibrary(jedis, bookId, libraryId, initialValue);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                log.error("Script not loaded, attempting to reload");
                loadScripts();
                executeAddLibrary(jedis, bookId, libraryId, initialValue);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON response: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse Redis response", e);
        }
    }

    private void executeAddLibrary(Jedis jedis, String bookId, String libraryId, Integer initialValue) throws JsonProcessingException {
        // Construct Redis keys dynamically
        List<String> keys = List.of(
                RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId),
                RedisKey.ADD_LIBRARY_STREAM.getKey()
        );

        // Prepare arguments for the script
        List<String> args = List.of(
                bookId,
                libraryId,
                String.valueOf(initialValue),
                String.valueOf(System.currentTimeMillis())
        );

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(addLibraryScriptSha, keys, args);

        // Parse and handle the response
        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(result.toString(), Map.class);

        log.info("Add library response: {}", response);

        if (response.containsKey("err")) {
            log.warn("Library entry already exists for book {} in library {}", bookId, libraryId);
            throw new LibraryEntryAlreadyExistsException(bookId, libraryId);
        }

        log.info("Successfully added library entry for book {} in library {}", bookId, libraryId);
    }

    /**
     * Removes a library entry for a specific book.
     *
     * @param bookId    The ID of the book.
     * @param libraryId The ID of the library.
     */
    public void removeLibraryEntry(String bookId, String libraryId, Integer totalCopies) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                executeRemoveLibraryEntry(jedis, bookId, libraryId, totalCopies);
            } catch (redis.clients.jedis.exceptions.JedisNoScriptException e) {
                log.error("Script not loaded, attempting to reload");
                loadScripts();
                executeRemoveLibraryEntry(jedis, bookId, libraryId, totalCopies);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON response: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse remove result from Redis", e);
        }
    }

    private void executeRemoveLibraryEntry(Jedis jedis, String bookId, String libraryId, Integer totalCopies) throws JsonProcessingException {
        // Construct Redis keys dynamically
        List<String> keys = List.of(
                RedisKey.BOOK_AVAILABILITY.getKey(bookId, libraryId),
                RedisKey.REMOVE_LIBRARY_STREAM.getKey()
        );

        // Prepare arguments for the script
        List<String> args = List.of(
                String.valueOf(totalCopies),
                bookId,
                libraryId,
                String.valueOf(System.currentTimeMillis())
        );

        // Execute Lua script using its SHA
        Object result = jedis.evalsha(removeBookSha, keys, args);

        // Handle script result
        if (result instanceof String jsonResult) {
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.readValue(jsonResult, Map.class);

            log.info("Remove library entry response: {}", resultMap);

            if (resultMap.containsKey("err")) {
                String errorMessage = (String) resultMap.get("err");
                log.warn("Failed to remove library entry: {}", errorMessage);

                if (errorMessage.contains("All copies must be available")) {
                    throw new CannotRemoveBookException(errorMessage);
                }
                throw new NoSuchElementException(errorMessage);
            }

            log.info("Successfully removed library entry for book {} in library {}", bookId, libraryId);
        } else {
            log.error("Unexpected result type from Redis script");
            throw new IllegalStateException("Unexpected result type from Redis script");
        }
    }

}
