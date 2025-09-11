package com.fsg.cacheservice.testcontainers

import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_EXPOSED_PORT
import com.fsg.cacheservice.testcontainers.TestContainersConfiguration.REDIS_IMAGE
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object RedisTestContainerSingleton {

    private val container: GenericContainer<*> = GenericContainer(DockerImageName.parse(REDIS_IMAGE))
        .withExposedPorts(REDIS_EXPOSED_PORT)
        .withReuse(true)

    fun start() {
        if (!container.isRunning) {
            container.start()
        }
    }

    val host: String
        get() = container.host

    val port: Int
        get() = container.getMappedPort(REDIS_EXPOSED_PORT)
}
