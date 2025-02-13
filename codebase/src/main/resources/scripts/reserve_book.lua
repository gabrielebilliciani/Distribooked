-- Reserve a book for a user in a specific library
local availabilityKey = KEYS[1]       -- book availability key
local userResLoansKey = KEYS[2]      -- user reservations key
local libraryResKey = KEYS[3]        -- library reservations key
local expirationZSetKey = KEYS[4]    -- key for the expiration tracking ZSet
local userReservationField = KEYS[5] -- user reservation field
local libraryReservationField = KEYS[6] -- library reservation field

local maxReservations = tonumber(ARGV[1])
local userId = ARGV[2]
local bookId = ARGV[3]
local libraryId = ARGV[4]
local currentTime = tonumber(ARGV[5])
local reservationExpiry = tonumber(ARGV[6])
local bookTitle = ARGV[7]
local libraryName = ARGV[8]
local zsetMember = ARGV[9]

-- Check if the user has already reserved this book
if redis.call('HEXISTS', userResLoansKey, userReservationField) == 1 then
    return cjson.encode({["err"] = "User has already reserved this book in this library"})
end

-- Check if the library already has this reservation
if redis.call('HEXISTS', libraryResKey, libraryReservationField) == 1 then
    return cjson.encode({["err"] = "This reservation already exists in the library"})
end

-- Check user reservation count
local currentReservations = redis.call('HLEN', userResLoansKey)
if currentReservations >= maxReservations then
    return cjson.encode({["err"] = "User already has maximum reservations"})
end

-- Check if the key exists
local keyExists = redis.call('EXISTS', availabilityKey)
if keyExists == 0 then
    return cjson.encode({["err"] = "Book not available in selected library"})
end

-- Check book availability
local availability = tonumber(redis.call('GET', availabilityKey))
if availability <= 0 then
    return cjson.encode({["err"] = "Book CURRENTLY not available in selected library"})
end

local expiryTimestamp = currentTime + (reservationExpiry * 1000) -- Convert to milliseconds
local userActivityJson = cjson.encode({
    status = "RESERVED",
    title = bookTitle,
    libraryName = libraryName,
    deadlineDate = expiryTimestamp
})

-- Decrement availability
redis.call('DECR', availabilityKey)

-- Add reservation to user's reservations
redis.call('HSET', userResLoansKey, userReservationField, userActivityJson)

-- Add reservation to library reservations
redis.call('HSET', libraryResKey, libraryReservationField, tostring(currentTime))

-- Set expiration for user reservation field
local userExpireSet = redis.call('HPEXPIRE', userResLoansKey, reservationExpiry * 1000, "FIELDS", 1, userReservationField)
if userExpireSet == 0 then
    return cjson.encode({["err"] = "Failed to set expiration for user reservation"})
end

-- Set expiration for library reservation field
local libraryExpireSet = redis.call('HPEXPIRE', libraryResKey, reservationExpiry * 1000, "FIELDS", 1, libraryReservationField)
if libraryExpireSet == 0 then
    return cjson.encode({["err"] = "Failed to set expiration for library reservation"})
end

-- Add reservation expiry to the ZSet for tracking
redis.call('ZADD', expirationZSetKey, currentTime + reservationExpiry, zsetMember)

return cjson.encode({
    ["status"] = "Reservation successful",
    ["reservationTime"] = currentTime,
    ["expiryTime"] = currentTime + reservationExpiry
})
