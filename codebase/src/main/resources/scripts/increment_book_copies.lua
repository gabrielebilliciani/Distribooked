-- Increment book copies and add stream message
local availabilityKey = KEYS[1]    -- book availability key
local streamKey = KEYS[2]          -- stream key

-- Check if the key exists
local exists = redis.call('EXISTS', availabilityKey)
if exists == 0 then
    return cjson.encode({["err"] = "Key not found"})
end

-- Increment the availability counter
local newValue = redis.call('INCR', availabilityKey)

-- Create and add stream message
local streamMessage = cjson.encode({
    bookId = ARGV[1],
    libraryId = ARGV[2],
    eventType = "INCREMENT_COPIES",
    newValue = newValue,
    timestamp = redis.call('TIME')[1] -- current timestamp
})
redis.call('XADD', streamKey, '*', 'data', streamMessage)

return cjson.encode({
    ["status"] = "Success",
    ["newValue"] = newValue
})