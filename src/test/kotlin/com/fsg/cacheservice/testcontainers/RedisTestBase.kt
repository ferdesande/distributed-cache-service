package com.fsg.cacheservice.testcontainers

import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_EXPOSED_PORT
import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_IMAGE
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
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
        protected val redisContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_EXPOSED_PORT)
            .withReuse(true)

        @DynamicPropertySource
        @JvmStatic
        @Suppress("Unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // HINT: It could happen that test classes using this as base class present flaky tests locally.
            //       Before moving this configuration to every subclass. Restart docker locally first.
            //       The key to fix the flaky tests is to start and stop the container in every test class.
            val port = redisContainer.firstMappedPort
            registry.add("spring.data.redis.host", redisContainer::getHost)
            registry.add("spring.data.redis.port") { port }
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
