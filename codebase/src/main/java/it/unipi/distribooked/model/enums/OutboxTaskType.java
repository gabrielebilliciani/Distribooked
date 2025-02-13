package it.unipi.distribooked.model.enums;

/**
 * Enumeration of task types in the Outbox collection.
 */
public enum OutboxTaskType {
    UPDATE_AUTHOR,
    CREATE_USER_REDIS_HASH,
    DECREMENT_BOOK_COPIES,
    REMOVE_LIBRARY_FROM_BOOK,
    ADD_LIBRARY_TO_BOOK,
    INCREMENT_BOOK_COPIES,
    INCREMENT_REDIS_AVAILABILITY,
    MARK_LOAN_OVERDUE,
    ADD_READ_BOOK
}
