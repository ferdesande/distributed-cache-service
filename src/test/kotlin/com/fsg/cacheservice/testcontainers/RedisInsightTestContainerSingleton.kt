package com.fsg.cacheservice.testcontainers

import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_INSIGHT_EXPOSED_PORT
import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_INSIGHT_FIXED_EXPOSED_PORT
import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_INSIGHT_IMAGE
import com.github.dockerjava.api.model.PortBinding
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object RedisInsightTestContainerSingleton {

    private val container: GenericContainer<*> = GenericContainer(DockerImageName.parse(REDIS_INSIGHT_IMAGE))
        .withExposedPorts(REDIS_INSIGHT_EXPOSED_PORT)
        .withReuse(true)
        .apply {
            withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig?.withPortBindings(
                    PortBinding.parse("$REDIS_INSIGHT_FIXED_EXPOSED_PORT:$REDIS_INSIGHT_EXPOSED_PORT")
                )
            }
        }

    fun start() {
        // Redis must be running beforehand
        RedisTestContainerSingleton.start()

        if (!container.isRunning) {
            container.start()
        }
    }

    val mappedPort: Int
        get() = REDIS_INSIGHT_FIXED_EXPOSED_PORT

    val url: String
        get() = "http://localhost:$mappedPort"
}
