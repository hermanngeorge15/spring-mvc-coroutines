package com.kss.proj.springmvccoroutines.example

import com.kss.proj.springmvccoroutines.dto.*
import com.kss.proj.springmvccoroutines.validator.*
import org.springframework.stereotype.Service

/**
 * Example service showing how to use the validators
 */
@Service
class TokenService(
    private val accessTokenValidator: AccessTokenValidator,
    private val refreshTokenValidator: RefreshTokenValidator
) {

    /**
     * Validates and processes an access token
     * Returns the validated token or throws an exception with validation errors
     */
    fun processAccessToken(raw: RawAccessToken): ValidatedAccessToken {
        return when (val result = accessTokenValidator.validate(raw)) {
            is ValidationResult.Valid -> {
                // Token is valid, proceed with business logic
                result.value
            }
            is ValidationResult.Failure -> {
                // Token is invalid, handle errors
                throw ValidationException("Access token validation failed", result.errors)
            }
        }
    }

    /**
     * Validates and processes a refresh token
     * Returns the validated token or null if invalid
     */
    fun processRefreshToken(raw: RawRefreshToken): ValidatedRefreshToken? {
        return when (val result = refreshTokenValidator.validate(raw)) {
            is ValidationResult.Valid -> result.value
            is ValidationResult.Failure -> {
                // Log errors and return null
                println("Refresh token validation failed: ${result.errors}")
                null
            }
        }
    }

    /**
     * Validates a token and returns detailed error information
     */
    fun validateWithDetails(raw: RawAccessToken): TokenValidationResponse {
        return when (val result = accessTokenValidator.validate(raw)) {
            is ValidationResult.Valid -> {
                TokenValidationResponse(
                    isValid = true,
                    token = result.value,
                    errors = emptyList()
                )
            }
            is ValidationResult.Failure -> {
                TokenValidationResponse(
                    isValid = false,
                    token = null,
                    errors = result.errors
                )
            }
        }
    }

    /**
     * Batch validation of multiple tokens
     */
    fun validateBatch(tokens: List<RawAccessToken>): BatchValidationResult {
        val results = tokens.map { raw ->
            when (val result = accessTokenValidator.validate(raw)) {
                is ValidationResult.Valid -> BatchTokenResult(raw, result.value, emptyList())
                is ValidationResult.Failure -> BatchTokenResult(raw, null, result.errors)
            }
        }

        return BatchValidationResult(
            total = results.size,
            valid = results.count { it.validated != null },
            invalid = results.count { it.validated == null },
            results = results
        )
    }
}

// Response DTOs
data class TokenValidationResponse(
    val isValid: Boolean,
    val token: ValidatedAccessToken?,
    val errors: List<ValidationError>
)

data class BatchTokenResult(
    val raw: RawAccessToken,
    val validated: ValidatedAccessToken?,
    val errors: List<ValidationError>
)

data class BatchValidationResult(
    val total: Int,
    val valid: Int,
    val invalid: Int,
    val results: List<BatchTokenResult>
)

class ValidationException(
    message: String,
    val errors: List<ValidationError>
) : RuntimeException(message) {
    override fun toString(): String {
        return "$message: ${errors.joinToString(", ") { "${it.field}: ${it.message}" }}"
    }
}

