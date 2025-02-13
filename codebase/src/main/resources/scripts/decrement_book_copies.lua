-- Keys: availability key
-- Args: bookId, libraryId, timestamp
local availabilityKey = KEYS[1]
local streamKey = KEYS[2]

-- Check availability
local available = redis.call('GET', availabilityKey)
if not available or tonumber(available) <= 0 then
    return cjson.encode({["err"] = "No available copies"})
end

-- Decrement availability
redis.call('DECR', availabilityKey)

-- Add message to stream
local streamMessage = cjson.encode({
    bookId = ARGV[1],
    libraryId = ARGV[2],
    eventType = "DECREMENT_COPIES",
    timestamp = ARGV[3]
})
redis.call('XADD', streamKey, '*', 'data', streamMessage)

return cjson.encode({
    ["status"] = "Copies decremented successfully"
})