package com.fsg.cacheservice.testcontainers

import com.fsg.cacheservice.testutils.TestContainersConfiguration.REDIS_INSIGHT_EXPOSED_PORT
import com.fsg.cacheservice.testutils.TestContainersConfiguration.REDIS_INSIGHT_FIXED_EXPOSED_PORT
import com.fsg.cacheservice.testutils.TestContainersConfiguration.REDIS_INSIGHT_IMAGE
import com.github.dockerjava.api.model.PortBinding
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class RedisTestWithInsightBase : RedisTestBase() {

    companion object {
        @Container
        @JvmStatic
        @Suppress("UnusedPrivateProperty", "Unused")
        private val redisInsightContainer = GenericContainer(DockerImageName.parse(REDIS_INSIGHT_IMAGE))
            .withExposedPorts(5540)
            .withReuse(true)
            .apply {
                withCreateContainerCmdModifier { cmd ->
                    cmd.hostConfig?.withPortBindings(
                        PortBinding.parse("${REDIS_INSIGHT_FIXED_EXPOSED_PORT}:${REDIS_INSIGHT_EXPOSED_PORT}")
                    )
                }
                dependsOn(redisContainer)
            }

        init {
            LoggerFactory.getLogger(RedisTestWithInsightBase::class.java)
                .info("RedisInsight will be available at: http://localhost:$REDIS_INSIGHT_FIXED_EXPOSED_PORT")
        }
    }
}
