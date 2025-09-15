package com.fsg.cacheservice.infrastructure.redis

import com.fsg.cacheservice.core.exception.InvalidCacheRangeException
import com.fsg.cacheservice.core.exception.InvalidIncrementValueException
import com.fsg.cacheservice.testcontainers.RedisTestBase
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.emptyIterable
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@SpringBootTest
class RedisCacheRepositoryTest : RedisTestBase() {

    @Autowired
    private lateinit var repository: RedisCacheRepository

    companion object {
        private const val EXISTING_KEY = "a-valid-key"
        private const val NON_EXISTING_KEY = "another-key"
        private const val SAMPLE_VALUE = "An entry value"
        private const val ANOTHER_VALUE = "Another entry value"
        private const val TTL_KEY = "ttl-test-key"
        private const val EXPIRED_KEY = "expired-test-key"
        private const val RANKING_KEY = "ranking-test-key"

        private const val EXPECTED_NOT_EXPIRATION_TIME = -1L

        private val SHORT_TTL = Duration.ofMillis(10)
        private val MEDIUM_TTL = Duration.ofMillis(100)
        private val LONG_TTL = Duration.ofSeconds(10)

        private const val SMALL_RANK = 1.0
        private const val MEDIUM_RANK = 5.0
        private const val LONG_RANK = 10.0

        private const val FIRST_MEMBER = "first-member"
        private const val SECOND_MEMBER = "second-member"
        private const val THIRD_MEMBER = "third-member"

        private const val SLEEP_IN_MILLIS = 20L

        private const val NEGATIVE_RANKED_RANGE_INDEX_ERROR_MESSAGE =
            "Indexes for a range must be greater than or equal to 0"
        private const val INVERTED_RANKED_RANGE_INDEXES_ERROR_MESSAGE = "Start range is greater than end range"
    }

    @Nested
    @DisplayName("Key-Value operations")
    inner class KeyValueOperations {

        @Test
        fun `set saves a value when there is no match`() {
            // Given
            assertThat(redisTemplate.opsForValue().get(EXISTING_KEY), nullValue())

            // When
            repository.set(EXISTING_KEY, SAMPLE_VALUE)

            // Then
            assertThat(redisTemplate.opsForValue().get(EXISTING_KEY), equalTo(SAMPLE_VALUE))
        }

        @Test
        fun `set updates a value when there is match`() {
            // Given
            redisTemplate.opsForValue().set(EXISTING_KEY, SAMPLE_VALUE)

            // When
            repository.set(EXISTING_KEY, ANOTHER_VALUE)

            // Then
            assertThat(redisTemplate.opsForValue().get(EXISTING_KEY), equalTo(ANOTHER_VALUE))
        }

        @Test
        fun `get returns null when there is no match`() {
            // Given
            assertThat(redisTemplate.opsForValue().get(EXISTING_KEY), nullValue())

            // When
            val result = repository.get(NON_EXISTING_KEY)

            // Then
            assertThat(result, nullValue())
        }

        @Test
        fun `get returns the value when there is match`() {
            // Given
            redisTemplate.opsForValue().set(EXISTING_KEY, SAMPLE_VALUE)

            // When
            val result = repository.get(EXISTING_KEY)

            // Then
            assertThat(result, equalTo(SAMPLE_VALUE))
        }
    }

