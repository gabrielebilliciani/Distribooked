-- Keys for atomic loan conversion
local userResLoansKey = KEYS[1]      -- user activity key
local libraryResKey = KEYS[2]        -- library reservations key
local libraryLoansKey = KEYS[3]      -- library loans key
local resExpirationZSetKey = KEYS[4] -- reservation expiration zset
local loanExpirationZSetKey = KEYS[5] -- loan expiration zset
local userReservationField = KEYS[6]  -- user hash field
local libraryReservationField = KEYS[7] -- library hash field

local userId = ARGV[1]
local bookId = ARGV[2]
local libraryId = ARGV[3]
local currentTime = tonumber(ARGV[4])
local loanDuration = tonumber(ARGV[5]) -- 30 days in seconds

-- Check if reservation exists
if redis.call('HEXISTS', userResLoansKey, userReservationField) == 0 then
    return cjson.encode({["err"] = "Reservation not found"})
end

-- Get current reservation data
local reservationJson = redis.call('HGET', userResLoansKey, userReservationField)
local reservationData = cjson.decode(reservationJson)

-- Update reservation data for loan
local loanExpiryTime = currentTime + (loanDuration * 1000)
reservationData.status = "LOANED"
reservationData.deadlineDate = loanExpiryTime
local updatedJson = cjson.encode(reservationData)

-- Remove from reservation tracking
local zsetMember = "user:" .. userId .. ":book:" .. bookId .. ":lib:" .. libraryId .. ":exp"
redis.call('ZREM', resExpirationZSetKey, zsetMember)

-- Add to loan tracking
redis.call('ZADD', loanExpirationZSetKey, loanExpiryTime, zsetMember)

-- Update user activity hash
redis.call('HSET', userResLoansKey, userReservationField, updatedJson)

-- Remove from library reservations and add to loans
redis.call('HDEL', libraryResKey, libraryReservationField)
redis.call('HSET', libraryLoansKey, libraryReservationField, tostring(currentTime))

-- Set loan expiry time
redis.call('HPEXPIRE', libraryLoansKey, loanDuration * 1000, "FIELDS", 1, libraryReservationField)

return cjson.encode({
    ["status"] = "Loan conversion successful",
    ["loanTime"] = currentTime,
    ["expiryTime"] = loanExpiryTime
})