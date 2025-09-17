package com.fsg.cacheservice.configuration

import com.fsg.cacheservice.core.CacheRepository
import com.fsg.cacheservice.core.ValueGenerator
import com.fsg.cacheservice.infrastructure.inmemory.InMemoryCacheRepository
import com.fsg.cacheservice.infrastructure.redis.RedisCacheRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class DependencyInjectionConfiguration {
    @Bean
    fun valueGenerator(): ValueGenerator = ValueGenerator()

    @Bean
    @ConditionalOnProperty(
        name = ["cache.implementation"],
        havingValue = "redis"
    )
    fun redisCacheRepository(redisTemplate: RedisTemplate<String, String>): CacheRepository {
        return RedisCacheRepository(redisTemplate)
    }

    @Bean
    @ConditionalOnProperty(
        name = ["cache.implementation"],
        havingValue = "inmemory",
        matchIfMissing = true // Default to in-memory if property not set
    )
    fun inMemoryCacheRepository(valueGenerator: ValueGenerator): CacheRepository {
        return InMemoryCacheRepository(valueGenerator)
    }
}
