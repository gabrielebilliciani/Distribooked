-- Lua script to cancel a reservation
local availabilityKey = KEYS[1]       -- book availability key
local userResLoansKey = KEYS[2]      -- user reservations hash key
local libraryResKey = KEYS[3]        -- library reservations hash key
local expirationZSetKey = KEYS[4]    -- expiration tracking ZSet key
local userReservationField = KEYS[5] -- user reservation field
local libraryReservationField = KEYS[6] -- library reservation field

local zsetMember = ARGV[4]

-- Check if user reservation exists
if redis.call('HEXISTS', userResLoansKey, userReservationField) == 0 then
    return cjson.encode({["err"] = "User reservation does not exist"})
end

-- Check if library reservation exists
if redis.call('HEXISTS', libraryResKey, libraryReservationField) == 0 then
    return cjson.encode({["err"] = "Library reservation does not exist"})
end

-- Delete reservation from user reservations hash
redis.call('HDEL', userResLoansKey, userReservationField)

-- Delete reservation from library reservations hash
redis.call('HDEL', libraryResKey, libraryReservationField)

-- Remove the reservation from the expiration ZSet
redis.call('ZREM', expirationZSetKey, zsetMember)

-- Increment availability
redis.call('INCR', availabilityKey)

-- Return success
return cjson.encode({
    ["status"] = "Reservation cancelled successfully",
    ["userId"] = ARGV[1],
    ["bookId"] = ARGV[2],
    ["libraryId"] = ARGV[3]
})