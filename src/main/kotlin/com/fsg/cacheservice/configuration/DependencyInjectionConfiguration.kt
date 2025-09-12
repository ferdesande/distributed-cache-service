package com.fsg.cacheservice.configuration

import com.fsg.cacheservice.core.ValueGenerator
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class DependencyInjectionConfiguration {
    @Bean
    fun valueGenerator(): ValueGenerator = ValueGenerator()
}