    @Nested
    @DisplayName("TTL operations")
    inner class TTLOperations {

        @Test
        fun `set with TTL works as expected`() {
            // Given
            assertThat(redisTemplate.opsForValue().get(TTL_KEY), nullValue())

            // When
            repository.set(TTL_KEY, SAMPLE_VALUE, MEDIUM_TTL)

            // Then
            assertThat(redisTemplate.opsForValue().get(TTL_KEY), equalTo(SAMPLE_VALUE))
            val ttl = redisTemplate.getExpire(TTL_KEY, TimeUnit.MILLISECONDS)
            assertThat(ttl, greaterThan(0L))
        }

        @Test
        fun `set updates the value and the TTL when it already exists`() {
            // Given
            redisTemplate.opsForValue().set(TTL_KEY, SAMPLE_VALUE, LONG_TTL)

            // When
            repository.set(TTL_KEY, ANOTHER_VALUE, MEDIUM_TTL)

            // Then
            assertThat(redisTemplate.opsForValue().get(TTL_KEY), equalTo(ANOTHER_VALUE))
            val ttl = redisTemplate.getExpire(TTL_KEY, TimeUnit.MILLISECONDS)
            assertThat(
                ttl,
                allOf(
                    greaterThan(0L),
                    lessThanOrEqualTo(MEDIUM_TTL.toMillis())
                )
            )
        }

        @Test
        fun `set without TTL removes existing TTL`() {
            // Given
            redisTemplate.opsForValue().set(TTL_KEY, SAMPLE_VALUE, SHORT_TTL)
            assertThat(
                redisTemplate.getExpire(TTL_KEY, TimeUnit.MILLISECONDS),
                greaterThan(0L)
            )

            // When
            repository.set(TTL_KEY, ANOTHER_VALUE)

            // Then
            assertThat(redisTemplate.opsForValue().get(TTL_KEY), equalTo(ANOTHER_VALUE))
            val ttl = redisTemplate.getExpire(TTL_KEY, TimeUnit.MILLISECONDS)
            assertThat(ttl, equalTo(EXPECTED_NOT_EXPIRATION_TIME))
        }

        @Test
        fun `get with TTL returns the value if it has not expired`() {
            // Given
            redisTemplate.opsForValue().set(TTL_KEY, SAMPLE_VALUE, MEDIUM_TTL)

            // When
            val result = repository.get(TTL_KEY)

            // Then
            assertThat(result, equalTo(SAMPLE_VALUE))
        }

        @Test
        fun `get with TTL returns null if it has expired`() {
            // Given
            redisTemplate.opsForValue().set(EXPIRED_KEY, SAMPLE_VALUE, SHORT_TTL)
            Thread.sleep(SLEEP_IN_MILLIS)

            // When
            val result = repository.get(EXPIRED_KEY)

            // Then
            assertThat(result, nullValue())
        }
    }

