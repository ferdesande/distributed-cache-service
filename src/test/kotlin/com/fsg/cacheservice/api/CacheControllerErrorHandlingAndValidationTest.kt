package com.fsg.cacheservice.api

import com.fsg.cacheservice.api.dto.RankingMemberDto
import com.fsg.cacheservice.api.helper.DataMother.ANY_CACHE_EXCEPTION
import com.fsg.cacheservice.api.helper.DataMother.LOW_SCORE
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_KEY
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_RANKING_MEMBER
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_RANKING_MEMBER_DTO
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_VALUE
import com.fsg.cacheservice.api.helper.DataMother.TIMESTAMP
import com.fsg.cacheservice.api.helper.ValidatableResponseHelper.expectBadRequestResponse
import com.fsg.cacheservice.api.helper.ValidatableResponseHelper.expectInternalServerErrorResponse
import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.ValueGenerator
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.ValidatableResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CacheControllerErrorHandlingAndValidationTest {

    companion object {
        private const val KEY_PATH_PARAMETER = "key"
        private const val MEMBER_PATH_PARAMETER = "member"
        private const val KEY_VALUE_PATH = "/{$KEY_PATH_PARAMETER}"
        private const val RANKING_BASE_PATH = "/{$KEY_PATH_PARAMETER}/ranking"
        private const val KEY_COUNT_PATH = "/keys/count"

        private const val CACHE_ERROR_CODE = "CACHE_ERROR"
        private const val MEMBER_KEY_EXCEPTION_MESSAGE = "Validation failed: Member cannot be blank"
        private const val BLANK_KEY_EXCEPTION_MESSAGE = "Validation failed: Key cannot be blank"
    }

    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var cacheRepository: CacheRepository

    @MockitoBean
    private lateinit var valueGenerator: ValueGenerator

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        whenever(valueGenerator.now()).thenReturn(TIMESTAMP)
    }

    @Nested
    @DisplayName("[GET] /{key}]")
    inner class GetKeyOperations {
        @Test
        fun `returns 400 when key has escaped white spaces`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, " ")
            } When {
                get(KEY_VALUE_PATH)
            } Then {
                expectBlankKeyBadRequestResponse("/%20")
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.get(SAMPLE_KEY)).thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                get(KEY_VALUE_PATH)
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY")
            }
        }
    }

    @Nested
    @DisplayName("[PUT] /{key}")
    inner class SetKeyOperations {

        @Test
        fun `returns 400 when key is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, " ")
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(SAMPLE_VALUE)
            } When {
                put(KEY_VALUE_PATH)
            } Then {
                expectBlankKeyBadRequestResponse("/%20")
            }
        }

        @Test
        fun `returns 400 when TTL is invalid`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                queryParam("ttl", 0)
                contentType(MediaType.TEXT_PLAIN_VALUE)
                body(SAMPLE_VALUE)
            } When {
                put(KEY_VALUE_PATH)
            } Then {
                expectBadRequestResponse(
                    message = "Validation failed: Time-to-live must be greater than zero",
                    path = "/$SAMPLE_KEY"
                )
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.set(SAMPLE_KEY, SAMPLE_VALUE)).thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                body(SAMPLE_VALUE)
                body(SAMPLE_VALUE)
            } When {
                put(KEY_VALUE_PATH)
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY")
            }
        }
    }

    @Nested
    @DisplayName("[DELETE] /{key}")
    inner class DeleteKeyOperations {

        @Test
        fun `returns 400 when key is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, " ")
            } When {
                delete(KEY_VALUE_PATH)
            } Then {
                expectBlankKeyBadRequestResponse("/%20")
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.delete(SAMPLE_KEY)).thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                delete(KEY_VALUE_PATH)
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY")
            }
        }
    }

    @Nested
    @DisplayName("[PUT] /{key}/increment")
    inner class IncrementCounterOperations {
        @Test
        fun `returns 400 when key is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, " ")
            } When {
                put("$KEY_VALUE_PATH/increment")
            } Then {
                expectBlankKeyBadRequestResponse("/%20/increment")
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.increment(SAMPLE_KEY)).thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                put("$KEY_VALUE_PATH/increment")
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY/increment")
            }
        }
    }

    @Nested
    @DisplayName("[POST] /{key}/ranking")
    inner class PostRankingOperations {

        @Test
        fun `returns 400 when key is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, " ")
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body(SAMPLE_RANKING_MEMBER_DTO)
            } When {
                post(RANKING_BASE_PATH)
            } Then {
                expectBlankKeyBadRequestResponse("/%20/ranking")
            }
        }

        @Test
        fun `returns 400 when member name is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body(RankingMemberDto(" ", LOW_SCORE))
            } When {
                post(RANKING_BASE_PATH)
            } Then {
                expectBadRequestResponse(
                    message = "Validation failed: Field member in RankingMemberDto cannot be blank",
                    path = "/$SAMPLE_KEY/ranking"
                )
            }
        }

        @Test
        fun `returns 400 when request body is invalid`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body("{\"invalid\": \"json\"}")
            } When {
                post(RANKING_BASE_PATH)
            } Then {
                expectBadRequestResponse(
                    message = "Body has an invalid JSON format",
                    path = "/$SAMPLE_KEY/ranking"
                )
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.setRankedElement(eq(SAMPLE_KEY), anyDouble(), anyString()))
                .thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                contentType(MediaType.APPLICATION_JSON_VALUE)
                body(SAMPLE_RANKING_MEMBER_DTO)
            } When {
                post(RANKING_BASE_PATH)
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY/ranking")
            }
        }
    }

    @Nested
    @DisplayName("[GET] /{key}/ranking/{member}/rank")
    inner class GetMemberRankOperations {
        private val getPath = "$RANKING_BASE_PATH/{$MEMBER_PATH_PARAMETER}/rank"

        @Test
        fun `returns 400 when key is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, " ")
                pathParam(MEMBER_PATH_PARAMETER, SAMPLE_RANKING_MEMBER)
            } When {
                get(getPath)
            } Then {
                expectBlankKeyBadRequestResponse("/%20/ranking/$SAMPLE_RANKING_MEMBER/rank")
            }
        }

        @Test
        fun `returns 400 when member name is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                pathParam(MEMBER_PATH_PARAMETER, " ")
            } When {
                get(getPath)
            } Then {
                expectBadRequestResponse(MEMBER_KEY_EXCEPTION_MESSAGE, "/$SAMPLE_KEY/ranking/%20/rank")
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.getRankedElementPosition(SAMPLE_KEY, SAMPLE_RANKING_MEMBER))
                .thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                pathParam(MEMBER_PATH_PARAMETER, SAMPLE_RANKING_MEMBER)
            } When {
                get(getPath)
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY/ranking/$SAMPLE_RANKING_MEMBER/rank")
            }
        }
    }

    @Nested
    @DisplayName("[GET] /{key}/ranking/range")
    inner class GetRankingRangeOperations {

        @ParameterizedTest
        @CsvSource(
            value = [
                "non_numeric, 1, Invalid value 'non_numeric' for parameter 'start'. Expected type: Integer",
                "1, non_numeric, Invalid value 'non_numeric' for parameter 'stop'. Expected type: Integer",
                "null, 1, Validation failed: start cannot be null",
                "3, null, Validation failed: stop cannot be null",
                "0, 10000, Range too largo. Maximum 100 elements allowed"
            ], nullValues = ["null"]
        )
        fun `returns 400 when start and (or) stop are invalid`(start: String?, stop: String?, message: String) {
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                queryParam("start", start)
                queryParam("stop", stop)
            } When {
                get("$RANKING_BASE_PATH/range")
            } Then {
                expectBadRequestResponse(
                    message = message,
                    path = "/$SAMPLE_KEY/ranking/range"
                )
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.getRankedElementRange(SAMPLE_KEY, 0, 0))
                .thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
                queryParam("start", 0)
                queryParam("stop", 0)
            } When {
                get("$RANKING_BASE_PATH/range")
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY/ranking/range")
            }
        }
    }

    @Nested
    @DisplayName("[GET] /{key}/ranking/count")
    inner class GetRankingCountOperations {
        @Test
        fun `returns 400 when key is blank`() {
            Given {
                pathParam(KEY_PATH_PARAMETER, " ")
            } When {
                get("$RANKING_BASE_PATH/count")
            } Then {
                expectBlankKeyBadRequestResponse("/%20/ranking/count")
            }
        }

        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.getRankedElementCount(SAMPLE_KEY)).thenThrow(ANY_CACHE_EXCEPTION)
            Given {
                pathParam(KEY_PATH_PARAMETER, SAMPLE_KEY)
            } When {
                get("$RANKING_BASE_PATH/count")
            } Then {
                expectRepositoryReturnedCacheException("/$SAMPLE_KEY/ranking/count")
            }
        }
    }

    @Nested
    @DisplayName("[GET] /keys/count")
    inner class GetKeyCountOperations {
        @Test
        fun `returns 500 when server throws an unexpected error`() {
            whenever(cacheRepository.getCacheKeyCount()).thenThrow(ANY_CACHE_EXCEPTION)
            When {
                get(KEY_COUNT_PATH)
            } Then {
                expectRepositoryReturnedCacheException(KEY_COUNT_PATH)
            }
        }
    }

    private fun ValidatableResponse.expectBlankKeyBadRequestResponse(
        path: String
    ): ValidatableResponse = this.expectBadRequestResponse(message = BLANK_KEY_EXCEPTION_MESSAGE, path = path)

    private fun ValidatableResponse.expectRepositoryReturnedCacheException(
        expectedPath: String
    ): ValidatableResponse = this.expectInternalServerErrorResponse(
        code = CACHE_ERROR_CODE,
        message = ANY_CACHE_EXCEPTION.message ?: "",
        path = expectedPath
    )
}
