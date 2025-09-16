package com.fsg.cacheservice.core

import com.fsg.cacheservice.core.exception.BadRequestException
import com.fsg.cacheservice.core.exception.InvalidValueException
import com.fsg.cacheservice.core.exception.OverflowException
import com.fsg.cacheservice.core.exception.WrongTypeException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.emptyIterable
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future

abstract class CacheRepositoryTestAcceptanceTest {

    companion object {
        private const val SAMPLE_KEY = "a-valid-sample-key"
        private const val ANOTHER_KEY = "another-valid-sample-key"
        private const val NON_EXISTING_KEY = "another-key"
        private const val SAMPLE_VALUE = "An entry value"
        private const val ANOTHER_VALUE = "Another entry value"
        private const val RANKING_KEY = "ranking-test-key"

        private val SHORT_TTL = Duration.ofMillis(10)
        private val MEDIUM_TTL = Duration.ofMillis(100)

        private const val SMALLEST_RANK = 1.0
        private const val SMALL_RANK = 2.0
        private const val MEDIUM_RANK = 5.0
        private const val HIGH_RANK = 10.0
        private const val HIGHEST_RANK = 15.0

        private const val FIRST_MEMBER = "first-member"
        private const val SECOND_MEMBER = "second-member"
        private const val THIRD_MEMBER = "third-member"
        private const val FORTH_MEMBER = "forth-member"

        private const val SLEEP_IN_MILLIS = 20L
    }

    private lateinit var repository: CacheRepository

    protected abstract fun setCacheRepository(): CacheRepository

    @BeforeEach
    fun setUp() {
        repository = setCacheRepository()
    }

    @Nested
    @DisplayName("Key-Value operations")
    inner class KeyValueOperations {

        @Test
        fun `set and get works as expected`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When
            repository.set(SAMPLE_KEY, SAMPLE_VALUE)
            repository.set(ANOTHER_KEY, ANOTHER_VALUE)

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo(SAMPLE_VALUE))
            assertThat(repository.get(ANOTHER_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(2))
        }

