package com.kss.proj.springmvccoroutines.validator

import com.kss.proj.springmvccoroutines.dto.*
import org.springframework.stereotype.Component

/**
 * Validator for Claims objects
 */
@Component
class ClaimsValidator : Validator<RawClaims, ValidatedClaims>(
    rules = listOf(
        notBlank("tenantId") { it.tenantId },
        notBlank("role") { it.role },
    ),
    transform = { raw ->
        if (raw.tenantId != null && raw.role != null) {
            ValidatedClaims(raw.tenantId, raw.role)
        } else {
            null
        }
    }
)

/**
 * Validator for Refresh Tokens
 */
@Component
class RefreshTokenValidator : Validator<RawRefreshToken, ValidatedRefreshToken>(
    rules = listOf(
        notBlank("sub") { it.sub },
        notBlank("iss") { it.iss },
        notNull("exp") { it.exp },
        notBlank("jti") { it.jti },
        matchesValue("iss", "https://auth.myapp.com") { it.iss }
    ),
    transform = { raw ->
        if (raw.sub != null && raw.iss != null && raw.exp != null && raw.jti != null) {
            ValidatedRefreshToken(raw.sub, raw.iss, raw.exp, raw.jti)
        } else {
            null
        }
    }
)

/**
 * Validator for Access Tokens with nested Claims validation
 */
@Component
class AccessTokenValidator(
    private val claimsValidator: ClaimsValidator
) : Validator<RawAccessToken, ValidatedAccessToken>(
    rules = listOf(
        notBlank("sub") { it.sub },
        notBlank("iss") { it.iss },
        notNull("exp") { it.exp },
        matchesValue("iss", "https://auth.myapp.com") { it.iss },
        nested("claims", { it.claims }, claimsValidator)
    ),
    transform = { raw ->
        if (raw.sub != null && raw.iss != null && raw.exp != null && raw.claims != null) {
            when (val claimsResult = claimsValidator.validate(raw.claims)) {
                is ValidationResult.Valid ->
                    ValidatedAccessToken(raw.sub, raw.iss, raw.exp, claimsResult.value)
                is ValidationResult.Failure -> null
            }
        } else {
            null
        }
    }
)

