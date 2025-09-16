package com.fsg.cacheservice.core

import java.time.Duration

interface CacheRepository {

    // HINT: the value does not expire when duration is null
    fun set(key: String, value: String, expiration: Duration? = null)

    fun get(key: String): String?

    // HINT: just remove a single key due to specification does not request for multiple key deletion
    fun delete(key: String): Boolean

    fun getCacheKeyCount(): Int

    // HINT: Key is set to 1 if it did not exist
    fun increment(key: String): Long

    // HINT: All sorted set items are hidden under the alias ranked element
    //       being 0-based rank with 0 as the smallest rank

    // HINT: Returns true if member was added, false if it was just updated
    fun setRankedElement(key: String, score: Double, member: String): Boolean?

    fun getRankedElementCount(key: String): Long

    // HINT: Returns null if member doesn't exist
    fun getRankedElementPosition(key: String, member: String): Long?

    // HINT: Both start and stop indices are inclusive.
    //       The output is converted into a List to guarantee the ranking order.
    fun getRankedElementRange(key: String, start: Long, stop: Long): List<String>
}
