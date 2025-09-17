package com.fsg.cacheservice.api

import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_KEY
import com.fsg.cacheservice.api.helper.DataMother.SAMPLE_VALUE
import com.fsg.cacheservice.testutils.RedisTestUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Duration

class RedisCacheControllerAcceptanceTest : CacheControllerIntegrationTest() {
    companion object {
        @Container
        @JvmStatic
        private val redisContainer: GenericContainer<*> = RedisTestUtils.createRedisContainer()

        @DynamicPropertySource
        @JvmStatic
        @Suppress("Unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            RedisTestUtils.configureRedisProperties(redisContainer, registry)
            registry.add("cache.implementation") { "redis" }
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

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @BeforeEach
    fun cleanupRedis() {
        super.setUp()
        RedisTestUtils.flushRedis(redisTemplate)
    }

    override fun setString(key: String, value: String, ttlInSeconds: Long?) {
        if (ttlInSeconds == null) {
            redisTemplate.opsForValue().set(key, value)
        } else {
            redisTemplate.opsForValue().set(SAMPLE_KEY, SAMPLE_VALUE, Duration.ofSeconds(ttlInSeconds))
        }
    }

    override fun getString(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    override fun getExpire(key: String): Long {
        return redisTemplate.getExpire(key)
    }

    override fun getRankingScore(key: String, member: String): Double? {
        return redisTemplate.opsForZSet().score(key, member)
    }

    override fun setRankingMember(key: String, member: String, score: Double): Boolean? {
        return redisTemplate.opsForZSet().add(key, member, score)
    }
}
