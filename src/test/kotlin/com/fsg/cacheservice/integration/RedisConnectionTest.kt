package com.fsg.cacheservice.integration

import com.fsg.cacheservice.testcontainers.WithTestContainers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
@WithTestContainers(redis = true)
class RedisConnectionTest {

    // HINT: Tests to check that Redis is correctly configured to work with test containers

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Test
    fun `should connect to Redis and perform basic operations`() {
        val key = "test-key"
        val value = "test-value"

        // Test SET
        redisTemplate.opsForValue().set(key, value)

        // Test GET
        val retrievedValue = redisTemplate.opsForValue().get(key)
        assertThat(retrievedValue, equalTo(value))

        // Test DELETE
        val deleted = redisTemplate.delete(key)
        assertThat(deleted, equalTo(true))

        // Verify key is gone
        val afterDelete = redisTemplate.opsForValue().get(key)
        assertThat(afterDelete, nullValue())
    }

    @Test
    fun `should verify Redis connection factory is available`() {
        assertThat(redisTemplate.connectionFactory, notNullValue())
        assertThat(redisTemplate.connectionFactory?.connection, notNullValue())
    }
}
