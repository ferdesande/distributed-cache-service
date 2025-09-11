package com.fsg.cacheservice.testcontainers

import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_EXPOSED_PORT
import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_IMAGE
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
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

        @JvmStatic
        protected fun configureRedisProperties(registry: DynamicPropertyRegistry) {
            // HINT: it cannot be annotated with @DynamicPropertySource because otherwise, it fails. Try to fix it.
            registry.add("spring.data.redis.host", redisContainer::getHost)
            registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort)
        }
    }

    @BeforeEach
    protected fun cleanupRedis() {
        flushRedisDatabase()
    }

    @Autowired
    protected lateinit var redisTemplate: RedisTemplate<String, String>

    private fun flushRedisDatabase() {
        redisTemplate.execute { connection ->
            connection.serverCommands().flushDb()
            null
        }
    }
}
