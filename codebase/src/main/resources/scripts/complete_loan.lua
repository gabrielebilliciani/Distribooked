-- Complete a loan by removing it from either LIBRARY_LOANS or LIBRARY_OVERDUE
local userResLoansKey = KEYS[1]      -- user activity key
local libraryLoansKey = KEYS[2]      -- library loans key
local libraryOverdueKey = KEYS[3]    -- library overdue key
local loanExpirationZSetKey = KEYS[4] -- loan expiration zset
local availabilityKey = KEYS[5]       -- book availability key
local userField = KEYS[6]             -- user hash field
local libraryField = KEYS[7]          -- library hash field
local streamKey = KEYS[8]             -- stream key

-- Check if loan exists in LIBRARY_LOANS
local inLoans = redis.call('HEXISTS', libraryLoansKey, libraryField)
local inOverdue = redis.call('HEXISTS', libraryOverdueKey, libraryField)

if inLoans == 0 and inOverdue == 0 then
    return cjson.encode({["err"] = "Loan not found"})
end

-- Remove from user activity
redis.call('HDEL', userResLoansKey, userField)

-- Remove from appropriate library hash and handle zset if needed
if inLoans == 1 then
    redis.call('HDEL', libraryLoansKey, libraryField)
    -- Remove from loan expiration tracking
    local zsetMember = libraryField -- This should match the format used in mark_as_loan
    redis.call('ZREM', loanExpirationZSetKey, zsetMember)
else
    redis.call('HDEL', libraryOverdueKey, libraryField)
end

-- Increment book availability
redis.call('INCR', availabilityKey)

local streamMessage = cjson.encode({
    userId = ARGV[1],
    bookId = ARGV[2],
    libraryId = ARGV[3],
    eventType = "COMPLETED_LOAN",
    timestamp = redis.call('TIME')[1] -- current timestamp
})
redis.call('XADD', streamKey, '*', 'data', streamMessage)

return cjson.encode({
    ["status"] = "Loan completed successfully"
})