package it.unipi.distribooked.utils;

public enum RedisKey {
    BOOK_AVAILABILITY("book:%s:lib:%s:avail"),
    USER_ACTIVITY("user:%s:active"),
    LIBRARY_RESERVATIONS("lib:%s:res"),
    LIBRARY_LOANS("lib:%s:loans"),
    LIBRARY_OVERDUE("lib:%s:overdue"),
    USER_HASH_ENTRY("lib:%s:book:%s:info"),
    LIBRARY_HASH_ENTRY("user:%s:book:%s:start"),

    // exp = expiration time: in the case of a reservation, it is the deadline;
    // in the case of a loan, it is the return date;
    // in the case of overdue loans, it is the maximum return date
    RESERVATION_ZSET_EXPIRATION("zset:res-exp"),
    LOAN_ZSET_EXPIRATION("zset:loan-exp"),
    ZSET_ENTRY("user:%s:book:%s:lib:%s:exp"),
    DECREMENT_COPIES_STREAM("stream:decrement-copies"),
    REMOVE_LIBRARY_STREAM("stream:remove-library"),
    INCREMENT_COPIES_STREAM("stream:increment-copies"),
    ADD_LIBRARY_STREAM("stream:add-library"),
    COMPLETED_LOANS_STREAM("stream:completed-loans");

    private final String pattern;

    RedisKey(String pattern) {
        this.pattern = pattern;
    }

    public String getKey(Object... args) {
        return String.format(pattern, args);
    }
}
