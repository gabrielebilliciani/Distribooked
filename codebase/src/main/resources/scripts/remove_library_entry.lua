-- Script for removing a book from the library if it exists
-- KEYS[1]: redisKey for the book in the library
-- KEYS[2]: streamKey for remove book events
-- ARGV[1]: total copies (availability status)
-- ARGV[2]: bookId
-- ARGV[3]: libraryId
-- ARGV[4]: timestamp
-- Return value: JSON string with the result of the operation

local exists = redis.call('EXISTS', KEYS[1])
if exists == 0 then
    return cjson.encode({
        err = "Library entry not found"
    })
end

local currentAvailability = redis.call('GET', KEYS[1])
if currentAvailability ~= ARGV[1] then
    return cjson.encode({
        err = "Cannot remove book. All copies must be available in the library",
        expected = ARGV[1],
        current = currentAvailability
    })
end

local deleted = redis.call('DEL', KEYS[1])
if deleted == 1 then
    -- Add message to stream
    local streamMessage = cjson.encode({
        bookId = ARGV[2],
        libraryId = ARGV[3],
        eventType = "REMOVE_LIBRARY_FROM_BOOK",
        timestamp = ARGV[4]
    })
    redis.call('XADD', KEYS[2], '*', 'data', streamMessage)

    return cjson.encode({
        success = true
    })
else
    return cjson.encode({
        err = "Failed to remove library entry"
    })
end