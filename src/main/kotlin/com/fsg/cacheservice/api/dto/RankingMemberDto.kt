package com.fsg.cacheservice.api.dto

import com.fsg.cacheservice.api.validation.ValidRankingMember

data class RankingMemberDto(
    @field:ValidRankingMember(message = "Field member in RankingMemberDto cannot be blank") val member: String,
    val score: Double
)
