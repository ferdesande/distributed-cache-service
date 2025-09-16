package com.fsg.cacheservice.testutils

import com.fsg.cacheservice.testutils.TestContainersConfiguration.REDIS_EXPOSED_PORT
import com.fsg.cacheservice.testutils.TestContainersConfiguration.REDIS_IMAGE
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object RedisTestUtils {
    fun createRedisContainer(): GenericContainer<*> =
        GenericContainer(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_EXPOSED_PORT)
            .withReuse(true)

    fun configureRedisProperties(
        container: GenericContainer<*>,
        registry: DynamicPropertyRegistry
    ) {
        val port = container.firstMappedPort
        registry.add("spring.data.redis.host", container::getHost)
        registry.add("spring.data.redis.port") { port }
    }

    fun flushRedis(redisTemplate: RedisTemplate<String, String>) {
        redisTemplate.execute { connection ->
            connection.serverCommands().flushDb()
            null
        }
    }
}
