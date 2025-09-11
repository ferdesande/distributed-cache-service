package com.fsg.cacheservice.testcontainers

object TestContainersConfiguration {
    const val REDIS_IMAGE = "redis:8-alpine"
    const val REDIS_EXPOSED_PORT = 6379
    const val REDIS_INSIGHT_IMAGE = "redislabs/redisinsight:latest"
    const val REDIS_INSIGHT_EXPOSED_PORT = 5540
    const val REDIS_INSIGHT_FIXED_EXPOSED_PORT = 5541
}
