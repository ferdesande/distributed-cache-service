package com.fsg.cacheservice.api

import com.fsg.cacheservice.api.dto.RankingMemberDto
import com.fsg.cacheservice.api.helper.DataMother.ANOTHER_RANKING_MEMBER
import com.fsg.cacheservice.api.helper.DataMother.HIGH_SCORE
import com.fsg.cacheservice.api.helper.DataMother.LOW_SCORE
import com.fsg.cacheservice.api.helper.DataMother.MEDIUM_SCORE
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_KEY
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_RANKING_MEMBER
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_RANKING_MEMBER_DTO
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_VALUE
import com.fsg.cacheservice.api.helper.DataMother.TIMESTAMP
import com.fsg.cacheservice.api.helper.ValidatableResponseHelper.expectBadRequestResponse
import com.fsg.cacheservice.api.helper.ValidatableResponseHelper.expectErrorResponse
import com.fsg.cacheservice.core.ValueGenerator
import com.fsg.cacheservice.testcontainers.RedisTestBase
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.ValidatableResponse
import org.apache.http.HttpStatus
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CacheControllerIntegrationTest : RedisTestBase() {

    companion object {
        private const val KEY_PATH_PARAMETER = "key"
        private const val MEMBER_PATH_PARAMETER = "member"
        private const val KEY_VALUE_PATH = "/{$KEY_PATH_PARAMETER}"
        private const val RANKING_BASE_PATH = "/{$KEY_PATH_PARAMETER}/ranking"
        private const val KEY_COUNT_PATH = "/keys/count"
        private const val ANOTHER_VALUE = "another value"
        private const val SHORT_TTL = 1
        private const val DEFAULT_TTL = 300
        private const val SHORT_DELAY_IN_MILLIS = 1100L

        @DynamicPropertySource
        @JvmStatic
        @Suppress("Unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configureRedisProperties(registry)
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var valueGenerator: ValueGenerator

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    @DisplayName("[GET] /{key}]")
    inner class GetKeyOperations {

        @Test
        fun `returns 200 and the value when key exist in cache`() {
            redisTemplate.opsForValue().set(SAMPLE_KEY, SAMPLE_VALUE)

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                get(KEY_VALUE_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo(SAMPLE_VALUE))
            }
        }

        @Test
        fun `returns 404 when key does not exist in cache`() {
            configureErrorTimestamp()
            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), nullValue())

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                get(KEY_VALUE_PATH)
            } Then {
                expectKeyNotFound(path = "/$SAMPLE_KEY")
            }
        }
    }

    @Nested
    @DisplayName("[PUT] /{key}")
    inner class SetKeyOperations {

        @Test
        fun `returns 200 when setting value without TTL`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(SAMPLE_VALUE)
            } When {
                put(KEY_VALUE_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("OK"))
            }

            val storedValue = redisTemplate.opsForValue().get(SAMPLE_KEY)
            assertThat(storedValue, equalTo(SAMPLE_VALUE))

            // -1 means no expiration
            assertThat(redisTemplate.getExpire(SAMPLE_KEY), equalTo(-1L))
        }

        @Test
        fun `returns 200 when setting overrides and existing`() {
            redisTemplate.opsForValue().set(SAMPLE_VALUE, SAMPLE_VALUE)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(ANOTHER_VALUE)
            } When {
                put(KEY_VALUE_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("OK"))
            }

            val storedValue = redisTemplate.opsForValue().get(SAMPLE_KEY)
            assertThat(storedValue, equalTo(ANOTHER_VALUE))

            // -1 means no expiration
            assertThat(redisTemplate.getExpire(SAMPLE_KEY), equalTo(-1L))
        }

        @Test
        fun `returns 200 when setting value with TTL`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                queryParam("ttl", SHORT_TTL)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(SAMPLE_VALUE)
            } When {
                put(KEY_VALUE_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("OK"))
            }

            // Verify value was stored and expires after TTL
            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), equalTo(SAMPLE_VALUE))
            Thread.sleep(SHORT_DELAY_IN_MILLIS)
            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), nullValue())
        }

        @Test
        fun `returns 200 and removes TTL when setting overrides value without TTL`() {
            redisTemplate.opsForValue()
                .set(SAMPLE_KEY, SAMPLE_VALUE, Duration.ofMinutes(DEFAULT_TTL.toLong()))
            assertThat(redisTemplate.getExpire(SAMPLE_KEY), greaterThan(0L))

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(SAMPLE_VALUE)
            } When {
                put(KEY_VALUE_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("OK"))
            }

            // -1 means no expiration
            assertThat(redisTemplate.getExpire(SAMPLE_KEY), equalTo(-1L))
        }
    }

    @Nested
    @DisplayName("[DELETE] /{key}")
    inner class DeleteKeyOperations {

        @Test
        fun `returns 200 when deleting existing key`() {
            redisTemplate.opsForValue().set(SAMPLE_KEY, SAMPLE_VALUE)

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                delete(KEY_VALUE_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("OK"))
            }

            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), nullValue())
        }

        @Test
        fun `returns 404 when deleting non-existing key`() {
            configureErrorTimestamp()
            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), nullValue())

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                delete(KEY_VALUE_PATH)
            } Then {
                expectKeyNotFound(path = "/$SAMPLE_KEY")
            }
        }
    }

    @Nested
    @DisplayName("[PUT] /{key}/increment")
    inner class IncrementCounterOperations {

        @Test
        fun `returns 200 with 1 when incrementing non-existing key`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                put("/{$KEY_PATH_PARAMETER}/increment")
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("1"))
            }

            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), equalTo("1"))
        }

        @Test
        fun `returns 200 with incremented value when key exists with numeric value`() {
            redisTemplate.opsForValue().set(SAMPLE_KEY, "5")

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                put("/{$KEY_PATH_PARAMETER}/increment")
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("6"))
            }

            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), equalTo("6"))
        }

        @Test
        fun `returns 400 when key exists with non-numeric value`() {
            configureErrorTimestamp()
            redisTemplate.opsForValue().set(SAMPLE_KEY, "not-a-number")

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                put("$KEY_VALUE_PATH/increment")
            } Then {
                expectBadRequestResponse(
                    message = "Key cannot be increased, contains non integer or out of range value",
                    path = "/$SAMPLE_KEY/increment",
                )
            }
        }
    }

    @Nested
    @DisplayName("[POST] /{key}/ranking")
    inner class PostRankingOperations {

        @Test
        fun `returns 201 when adding new member to ranking`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body(SAMPLE_RANKING_MEMBER_DTO)
            } When {
                post(RANKING_BASE_PATH)
            } Then {
                statusCode(HttpStatus.SC_CREATED)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("OK"))
            }

            val score = redisTemplate.opsForZSet().score(SAMPLE_KEY, SAMPLE_RANKING_MEMBER)
            assertThat(score, equalTo(LOW_SCORE))
        }

        @Test
        fun `returns 200 when updating existing member in ranking`() {
            redisTemplate.opsForZSet().add(SAMPLE_KEY, SAMPLE_RANKING_MEMBER, LOW_SCORE)

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body(RankingMemberDto(SAMPLE_RANKING_MEMBER, MEDIUM_SCORE))
            } When {
                post(RANKING_BASE_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("OK"))
            }

            val score = redisTemplate.opsForZSet().score(SAMPLE_KEY, SAMPLE_RANKING_MEMBER)
            assertThat(score, equalTo(MEDIUM_SCORE))
        }
    }

    @Nested
    @DisplayName("[GET] /{key}/ranking/{member}/rank")
    inner class GetMemberRankOperations {

        private val getPath = "$RANKING_BASE_PATH/{$MEMBER_PATH_PARAMETER}/rank"

        @ParameterizedTest
        @CsvSource(
            value = [
                "$SAMPLE_RANKING_MEMBER, 0",
                "$ANOTHER_RANKING_MEMBER, 1",
                "player3, 2"
            ]
        )
        fun `returns 200 with rank 0-based rank when get rank for member`(member: String, rank: String) {
            redisTemplate.opsForZSet().add(SAMPLE_KEY, SAMPLE_RANKING_MEMBER, LOW_SCORE)
            redisTemplate.opsForZSet().add(SAMPLE_KEY, ANOTHER_RANKING_MEMBER, MEDIUM_SCORE)
            redisTemplate.opsForZSet().add(SAMPLE_KEY, "player3", HIGH_SCORE)

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                pathParam(MEMBER_PATH_PARAMETER, member)
            } When {
                get(getPath)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo(rank))
            }
        }

        @Test
        fun `returns 404 when member does not exist in ranking`() {
            configureErrorTimestamp()
            redisTemplate.opsForZSet().add(SAMPLE_KEY, SAMPLE_RANKING_MEMBER, 100.0)

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                pathParam(MEMBER_PATH_PARAMETER, "non-existing-player")
            } When {
                get(getPath)
            } Then {
                expectErrorResponse(
                    status = HttpStatus.SC_NOT_FOUND,
                    code = "NOT_FOUND",
                    message = "Member non-existing-player not found in ranking",
                    path = "/$SAMPLE_KEY/ranking/non-existing-player/rank"
                )
            }
        }

        @Test
        fun `returns 404 when ranking key does not exist`() {
            configureErrorTimestamp()
            assertThat(redisTemplate.opsForValue().get(SAMPLE_KEY), nullValue())

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                pathParam(MEMBER_PATH_PARAMETER, SAMPLE_RANKING_MEMBER)
            } When {
                get(getPath)
            } Then {
                expectKeyNotFound("/$SAMPLE_KEY/ranking/$SAMPLE_RANKING_MEMBER/rank")
            }
        }
    }

    @Nested
    @DisplayName("[GET] /{key}/ranking/range")
    inner class GetRankingRangeOperations {

        @ParameterizedTest
        @CsvSource(
            value = [
                "0, 3, player1:player2:player3:player4",
                "1, 2, player2:player3",
                "2, 2, player3",
                "0, 7, player1:player2:player3:player4",
                "0, 1, player1:player2",
                "6, 7, null"
            ], nullValues = ["null"]
        )
        fun `returns 200 and the requested members`(start: Int, stop: Int, members: String?) {
            // easier to read when values instead of with constants
            redisTemplate.opsForZSet().add(SAMPLE_KEY, "player1", LOW_SCORE)
            redisTemplate.opsForZSet().add(SAMPLE_KEY, "player2", MEDIUM_SCORE)
            redisTemplate.opsForZSet().add(SAMPLE_KEY, "player3", HIGH_SCORE)
            redisTemplate.opsForZSet().add(SAMPLE_KEY, "player4", HIGH_SCORE + 1000)

            val expectedMembers = members?.split(":") ?: emptyList()

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                queryParam("start", start)
                queryParam("stop", stop)
            } When {
                get("$RANKING_BASE_PATH/range")
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body("", equalTo(expectedMembers))
            }
        }

        @Test
        fun `returns 200 with empty array when ranking key does not exist`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, "non-existing-ranking")
                queryParam("start", 0)
                queryParam("stop", 5)
            } When {
                get("$RANKING_BASE_PATH/range")
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body("", equalTo(emptyList<String>()))
            }
        }
    }

    @Nested
    @DisplayName("[GET] /{key}/ranking/count")
    inner class GetRankingCountOperations {

        @Test
        fun `returns 200 with 0 when ranking is empty`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                get("$RANKING_BASE_PATH/count")
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("0"))
            }
        }

        @Test
        fun `returns 200 with 1 when ranking has single member`() {
            redisTemplate.opsForZSet().add(SAMPLE_KEY, SAMPLE_RANKING_MEMBER, LOW_SCORE)

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                get("$RANKING_BASE_PATH/count")
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("1"))
            }
        }

        @Test
        fun `returns 200 with correct count when ranking has multiple members`() {
            redisTemplate.opsForZSet().add(SAMPLE_KEY, SAMPLE_RANKING_MEMBER, LOW_SCORE)
            redisTemplate.opsForZSet().add(SAMPLE_KEY, ANOTHER_RANKING_MEMBER, MEDIUM_SCORE)
            redisTemplate.opsForZSet().add(SAMPLE_KEY, "player3", HIGH_SCORE)

            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                get("$RANKING_BASE_PATH/count")
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("3"))
            }
        }
    }

    @Nested
    @DisplayName("[GET] /keys/count")
    inner class GetKeyCountOperations {

        @Test
        fun `returns 200 with 0 when cache is empty`() {
            When {
                get(KEY_COUNT_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("0"))
            }
        }

        @Test
        fun `returns 200 with 1 when cache has single key`() {
            redisTemplate.opsForValue().set(SAMPLE_KEY, SAMPLE_VALUE)

            When {
                get(KEY_COUNT_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("1"))
            }
        }

        @Test
        fun `returns 200 with correct count when cache has multiple keys`() {
            redisTemplate.opsForValue().set(SAMPLE_KEY, SAMPLE_VALUE)
            redisTemplate.opsForValue().set("another-key", ANOTHER_VALUE)
            redisTemplate.opsForZSet().add("ranking-key", SAMPLE_RANKING_MEMBER, LOW_SCORE)

            When {
                get(KEY_COUNT_PATH)
            } Then {
                statusCode(HttpStatus.SC_OK)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(equalTo("3"))
            }
        }
    }

    private fun configureErrorTimestamp() {
        whenever(valueGenerator.now()).thenReturn(TIMESTAMP)
    }

    private fun ValidatableResponse.expectKeyNotFound(
        path: String
    ): ValidatableResponse = this.expectErrorResponse(
        status = HttpStatus.SC_NOT_FOUND,
        code = "NOT_FOUND",
        message = "Key $SAMPLE_KEY not found",
        path = path
    )
}
