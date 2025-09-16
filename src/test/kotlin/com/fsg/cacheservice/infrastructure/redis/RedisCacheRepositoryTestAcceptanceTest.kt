package com.fsg.cacheservice.infrastructure.redis

import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.CacheRepositoryTestAcceptanceTest
import com.fsg.cacheservice.testutils.RedisTestUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container

@SpringBootTest
class RedisCacheRepositoryTestAcceptanceTest : CacheRepositoryTestAcceptanceTest() {
    companion object {
        @Container
        @JvmStatic
        private val redisContainer: GenericContainer<*> = RedisTestUtils.createRedisContainer()

        @DynamicPropertySource
        @JvmStatic
        @Suppress("Unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            RedisTestUtils.configureRedisProperties(redisContainer, registry)
        }

        @JvmStatic
        @BeforeAll
        fun initialize() {
            redisContainer.start()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            redisContainer.stop()
        }
    }

    @BeforeEach
    fun cleanupRedis() {
        RedisTestUtils.flushRedis(redisTemplate)
    }

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var repository: RedisCacheRepository

    override fun setCacheRepository(): CacheRepository = repository
}
