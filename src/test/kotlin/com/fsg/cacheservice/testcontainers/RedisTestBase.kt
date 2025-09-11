package com.fsg.cacheservice.testcontainers

import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_EXPOSED_PORT
import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_IMAGE
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@Suppress("UtilityClassWithPublicConstructor")
abstract class RedisTestBase {

    companion object {
        @Container
        @JvmStatic
        protected val redisContainer = GenericContainer(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_EXPOSED_PORT)
            .withReuse(true)

        @DynamicPropertySource
        @JvmStatic
        @Suppress("Unused")
        fun configureRedisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host", redisContainer::getHost)
            registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort)
        }
    }
}
