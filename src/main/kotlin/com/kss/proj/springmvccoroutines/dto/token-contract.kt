package com.kss.proj.springmvccoroutines.dto

data class RawRefreshToken(
    val sub: String?,
    val iss: String?,
    val exp: Long?,
    val jti: String?
)

data class ValidatedRefreshToken(
    val sub: String,
    val iss: String,
    val exp: Long,
    val jti: String
)

// === NESTED TOKEN ===

data class RawAccessToken(
    val sub: String?,
    val iss: String?,
    val exp: Long?,
    val claims: RawClaims?
)

data class RawClaims(
    val tenantId: String?,
    val role: String?
)

data class ValidatedAccessToken(
    val sub: String,
    val iss: String,
    val exp: Long,
    val claims: ValidatedClaims
)

data class ValidatedClaims(
    val tenantId: String,
    val role: String
)

data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)