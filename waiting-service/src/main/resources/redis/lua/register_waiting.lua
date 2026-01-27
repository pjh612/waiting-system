-- KEYS
-- 1: waiting queue key (ZSET)
-- 2: heartbeat key (HASH)

-- ARGV
-- 1: userId
-- 2: score (epoch millis)
-- 3: ttlSeconds

local queueKey = KEYS[1]
local heartbeatKey = KEYS[2]

local userId = ARGV[1]
local score = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])

-- 1. 대기열 등록
redis.call("ZADD", queueKey, score, userId)

-- 2. heartbeat 기록
redis.call("HSET", heartbeatKey, userId, score)

redis.call("EXPIRE", queueKey, ttl)
redis.call("EXPIRE", heartbeatKey, ttl)

-- 4. 순번 계산 (0-based → 1-based)
local rank = redis.call("ZRANK", queueKey, userId)

if rank == false then
    return -1
end

return rank + 1
