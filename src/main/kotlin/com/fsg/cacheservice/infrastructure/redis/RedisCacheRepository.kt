package com.fsg.cacheservice.infrastructure.redis

import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.exception.CacheException
import com.fsg.cacheservice.core.exception.InvalidCacheRangeException
import com.fsg.cacheservice.core.exception.InvalidIncrementValueException
import io.lettuce.core.RedisCommandExecutionException
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisCacheRepository(
    private val redisTemplate: RedisTemplate<String, String>
) : CacheRepository {

    // TODO: possible future new feature:
    // - Think if add validation for null / empty keys are needed.
    // - Think if add validation for null / empty members in ranked elements are needed.
    // - It could be a good idea to do the parameter validation with a decorator to respect SRP.
    //   This would make it interchangeable with any other cache implementation
    // - Think if catch Redis exception in any method is needed or if it could be handled in the rest layer.
    override fun set(key: String, value: String, expiration: Duration?) {
        if (expiration == null) {
            redisTemplate.opsForValue().set(key, value)
        } else {
            redisTemplate.opsForValue().set(key, value, expiration)
        }
    }

    override fun get(key: String): String? = redisTemplate.opsForValue().get(key)

    override fun delete(key: String): Boolean = redisTemplate.delete(key)

    override fun getCacheKeyCount(): Int = redisTemplate.keys("*").size

    override fun increment(key: String): Long? {
        try {
            return redisTemplate.opsForValue().increment(key)
        } catch (ex: RedisSystemException) {
            throw when (ex.cause) {
                is RedisCommandExecutionException -> {
                    InvalidIncrementValueException(
                        "Key cannot be increased, contains non integer or out of range value",
                        ex.cause
                    )
                }

                else -> CacheException("Unexpected exception occurred while incrementing $key", ex)
            }
        }
    }

    override fun setRankedElement(key: String, score: Double, member: String): Boolean? =
        redisTemplate.opsForZSet().add(key, member, score)

    override fun getRankedElementCount(key: String): Long? {
        return redisTemplate.opsForZSet().zCard(key)
    }

    override fun getRankedElementPosition(key: String, member: String): Long? =
        redisTemplate.opsForZSet().rank(key, member)

    override fun getRankedElementRange(
        key: String,
        start: Long,
        stop: Long
    ): List<String> {
        if (start < 0 || stop < 0) {
            throw InvalidCacheRangeException("Indexes for a range must be greater than or equal to 0")
        } else if (start > stop) {
            throw InvalidCacheRangeException("Start range is greater than end range")
        }
        return redisTemplate.opsForZSet().range(key, start, stop)?.toList() ?: emptyList()
    }
}
