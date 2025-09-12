package com.fsg.cacheservice.api.validation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
// TODO: AOP aspect not implemented yet - validation is currently non-functional
annotation class ValidKey
