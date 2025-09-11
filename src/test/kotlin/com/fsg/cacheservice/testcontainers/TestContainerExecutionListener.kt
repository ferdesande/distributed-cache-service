package com.fsg.cacheservice.testcontainers

import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

class TestContainerExecutionListener : TestExecutionListener {
    /**
     * HINT: Orchestrates multiple test containers based on @TestContainers annotation parameters.
     *
     * This listener provides more lifecycle control compared to standard annotations like @BeforeAll.
     * Configure containers via @TestContainers(redis = true, redisInsight = true, etc.)
     *
     * @see WithTestContainers
     */
    override fun beforeTestClass(testContext: TestContext) {
        val annotation = testContext.testClass.getAnnotation(WithTestContainers::class.java) ?: return

        if (annotation.redis) {
            RedisTestContainerSingleton.start()
            System.setProperty("spring.data.redis.host", RedisTestContainerSingleton.host)
            System.setProperty("spring.data.redis.port", RedisTestContainerSingleton.port.toString())
        }

        if (annotation.redisInsight) {
            RedisInsightTestContainerSingleton.start()
            println("RedisInsight: ${RedisInsightTestContainerSingleton.url}")
        }
    }
}
