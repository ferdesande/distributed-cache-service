package com.fsg.cacheservice.infrastructure.inmemory

import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.CacheRepositoryTestAcceptanceTest
import com.fsg.cacheservice.core.ValueGenerator

class InMemoryCacheRepositoryAcceptanceTest : CacheRepositoryTestAcceptanceTest() {
    override fun setCacheRepository(): CacheRepository = InMemoryCacheRepository(ValueGenerator())
}
