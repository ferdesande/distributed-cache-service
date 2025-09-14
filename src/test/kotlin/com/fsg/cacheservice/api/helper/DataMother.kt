package com.fsg.cacheservice.api.helper

import com.fsg.cacheservice.api.dto.RankingMemberDto
import com.fsg.cacheservice.core.exception.CacheException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal object DataMother {
    const val SAMPLE_KEY = "a-sample-key"
    const val SAMPLE_VALUE = "a sample value"
    const val LOW_SCORE = 1500.0
    const val MEDIUM_SCORE = 2000.0
    const val HIGH_SCORE = 2500.0
    const val SAMPLE_RANKING_MEMBER = "player1"
    const val ANOTHER_RANKING_MEMBER = "player2"

    val SAMPLE_RANKING_MEMBER_DTO = RankingMemberDto(SAMPLE_RANKING_MEMBER, LOW_SCORE)
    val TIMESTAMP: Instant = LocalDateTime.of(2020, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)
    val ANY_CACHE_EXCEPTION = CacheException("Any fancy message")
}
