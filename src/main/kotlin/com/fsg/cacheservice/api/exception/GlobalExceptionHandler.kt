package com.fsg.cacheservice.api.exception

import com.fsg.cacheservice.api.dto.ErrorDetailsDto
import com.fsg.cacheservice.api.dto.ErrorResponseDto
import com.fsg.cacheservice.core.ValueGenerator
import com.fsg.cacheservice.core.exception.BadRequestException
import com.fsg.cacheservice.core.exception.CacheException
import com.fsg.cacheservice.core.exception.InvalidValueException
import com.fsg.cacheservice.core.exception.NotFoundException
import com.fsg.cacheservice.core.exception.OverflowException
import com.fsg.cacheservice.core.exception.WrongTypeException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Global exception handler for REST API endpoints.
 * Converts domain exceptions into appropriate HTTP responses with standardized error format.
 */
@RestControllerAdvice
@Suppress("TooManyFunctions")
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

    @ExceptionHandler(InvalidValueException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidValueException(ex: InvalidValueException, request: WebRequest): ErrorResponseDto {
        return createErrorResponse(
            code = "BAD_REQUEST",
            message = ex.message ?: "Invalid request",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(WrongTypeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleWrongTypeException(ex: WrongTypeException, request: WebRequest): ErrorResponseDto {
        return createErrorResponse(
            code = "BAD_REQUEST",
            message = ex.message ?: "Invalid request",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(OverflowException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleOverflowException(ex: OverflowException, request: WebRequest): ErrorResponseDto {
        return createErrorResponse(
            code = "INTERNAL_SERVER_ERROR",
            message = ex.message ?: "Problem accessing cache",
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

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidJson(
        @Suppress("UnusedParameter", "Unused") ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ErrorResponseDto {
        return createErrorResponse(
            code = "BAD_REQUEST",
            message = "Body has an invalid JSON format",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationErrors(ex: ConstraintViolationException, request: WebRequest): ErrorResponseDto {
        val errors = ex.constraintViolations.joinToString(", ") { "${it.message}" }
        return createErrorResponse(
            code = "BAD_REQUEST",
            message = "Validation failed: $errors",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ErrorResponseDto {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { it.defaultMessage.toString() }
        return createErrorResponse(
            code = "BAD_REQUEST",
            message = "Validation failed: $errors",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ErrorResponseDto {
        val parameterName = ex.name
        val requiredType = ex.requiredType?.simpleName ?: "unknown"
        val providedValue = ex.value

        return createErrorResponse(
            code = "BAD_REQUEST",
            message = "Invalid value '$providedValue' for parameter '$parameterName'. Expected type: $requiredType",
            path = getRequestPath(request)
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(
        @Suppress("UnusedParameter", "Unused") ex: Exception,
        request: WebRequest
    ): ErrorResponseDto {
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
