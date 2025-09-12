package com.fsg.cacheservice.api.exception

import com.fsg.cacheservice.api.dto.ErrorDetailsDto
import com.fsg.cacheservice.api.dto.ErrorResponseDto
import com.fsg.cacheservice.core.ValueGenerator
import com.fsg.cacheservice.core.exception.BadRequestException
import com.fsg.cacheservice.core.exception.CacheException
import com.fsg.cacheservice.core.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

/**
 * Global exception handler for REST API endpoints.
 * Converts domain exceptions into appropriate HTTP responses with standardized error format.
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    private val valueGenerator: ValueGenerator
) {

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: NotFoundException, request: WebRequest): ErrorResponseDto {
        return createErrorResponse(
            code = "NOT_FOUND",
            message = ex.message ?: "Resource not found",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(BadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(ex: BadRequestException, request: WebRequest): ErrorResponseDto {
        return createErrorResponse(
            code = "BAD_REQUEST",
            message = ex.message ?: "Invalid request",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(CacheException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleCacheException(ex: CacheException, request: WebRequest): ErrorResponseDto {
        return createErrorResponse(
            code = "CACHE_ERROR",
            message = ex.message ?: "Problem accessing cache",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception, request: WebRequest): ErrorResponseDto {
        return createErrorResponse(
            code = "INTERNAL_ERROR",
            message = "An unexpected error occurred",
            path = getRequestPath(request)
        )
    }

    private fun createErrorResponse(code: String, message: String, path: String): ErrorResponseDto {
        return ErrorResponseDto(
            error = ErrorDetailsDto(
                code = code,
                message = message,
                timestamp = valueGenerator.now(),
                path = path
            )
        )
    }

    private fun getRequestPath(request: WebRequest): String {
        return request.getDescription(false).removePrefix("uri=")
    }
}
