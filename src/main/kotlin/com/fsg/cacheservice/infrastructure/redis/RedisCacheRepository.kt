package com.fsg.cacheservice.infrastructure.redis

import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.exception.BadRequestException
import com.fsg.cacheservice.core.exception.CacheException
import com.fsg.cacheservice.core.exception.InvalidValueException
import com.fsg.cacheservice.core.exception.OverflowException
import com.fsg.cacheservice.core.exception.WrongTypeException
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
        if (expiration != null && (expiration.isZero || expiration.isNegative)) {
            throw BadRequestException("Expiration time must greater than zero")
        }

        if (expiration == null) {
            redisTemplate.opsForValue().set(key, value)
        } else {
            redisTemplate.opsForValue().set(key, value, expiration)
        }
    }

    override fun get(key: String): String? {
        try {
            return redisTemplate.opsForValue().get(key)
        } catch (ex: RedisSystemException) {
            throw when {
                ex.cause is RedisCommandExecutionException && ex.cause!!.message?.contains("WRONGTYPE") == true ->
                    WrongTypeException("The value for key '$key' is not a String")

                else -> CacheException("Unhandled cache exception", ex)
            }
        }
    }

    override fun delete(key: String): Boolean = redisTemplate.delete(key)

    override fun getCacheKeyCount(): Int = redisTemplate.keys("*").size

    override fun increment(key: String): Long {
        try {
            // Hint: This method returns null when it's used within a pipeline / transaction
            return redisTemplate.opsForValue().increment(key)!!
        } catch (ex: RedisSystemException) {
            val cause = ex.cause
            throw when (cause) {
                is RedisCommandExecutionException -> {
                    val message = cause.message ?: ""
                    when {
                        message.contains("WRONGTYPE") ->
                            WrongTypeException("The value for key '$key' is not a number")

                        message.contains("ERR value is not an integer or out of range") ->
                            InvalidValueException("The value for key '$key' is not a number")

                        message.contains("ERR increment or decrement would overflow") ->
                            OverflowException(
                                "Increment for key '$key' cannot be done because result overflows 64-bits value"
                            )

                        else ->
                            CacheException("Unexpected exception occurred while incrementing $key", ex.cause)
                    }
                }

                else -> CacheException("Unexpected exception occurred while incrementing $key", ex)
            }
        }
    }

    // Hint: This method returns null when it's used within a pipeline / transaction
    override fun setRankedElement(key: String, score: Double, member: String): Boolean =
        redisTemplate.opsForZSet().add(key, member, score)!!

    // Hint: This method returns null when it's used within a pipeline / transaction
    override fun getRankedElementCount(key: String): Long = redisTemplate.opsForZSet().zCard(key)!!

    override fun getRankedElementPosition(key: String, member: String): Long? =
        redisTemplate.opsForZSet().rank(key, member)

    override fun getRankedElementRange(
        key: String,
        start: Long,
        stop: Long
    ): List<String> = redisTemplate.opsForZSet().range(key, start, stop)?.toList() ?: emptyList()
}