        @Test
        fun `override keys works as expected`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When
            repository.set(SAMPLE_KEY, SAMPLE_VALUE)
            repository.set(SAMPLE_KEY, ANOTHER_VALUE)

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `set should override a ranking`() {
            // Given
            assertThat(repository.getRankedElementCount(SAMPLE_KEY), equalTo(0L))
            repository.setRankedElement(SAMPLE_KEY, SMALL_RANK, FIRST_MEMBER)

            // When
            repository.set(SAMPLE_KEY, SAMPLE_VALUE)

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo(SAMPLE_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `get throws WrongTypeException when keys belongs to a raking`() {
            // Given
            repository.setRankedElement(SAMPLE_KEY, SMALL_RANK, FIRST_MEMBER)

            // When / Then
            val exception = assertThrows<WrongTypeException> { repository.get(SAMPLE_KEY) }

            // Then
            assertThat(exception.message, equalTo("The value for key '$SAMPLE_KEY' is not a String"))
        }
    }

    @Nested
    @DisplayName("TTL operations")
    inner class TTLOperations {

        @Test
        fun `set and get with TTL works as expected`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When
            repository.set(SAMPLE_KEY, SAMPLE_VALUE, SHORT_TTL)
            repository.set(ANOTHER_KEY, ANOTHER_VALUE)

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo(SAMPLE_VALUE))
            assertThat(repository.get(ANOTHER_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(2))

            Thread.sleep(SLEEP_IN_MILLIS)

            assertThat(repository.get(SAMPLE_KEY), nullValue())
            assertThat(repository.get(ANOTHER_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `override keys without TTL removes the TTL`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When
            repository.set(SAMPLE_KEY, SAMPLE_VALUE, SHORT_TTL)
            repository.set(SAMPLE_KEY, ANOTHER_VALUE)

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(1))

            Thread.sleep(SLEEP_IN_MILLIS)

            assertThat(repository.get(SAMPLE_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `override keys with TTL adds the TTL`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When
            repository.set(SAMPLE_KEY, SAMPLE_VALUE)
            repository.set(SAMPLE_KEY, ANOTHER_VALUE, SHORT_TTL)

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(1))

            Thread.sleep(SLEEP_IN_MILLIS)

            assertThat(repository.get(SAMPLE_KEY), nullValue())
            assertThat(repository.getCacheKeyCount(), equalTo(0))
        }

        @Test
        fun `get key count does not include expired keys`() {
            // Given
            repository.set(SAMPLE_KEY, SAMPLE_VALUE, SHORT_TTL)
            assertThat(repository.getCacheKeyCount(), equalTo(1))
            Thread.sleep(SLEEP_IN_MILLIS)

            // When / Then
            assertThat(repository.getCacheKeyCount(), equalTo(0))
        }

        @ParameterizedTest
        @ValueSource(longs = [0L, -1L, -100L])
        fun `set with invalid TTL throws bad request exception`(duration: Long) {
            // When
            val exception = assertThrows<BadRequestException> {
                repository.set(SAMPLE_KEY, SAMPLE_VALUE, Duration.ofMillis(duration))
            }

            // Then
            assertThat(exception.message, equalTo("Expiration time must greater than zero"))
        }
    }

    @Nested
    @DisplayName("Delete operations")
    inner class DeleteOperations {

        @Test
        fun `delete existing key removes the string value and return true`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            repository.set(SAMPLE_KEY, SAMPLE_VALUE)
            repository.set(ANOTHER_KEY, ANOTHER_VALUE)

            // When / Then
            assertThat(repository.delete(SAMPLE_KEY), equalTo(true))

            // Then
            assertThat(repository.get(SAMPLE_KEY), nullValue())
            assertThat(repository.get(ANOTHER_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `delete existing key removes the increment and return true`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.increment(SAMPLE_KEY), equalTo(1))
            assertThat(repository.increment(ANOTHER_KEY), equalTo(1))

            // When / Then
            assertThat(repository.delete(SAMPLE_KEY), equalTo(true))

            // Then
            assertThat(repository.get(SAMPLE_KEY), nullValue())
            assertThat(repository.get(ANOTHER_KEY), equalTo("1"))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `delete existing key removes the raking and return true`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.setRankedElement(SAMPLE_KEY, SMALL_RANK, FIRST_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(ANOTHER_KEY, MEDIUM_RANK, SECOND_MEMBER), equalTo(true))

            // When
            assertThat(repository.delete(SAMPLE_KEY), equalTo(true))

            // Then
            assertThat(repository.get(SAMPLE_KEY), nullValue())
            assertThat(repository.getRankedElementCount(SAMPLE_KEY), equalTo(0))
            assertThat(repository.getRankedElementCount(ANOTHER_KEY), equalTo(1))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `delete non-existing key return false`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When
            repository.set(SAMPLE_KEY, SAMPLE_VALUE)
            repository.set(ANOTHER_KEY, ANOTHER_VALUE)

            // When / Then
            assertThat(repository.delete(NON_EXISTING_KEY), equalTo(false))

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo(SAMPLE_VALUE))
            assertThat(repository.get(ANOTHER_KEY), equalTo(ANOTHER_VALUE))
            assertThat(repository.getCacheKeyCount(), equalTo(2))
        }
    }

    @Nested
    @DisplayName("Increment operations")
    inner class IncrementOperations {

        @Test
        fun `increment creates a new key set to 1 if does not exist`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When / Then
            assertThat(repository.increment(SAMPLE_KEY), equalTo(1))

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo("1"))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `multiple increment works`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.get(SAMPLE_KEY), nullValue())