    @Nested
    @DisplayName("Delete operations")
    inner class DeleteOperations {

        @Test
        fun `delete returns false when key does not exist`() {
            // Given
            assertThat(redisTemplate.hasKey(NON_EXISTING_KEY), equalTo(false))

            // When
            val result = repository.delete(NON_EXISTING_KEY)

            // Then
            assertThat(result, equalTo(false))
        }

        @Test
        fun `delete returns true when removes string value`() {
            // Given
            redisTemplate.opsForValue().set(EXISTING_KEY, SAMPLE_VALUE)

            // When
            val result = repository.delete(EXISTING_KEY)

            // Then
            assertThat(result, equalTo(true))
        }

        @Test
        fun `delete returns true when removes numeric value`() {
            // Given
            redisTemplate.opsForValue().increment(EXISTING_KEY)

            // When
            val result = repository.delete(EXISTING_KEY)

            // Then
            assertThat(result, equalTo(true))
        }

        @Test
        fun `delete returns true when removes ranked elements`() {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)

            // When
            val result = repository.delete(RANKING_KEY)

            // Then
            assertThat(result, equalTo(true))
        }
    }

    @Nested
    @DisplayName("Increment operations")
    inner class IncrementOperations {

        @Test
        fun `increment creates a new key set to 1 if there is no match`() {
            // Given
            assertThat(redisTemplate.opsForValue().get(EXISTING_KEY), nullValue())

            // When
            val result = repository.increment(EXISTING_KEY)

            // Then
            assertThat(result, equalTo(1L))
        }

        @Test
        fun `increment works as expected if there is match`() {
            // Given
            redisTemplate.opsForValue().set(EXISTING_KEY, "5")

            // When
            val result = repository.increment(EXISTING_KEY)

            // Then
            assertThat(result, equalTo(6L))
        }
    }

    @Nested
    @DisplayName("Ranked Element operations")
    inner class RankedElementOperations {

        @Test
        fun `setRankedElement return true when adds a value when there is no ranking`() {
            // Given
            assertThat(redisTemplate.opsForZSet().size(RANKING_KEY), equalTo(0L))

            // When
            val result = repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER)

            // Then
            assertThat(result, equalTo(true))
        }

        @Test
        fun `setRankedElement return true when adds a value when there is no match in ranking`() {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)

            // When
            val result = repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, SECOND_MEMBER)

            // Then
            assertThat(result, equalTo(true))
        }

        @Test
        fun `setRankedElement return false when updates a value when there is match in ranking`() {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)

            // When
            val result = repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, FIRST_MEMBER)

            // Then
            assertThat(result, equalTo(false))
        }

        @Test
        fun `getRankedElementPosition returns null when there is no ranking`() {
            // Given
            assertThat(redisTemplate.opsForZSet().size(RANKING_KEY), equalTo(0L))

            // When
            val result = repository.getRankedElementPosition(RANKING_KEY, SECOND_MEMBER)

            // Then
            assertThat(result, nullValue())
        }

        @Test
        fun `getRankedElementPosition returns null when there is no match in ranking`() {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)

            // When
            val result = repository.getRankedElementPosition(RANKING_KEY, SECOND_MEMBER)

            // Then
            assertThat(result, nullValue())
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "$SMALL_RANK, $MEDIUM_RANK, 0",
                "$LONG_RANK, $MEDIUM_RANK, 1",
            ]
        )
        fun `getRankedElementPosition returns the rank when there is match`(
            elementRank: Double,
            anotherRank: Double,
            expectedRank: Long
        ) {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, elementRank)
            redisTemplate.opsForZSet().add(RANKING_KEY, SECOND_MEMBER, anotherRank)

            // When
            val result = repository.getRankedElementPosition(RANKING_KEY, FIRST_MEMBER)

            // Then
            assertThat(result, equalTo(expectedRank))
        }

        @Test
        fun `getRankedElementPosition returns zero when there is only one element`() {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)

            // When
            val result = repository.getRankedElementPosition(RANKING_KEY, FIRST_MEMBER)

            // Then
            assertThat(result, equalTo(0L))
        }

        @Test
        fun `getRankedElementRange returns empty list when ranking does not exist`() {
            // Given
            assertThat(redisTemplate.opsForZSet().size(RANKING_KEY), equalTo(0L))

            // When
            val result = repository.getRankedElementRange(RANKING_KEY, 0, 2)

            // Then
            assertThat(result, emptyIterable())
        }

        @Test
        fun `getRankedElementRange returns correct range of members`() {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)
            redisTemplate.opsForZSet().add(RANKING_KEY, SECOND_MEMBER, MEDIUM_RANK)
            redisTemplate.opsForZSet().add(RANKING_KEY, THIRD_MEMBER, LONG_RANK)

            // When
            val result = repository.getRankedElementRange(RANKING_KEY, 0, 1)

            // Then
            // HINT: a set does not guarantee the order.
            assertThat(result, contains(FIRST_MEMBER, SECOND_MEMBER))
        }
    }

    @Nested
    @DisplayName("Cache management")
    inner class CacheManagement {

        @Test
        fun `getCacheKeyCount returns 0 when no key is stored`() {
            // When
            val result = repository.getCacheKeyCount()

            // Then
            assertThat(result, equalTo(0))
        }

        @Test
        fun `getCacheKeyCount works as expected`() {
            // Given
            redisTemplate.opsForValue().set(EXISTING_KEY, SAMPLE_VALUE)
            redisTemplate.opsForValue().set(NON_EXISTING_KEY, ANOTHER_VALUE)
            redisTemplate.opsForValue().set(TTL_KEY, SAMPLE_VALUE)

            // When
            val result = repository.getCacheKeyCount()

            // Then
            assertThat(result, equalTo(3))
        }

        @Test
        fun `getRankedElementCount returns 0 when ranking does not exist`() {
            // Given
            assertThat(redisTemplate.opsForZSet().size(RANKING_KEY), equalTo(0L))

            // When
            val result = repository.getRankedElementCount(RANKING_KEY)

            // Then
            assertThat(result, equalTo(0L))
        }

        @Test
        fun `getRankedElementCount works as expected`() {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)
            redisTemplate.opsForZSet().add(RANKING_KEY, SECOND_MEMBER, MEDIUM_RANK)
            redisTemplate.opsForZSet().add(RANKING_KEY, THIRD_MEMBER, LONG_RANK)

            // When
            val result = repository.getRankedElementCount(RANKING_KEY)

            // Then
            assertThat(result, equalTo(3L))
        }
    }

    @Nested
    @DisplayName("Exception handling")
    inner class ExceptionHandling {

        @Test
        fun `increment throws exception when key contains non-numeric value`() {
            // Given
            redisTemplate.opsForValue().set(EXISTING_KEY, SAMPLE_VALUE)

            // When & Then
            val exception = assertThrows<InvalidIncrementValueException> {
                repository.increment(EXISTING_KEY)
            }

            assertThat(
                exception.message,
                equalTo("Key cannot be increased, contains non integer or out of range value")
            )
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "-1, 5, $NEGATIVE_RANKED_RANGE_INDEX_ERROR_MESSAGE",
                "0, -1, $NEGATIVE_RANKED_RANGE_INDEX_ERROR_MESSAGE",
                "-1, -1, $NEGATIVE_RANKED_RANGE_INDEX_ERROR_MESSAGE",
                "5, 2, $INVERTED_RANKED_RANGE_INDEXES_ERROR_MESSAGE"
            ]
        )
        fun `getRankedElementRange throws exception with invalid range parameters`(
            start: Long,
            stop: Long,
            errorMessage: String
        ) {
            // Given
            redisTemplate.opsForZSet().add(RANKING_KEY, FIRST_MEMBER, SMALL_RANK)

            // When & Then
            val exception = assertThrows<InvalidCacheRangeException> {
                repository.getRankedElementRange(RANKING_KEY, start, stop)
            }

            assertThat(exception.message, equalTo(errorMessage))
        }
    }

    @Nested
    @DisplayName("Method interaction")
    inner class MethodInteraction {
        @Test
        fun `complete cache workflow integration test`() {
            // 1. Basic key-value operations
            assertThat(repository.get("user:123"), nullValue())
            repository.set("user:123", "John Doe")
            assertThat(repository.get("user:123"), equalTo("John Doe"))

            // 2. TTL operations
            repository.set("session:abc", "active", SHORT_TTL)
            assertThat(repository.get("session:abc"), equalTo("active"))
            Thread.sleep(MEDIUM_TTL)
            assertThat(repository.get("session:abc"), nullValue())

            // 3. Counter operations
            assertThat(repository.increment("page:views"), equalTo(1L))
            assertThat(repository.increment("page:views"), equalTo(2L))

            // 4. Ranking operations
            repository.setRankedElement("leaderboard", 100.0, "player1")
            repository.setRankedElement("leaderboard", 200.0, "player2")
            assertThat(repository.getRankedElementPosition("leaderboard", "player1"), equalTo(0L))
            assertThat(
                repository.getRankedElementRange("leaderboard", 0, 1),
                equalTo(listOf("player1", "player2"))
            )

            // 5. Delete operations
            assertThat(repository.delete("user:123"), equalTo(true))
            assertThat(repository.get("user:123"), nullValue())

            // 6. Count operations
            assertThat(repository.getCacheKeyCount(), equalTo(2))
            assertThat(repository.getRankedElementCount("leaderboard"), equalTo(2L))

            // 7. Delete ranks and increments
            assertThat(repository.delete("leaderboard"), equalTo(true))
            assertThat(repository.delete("page:views"), equalTo(true))
            assertThat(repository.get("leaderboard"), nullValue())
            assertThat(repository.get("leaderboard"), nullValue())
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.getRankedElementCount("leaderboard"), equalTo(0L))
        }
    }

    @Nested
    @DisplayName("Method interaction")
    inner class ConcurrencyInteraction {
        @Test
        fun `cache operations are thread safe with concurrent access`() {
            val numberOfThreads = 10
            val operationsPerThread = 100
            val executor = Executors.newFixedThreadPool(numberOfThreads)
            val futures = mutableListOf<Future<*>>()

            // Test concurrent increments
            repeat(numberOfThreads) { threadId ->
                val future = executor.submit {
                    repeat(operationsPerThread) {
                        repository.increment("counter")
                        repository.setRankedElement("scores", threadId.toDouble(), "thread-$threadId")
                    }
                }
                futures.add(future)
            }

            // Wait for all threads to complete
            futures.forEach { it.get() }
            executor.shutdown()

            // Verify atomicity - counter should be exactly numberOfThreads * operationsPerThread
            val finalCount = repository.get("counter")?.toLong()
            assertThat(finalCount, equalTo(numberOfThreads * operationsPerThread.toLong()))

            // Verify ranking operations completed successfully
            val rankingSize = repository.getRankedElementCount("scores")
            assertThat(rankingSize, equalTo(numberOfThreads.toLong()))
        }
    }
}
