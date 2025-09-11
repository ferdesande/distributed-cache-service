package com.fsg.cacheservice.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.data.redis")
data class RedisProperties(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String = ""
)
