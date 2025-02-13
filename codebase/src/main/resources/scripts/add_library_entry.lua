-- Keys: availability key, stream key
-- Args: bookId, libraryId, initialValue, timestamp
local availabilityKey = KEYS[1]
local streamKey = KEYS[2]

-- Check if the key already exists
local exists = redis.call('EXISTS', availabilityKey)
if exists == 1 then
    return cjson.encode({["err"] = "Library entry already exists"})
end

-- Set the initial value
redis.call('SET', availabilityKey, ARGV[3])

-- Add message to stream
local streamMessage = cjson.encode({
    bookId = ARGV[1],
    libraryId = ARGV[2],
    initialValue = ARGV[3],
    eventType = "ADD_LIBRARY_TO_BOOK",
    timestamp = ARGV[4]
})
redis.call('XADD', streamKey, '*', 'data', streamMessage)

return cjson.encode({
    ["status"] = "Library entry added successfully"
})