            // When / Then
            repeat(5) { i -> assertThat(repository.increment(SAMPLE_KEY), equalTo(i.toLong() + 1)) }

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo("5"))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `increment adds one unit to a key if already exists as a valid numeric String`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            repository.set(SAMPLE_KEY, "-19")

            // When / Then
            assertThat(repository.increment(SAMPLE_KEY), equalTo(-18))

            // Then
            assertThat(repository.get(SAMPLE_KEY), equalTo("-18"))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `increment throws OverflowException when value overflows 64-bits value`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            repository.set(SAMPLE_KEY, Long.MAX_VALUE.toString())

            // When / Then
            val exception = assertThrows<OverflowException> { repository.increment(SAMPLE_KEY) }

            // Then
            assertThat(
                exception.message,
                equalTo("Increment for key '$SAMPLE_KEY' cannot be done because result overflows 64-bits value")
            )
            assertThat(repository.get(SAMPLE_KEY), equalTo(Long.MAX_VALUE.toString()))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `increment throws InvalidValueException when value for key string is not a number`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            repository.set(SAMPLE_KEY, "not-a-number")

            // When / Then
            val exception = assertThrows<InvalidValueException> { repository.increment(SAMPLE_KEY) }

            // Then
            assertThat(exception.message, equalTo("The value for key '$SAMPLE_KEY' is not a number"))
            assertThat(repository.get(SAMPLE_KEY), equalTo("not-a-number"))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `increment throws WrongTypeException when value for key is a ranking`() {
            // Given
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            repository.setRankedElement(SAMPLE_KEY, SMALL_RANK, FIRST_MEMBER)

            // When / Then
            val exception = assertThrows<WrongTypeException> { repository.increment(SAMPLE_KEY) }

            // Then
            assertThat(exception.message, equalTo("The value for key '$SAMPLE_KEY' is not a number"))
            assertThat(repository.getRankedElementCount(SAMPLE_KEY), equalTo(1))
        }
    }

    @Nested
    @DisplayName("Ranked Element operations")
    inner class RankedElementOperations {

        @Test
        fun `set ranked element return true when adds an element which is not in the ranking`() {
            // Given
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(0L))

            // When / Then
            assertThat(repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER), equalTo(true))
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(1L))
        }

        @Test
        fun `set ranked element return false when adds an element which is already in the ranking`() {
            // Given
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(0L))
            repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER)

            // When
            assertThat(repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, FIRST_MEMBER), equalTo(false))
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(1L))
        }

        @Test
        fun `elements are return in order after update member score`() {
            // Given
            assertThat(repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, FIRST_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, HIGHEST_RANK, SECOND_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, SMALL_RANK, THIRD_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, HIGH_RANK, FORTH_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, SMALLEST_RANK, FORTH_MEMBER), equalTo(false))

            // When
            val sortedMembers = repository.getRankedElementRange(RANKING_KEY, 0, 3)

            // Then
            assertThat(sortedMembers, contains(FORTH_MEMBER, THIRD_MEMBER, FIRST_MEMBER, SECOND_MEMBER))
        }

        @Test
        fun `set ranked element throws an exception when try to override an string value`() {
            // Given
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(0L))
            repository.set(RANKING_KEY, SAMPLE_VALUE)

            // When
            val exception = assertThrows<WrongTypeException> {
                repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER)
            }

            // Then
            assertThat(exception.message, equalTo("The value for key '$RANKING_KEY' is not a Ranking"))
            assertThat(repository.getCacheKeyCount(), equalTo(1))
        }

        @Test
        fun `get ranked position returns the correct 0-based position`() {
            // Given
            assertThat(repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, FIRST_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, HIGHEST_RANK, SECOND_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, SMALL_RANK, THIRD_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, HIGH_RANK, FORTH_MEMBER), equalTo(true))

            // When / Then
            assertThat(repository.getRankedElementPosition(RANKING_KEY, FIRST_MEMBER), equalTo(1))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, SECOND_MEMBER), equalTo(3))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, THIRD_MEMBER), equalTo(0))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, FORTH_MEMBER), equalTo(2))
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(4L))
        }

        @Test
        fun `get ranked position returns the correct 0-based position with duplicated rank`() {
            // Given
            assertThat(repository.setRankedElement(RANKING_KEY, HIGHEST_RANK, FIRST_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, THIRD_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, SECOND_MEMBER), equalTo(true))
            assertThat(repository.setRankedElement(RANKING_KEY, HIGH_RANK, FORTH_MEMBER), equalTo(true))

            // When / Then
            assertThat(repository.getRankedElementPosition(RANKING_KEY, FIRST_MEMBER), equalTo(3))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, SECOND_MEMBER), equalTo(0))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, THIRD_MEMBER), equalTo(1))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, FORTH_MEMBER), equalTo(2))
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(4L))
        }

        @Test
        fun `get Ranked Element Position returns null when there is no ranking`() {
            // Given
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(0L))

            // When / Then
            assertThat(repository.getRankedElementPosition(RANKING_KEY, FIRST_MEMBER), nullValue())
        }

        @Test
        fun `get Ranked Element Position returns null when member is not in the ranking`() {
            // Given
            repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER)
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(1L))

            // When / Then
            assertThat(repository.getRankedElementPosition(RANKING_KEY, SECOND_MEMBER), nullValue())
        }

        @Test
        fun `returned ranked position is update when member score is updated`() {
            // Given
            repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER)
            repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, SECOND_MEMBER)
            repository.setRankedElement(RANKING_KEY, HIGH_RANK, THIRD_MEMBER)

            // When
            assertThat(repository.setRankedElement(RANKING_KEY, HIGHEST_RANK, SECOND_MEMBER), equalTo(false))

            // Then
            assertThat(repository.getRankedElementPosition(RANKING_KEY, FIRST_MEMBER), equalTo(0L))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, SECOND_MEMBER), equalTo(2L))
            assertThat(repository.getRankedElementPosition(RANKING_KEY, THIRD_MEMBER), equalTo(1L))
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(3L))
        }

        @Test
        fun `getRankedElementPosition returns zero when there is only one element`() {
            // Given
            repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER)
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(1L))

            // When / Then
            assertThat(repository.getRankedElementPosition(RANKING_KEY, FIRST_MEMBER), equalTo(0L))
        }

        @Test
        fun `getRankedElementRange return empty list when ranking does not exist`() {
            // Given
            assertThat(repository.getRankedElementCount(RANKING_KEY), equalTo(0L))

            // When / Then
            assertThat(repository.getRankedElementRange(RANKING_KEY, 0, 2), emptyIterable())
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "0, 3, $FIRST_MEMBER; $SECOND_MEMBER; $THIRD_MEMBER; $FORTH_MEMBER",
                "1, 2, $SECOND_MEMBER; $THIRD_MEMBER",
                "1, 1, $SECOND_MEMBER",
                "1, 13, $SECOND_MEMBER; $THIRD_MEMBER; $FORTH_MEMBER",
                "0, -1, $FIRST_MEMBER; $SECOND_MEMBER; $THIRD_MEMBER; $FORTH_MEMBER",
                "-2, -1, $THIRD_MEMBER; $FORTH_MEMBER",
                "0, -3, $FIRST_MEMBER; $SECOND_MEMBER",
                "3, 1, ''",
                "0, -5, ''",
            ]
        )
        fun `getRankedElementRange returns correct range of members`(start: Long, stop: Long, expectedResult: String) {
            // Given
            repository.setRankedElement(RANKING_KEY, SMALL_RANK, FIRST_MEMBER)
            repository.setRankedElement(RANKING_KEY, MEDIUM_RANK, SECOND_MEMBER)
            repository.setRankedElement(RANKING_KEY, HIGH_RANK, THIRD_MEMBER)
            repository.setRankedElement(RANKING_KEY, HIGHEST_RANK, FORTH_MEMBER)

            // When
            val result = repository.getRankedElementRange(RANKING_KEY, start, stop).joinToString(separator = "; ")

            // Then
            assertThat(result, equalTo(expectedResult))
        }

        @Test
        fun `getRankedElementRange throws WrongTypeException when key is string`() {
            // Given
            repository.set(SAMPLE_KEY, SAMPLE_VALUE)

            // When / Then
            val exception = assertThrows<WrongTypeException> {
                repository.getRankedElementRange(SAMPLE_KEY, 0, 1)
            }

            assertThat(exception.message, equalTo("The value for key '$SAMPLE_KEY' is not a Ranking"))
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
            assertThat(repository.getCacheKeyCount(), equalTo(0))
            assertThat(repository.getRankedElementCount("leaderboard"), equalTo(0L))
        }
    }

    @Nested
    @DisplayName("Concurrency interaction")
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
