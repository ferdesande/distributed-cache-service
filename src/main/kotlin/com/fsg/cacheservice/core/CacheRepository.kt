package com.fsg.cacheservice.core

import java.time.Duration

interface CacheRepository {

    // HINT: the value does not expire when duration is null
    fun set(key: String, value: String, expiration: Duration? = null)

    fun get(key: String): String?

    // HINT: just remove a single key due to specification does not request for multiple key deletion
    fun delete(key: String): Boolean

    fun getCacheKeyCount(): Long

    // HINT: Key is set to 1 if it did not exist
    fun increment(key: String): Long

    // HINT: All sorted set items are 0-based rank with 0 as the smallest rank

    // HINT: Returns true if member was added or false if it was just updated
    fun sortedSetAdd(key: String, score: Double, member: String): Boolean

    fun getSortedSetElementCount(key: String): Long

    // HINT: Returns null if member doesn't exist
    fun sortedSetRank(key: String, member: String): Long?

    // HINT: Both start and stop indices are inclusive
    fun sortedSetRange(key: String, start: Long, stop: Long): List<String>
}
