package it.unipi.distribooked.repository.redis;

import it.unipi.distribooked.utils.RedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class OverdueLoanRepository {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Marks a loan as overdue in Redis
     *
     * @param userId The ID of the user with the overdue loan
     * @param bookId The ID of the overdue book
     * @param libraryId The ID of the library
     * @return true if the loan was successfully marked as overdue, false otherwise
     */
    public boolean markLoanAsOverdue(String userId, String bookId, String libraryId) {
        String libraryOverdueKey = RedisKey.LIBRARY_OVERDUE.getKey(libraryId);
        String loanField = RedisKey.LIBRARY_HASH_ENTRY.getKey(userId, bookId);

        redisTemplate.opsForHash().put(libraryOverdueKey, loanField, String.valueOf(System.currentTimeMillis()));

        boolean isAdded = redisTemplate.opsForHash().hasKey(libraryOverdueKey, loanField);
        if (!isAdded) {
            log.error("Failed to add overdue loan for user {} book {} in library {}",
                    userId, bookId, libraryId);
            return false;
        }

        log.info("Successfully added overdue loan for user {} book {} in library {}",
                userId, bookId, libraryId);
        return true;
    }
}
