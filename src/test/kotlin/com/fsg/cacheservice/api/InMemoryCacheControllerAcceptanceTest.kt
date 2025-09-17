package com.fsg.cacheservice.api

import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.infrastructure.inmemory.InMemoryCacheRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration

class InMemoryCacheControllerAcceptanceTest : CacheControllerIntegrationTest() {
    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("Unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("cache.implementation") { "inmemory" }
        }
    }

    @Autowired
    private lateinit var cacheRepository: CacheRepository

    private lateinit var inMemoryCache: InMemoryCacheRepository

    @BeforeEach
    fun cleanupRedis() {
        super.setUp()
        inMemoryCache = cacheRepository as InMemoryCacheRepository
        inMemoryCache.clear()
    }

    override fun setString(key: String, value: String, ttlInSeconds: Long?) {
        if (ttlInSeconds == null) {
            cacheRepository.set(key, value)
        } else {
            cacheRepository.set(key, value, Duration.ofSeconds(ttlInSeconds))
        }
    }

    override fun getString(key: String): String? {
        return cacheRepository.get(key)
    }

    override fun getExpire(key: String): Long {
        return inMemoryCache.getExpire(key)
    }

    override fun getRankingScore(key: String, member: String): Double? {
        return inMemoryCache.getRankingScore(key, member)
    }

    override fun setRankingMember(key: String, member: String, score: Double): Boolean? {
        return cacheRepository.setRankedElement(key, score, member)
    }
}
