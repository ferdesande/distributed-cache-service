package com.fsg.cacheservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CacheServiceApplication

fun main(args: Array<String>) {
    runApplication<CacheServiceApplication>(*args)
}
