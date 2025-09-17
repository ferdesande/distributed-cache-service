package com.fsg.cacheservice.infrastructure.inmemory.model

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.locks.ReentrantReadWriteLock

internal class Ranking {
    private val scoreToMembers = ConcurrentSkipListMap<Double, MutableSet<String>>()
    private val memberToScore = ConcurrentHashMap<String, Double>()
    private val lock = ReentrantReadWriteLock()

    fun addOrUpdateMember(member: String, score: Double): Boolean {
        lock.writeLock().lock()
        try {
            val existingScore = memberToScore[member]
            if (existingScore != null) {
                scoreToMembers[existingScore]?.remove(member)
            }

            scoreToMembers.computeIfAbsent(score) { ConcurrentHashMap.newKeySet() }.add(member)
            memberToScore[member] = score

            return existingScore == null
        } finally {
            lock.writeLock().unlock()
        }
    }

    @Suppress("ReturnCount")
    fun getMemberPosition(member: String): Long? {
        lock.readLock().lock()
        try {
            val targetScore = memberToScore[member] ?: return null
            var position = 0L

            for ((score, members) in scoreToMembers) {
                when {
                    score < targetScore -> position += members.size
                    score == targetScore -> {
                        val sortedMembers = members.sorted()
                        return position + sortedMembers.indexOf(member)
                    }

                    else -> break // Hint: it should never reach this point
                }
            }
            return null // Hint: it should never reach this point
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getMemberRange(start: Long, stop: Long): List<String> {
        lock.readLock().lock()
        try {
            val sortedMembers = getSortedMembers()
            val size = sortedMembers.size

            val normalizedStart = if (start < 0) maxOf(0, size + start.toInt()) else start.toInt()
            val normalizedStop = if (stop < 0) maxOf(-1, size + stop.toInt()) else minOf(size - 1, stop.toInt())

            return if (normalizedStart <= normalizedStop && normalizedStart < size) {
                sortedMembers.subList(normalizedStart, normalizedStop + 1)
            } else {
                emptyList()
            }
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getMemberCount(): Long = memberToScore.count().toLong()

    internal fun getMemberScore(member: String): Double? = memberToScore[member]

    private fun getSortedMembers(): List<String> {
        return scoreToMembers.values.map { it.sorted() }.flatten()
    }
}
