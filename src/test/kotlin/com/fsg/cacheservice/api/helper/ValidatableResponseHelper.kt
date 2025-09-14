package com.fsg.cacheservice.api.helper

import io.restassured.response.ValidatableResponse
import org.apache.http.HttpStatus
import org.hamcrest.Matchers.equalTo
import org.springframework.http.MediaType

internal object ValidatableResponseHelper {
    private const val BAD_REQUEST_ERROR_CODE = "BAD_REQUEST"
    private const val INTERNAL_SERVER_ERROR_ERROR_CODE = "INTERNAL_SERVER_ERROR"
    private const val TIMESTAMP_STRING = "2020-01-01T00:00:00Z"

    fun ValidatableResponse.expectErrorResponse(
        status: Int,
        code: String,
        message: String,
        path: String,
    ): ValidatableResponse {
        return this
            .statusCode(status)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("error.code", equalTo(code))
            .body("error.message", equalTo(message))
            .body("error.path", equalTo(path))
            .body("error.timestamp", equalTo(TIMESTAMP_STRING))
    }

    fun ValidatableResponse.expectBadRequestResponse(
        message: String,
        path: String,
        code: String = BAD_REQUEST_ERROR_CODE,
    ): ValidatableResponse {
        return this.expectErrorResponse(
            status = HttpStatus.SC_BAD_REQUEST,
            code = code,
            message = message,
            path = path
        )
    }

    fun ValidatableResponse.expectInternalServerErrorResponse(
        message: String,
        path: String,
        code: String = INTERNAL_SERVER_ERROR_ERROR_CODE,
    ): ValidatableResponse {
        return this.expectErrorResponse(
            status = HttpStatus.SC_INTERNAL_SERVER_ERROR,
            code = code,
            message = message,
            path = path
        )
    }
}
