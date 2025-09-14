package com.fsg.cacheservice.api.dto

import java.time.Instant

data class ErrorDetailsDto(
    val code: String,
    val message: String,
    val timestamp: Instant,
    val path: String
)
