package com.fsg.cacheservice.testcontainers

import org.springframework.test.context.TestExecutionListeners

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@TestExecutionListeners(
    value = [TestContainerExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
/**
 * For more info
 * @see TestContainerExecutionListener
 */
annotation class WithTestContainers(
    val redis: Boolean = false,
    val redisInsight: Boolean = false
)
