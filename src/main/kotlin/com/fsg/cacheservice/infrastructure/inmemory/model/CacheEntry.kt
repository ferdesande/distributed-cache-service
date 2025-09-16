package com.fsg.cacheservice.infrastructure.inmemory.model

import java.time.Instant

internal data class CacheEntry<T>(
    val value: T,
    val expiresAt: Instant? = null
)
