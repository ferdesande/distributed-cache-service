package com.fsg.cacheservice.infrastructure.inmemory

import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.ValueGenerator
import com.fsg.cacheservice.core.exception.BadRequestException
import com.fsg.cacheservice.core.exception.InvalidValueException
import com.fsg.cacheservice.core.exception.OverflowException
import com.fsg.cacheservice.core.exception.WrongTypeException
import com.fsg.cacheservice.infrastructure.inmemory.model.CacheEntry
import com.fsg.cacheservice.infrastructure.inmemory.model.Ranking
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

@Suppress("TooManyFunctions")
class InMemoryCacheRepository(
    private val valueGenerator: ValueGenerator
) : CacheRepository {
    private val inMemoryCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val incrementLock = ReentrantReadWriteLock()

    override fun set(key: String, value: String, expiration: Duration?) {
        val expirationInstant = expiration?.let {
            if (it.isNegative || it.isZero) {
                throw BadRequestException("Expiration time must greater than zero")
            }
            valueGenerator.now().plusMillis(it.toMillis())
        }

        inMemoryCache[key] = CacheEntry(value, expirationInstant)
    }

    override fun get(key: String): String? {
        checkExpired(key)
        return getString(key)
    }

    override fun delete(key: String): Boolean {
        return inMemoryCache.remove(key) != null
    }

    override fun getCacheKeyCount(): Int {
        incrementLock.writeLock().lock()
        try {
            cleanExpiredKeys()
            return inMemoryCache.size
        } finally {
            incrementLock.writeLock().unlock()
        }
    }

    override fun increment(key: String): Long {
        incrementLock.writeLock().lock()
        try {
            checkExpired(key)
            val value = when (val value = inMemoryCache[key]?.value) {
                null -> null
                is String -> value
                else -> throw WrongTypeException("The value for key '$key' is not a number")
            } ?: "0"
            val longValue = toLong(key, value) + 1
            inMemoryCache[key] = CacheEntry((longValue).toString())
            return longValue
        } finally {
            incrementLock.writeLock().unlock()
        }
    }

    override fun setRankedElement(key: String, score: Double, member: String): Boolean =
        (getRanking(key) ?: this.run {
            val newRanking = Ranking()
            inMemoryCache[key] = CacheEntry(newRanking)
            newRanking
        }).addOrUpdateMember(member, score)

    override fun getRankedElementCount(
        key: String
    ): Long = (inMemoryCache[key]?.value as? Ranking)?.getMemberCount() ?: 0L

    override fun getRankedElementPosition(key: String, member: String): Long? =
        getRanking(key)?.getMemberPosition(member)

    override fun getRankedElementRange(
        key: String,
        start: Long,
        stop: Long
    ): List<String> = getRanking(key)?.getMemberRange(start, stop) ?: emptyList()

    private fun getRanking(key: String): Ranking? {
        return when (val ranking = inMemoryCache[key]?.value) {
            null -> null
            is Ranking -> ranking
            else -> throw WrongTypeException("The value for key '$key' is not a Ranking")
        }
    }

    private fun getString(key: String): String? {
        return when (val value = inMemoryCache[key]?.value) {
            null -> null
            is String -> value
            else -> throw WrongTypeException("The value for key '$key' is not a String")
        }
    }

    private fun checkExpired(key: String) {
        val cacheEntry = inMemoryCache[key]
        if (cacheEntry?.expiresAt != null && cacheEntry.expiresAt < valueGenerator.now()) {
            inMemoryCache.remove(key)
        }
    }

    private fun cleanExpiredKeys() {
        inMemoryCache.keys.forEach { checkExpired(it) }
    }

    private fun toLong(key: String, value: String): Long {
        val longValue = try {
            value.toLong()
        } catch (_: NumberFormatException) {
            throw InvalidValueException("The value for key '$key' is not a number")
        }

        if (longValue == Long.MAX_VALUE) {
            throw OverflowException("Increment for key '$key' cannot be done because result overflows 64-bits value")
        }

        return longValue
    }
}
