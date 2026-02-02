package com.kss.proj.springmvccoroutines.validator

import com.kss.proj.springmvccoroutines.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ClaimsValidator Tests")
class ClaimsValidatorTest {

    private val validator = ClaimsValidator()

    @Nested
    @DisplayName("Valid Claims")
    inner class ValidClaims {

        @Test
        @DisplayName("should validate claims with all required fields")
        fun testValidClaims() {
            // Given
            val raw = RawClaims(tenantId = "tenant-123", role = "admin")

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Valid)
            val valid = result as ValidationResult.Valid
            assertEquals("tenant-123", valid.value.tenantId)
            assertEquals("admin", valid.value.role)
        }

        @Test
        @DisplayName("should handle various role values")
        fun testDifferentRoles() {
            // Given
            val roles = listOf("user", "admin", "superadmin", "guest")

            roles.forEach { role ->
                // When
                val result = validator.validate(RawClaims(tenantId = "tenant-1", role = role))

                // Then
                assertTrue(result is ValidationResult.Valid, "Role '$role' should be valid")
            }
        }
    }

    @Nested
    @DisplayName("Invalid Claims")
    inner class InvalidClaims {

        @Test
        @DisplayName("should fail when tenantId is null")
        fun testNullTenantId() {
            // Given
            val raw = RawClaims(tenantId = null, role = "admin")

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "tenantId" && it.code == "REQUIRED" })
        }

        @Test
        @DisplayName("should fail when role is null")
        fun testNullRole() {
            // Given
            val raw = RawClaims(tenantId = "tenant-123", role = null)

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "role" && it.code == "REQUIRED" })
        }

        @Test
        @DisplayName("should fail when tenantId is blank")
        fun testBlankTenantId() {
            // Given
            val raw = RawClaims(tenantId = "  ", role = "admin")

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "tenantId" && it.code == "BLANK" })
        }

        @Test
        @DisplayName("should fail with multiple errors when all fields are invalid")
        fun testMultipleErrors() {
            // Given
            val raw = RawClaims(tenantId = null, role = "")

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertEquals(2, failure.errors.size)
            assertTrue(failure.errors.any { it.field == "tenantId" })
            assertTrue(failure.errors.any { it.field == "role" })
        }
    }
}

@DisplayName("RefreshTokenValidator Tests")
class RefreshTokenValidatorTest {

    private val validator = RefreshTokenValidator()

    @Nested
    @DisplayName("Valid Refresh Tokens")
    inner class ValidTokens {

        @Test
        @DisplayName("should validate refresh token with all required fields")
        fun testValidToken() {
            // Given
            val raw = RawRefreshToken(
                sub = "user-123",
                iss = "https://auth.myapp.com",
                exp = 1234567890L,
                jti = "token-id-123"
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Valid)
            val valid = result as ValidationResult.Valid
            assertEquals("user-123", valid.value.sub)
            assertEquals("https://auth.myapp.com", valid.value.iss)
            assertEquals(1234567890L, valid.value.exp)
            assertEquals("token-id-123", valid.value.jti)
        }
    }

    @Nested
    @DisplayName("Invalid Refresh Tokens")
    inner class InvalidTokens {

        @Test
        @DisplayName("should fail when sub is missing")
        fun testMissingSub() {
            // Given
            val raw = RawRefreshToken(
                sub = null,
                iss = "https://auth.myapp.com",
                exp = 1234567890L,
                jti = "token-id-123"
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "sub" && it.code == "REQUIRED" })
        }

        @Test
        @DisplayName("should fail when iss is wrong value")
        fun testWrongIssuer() {
            // Given
            val raw = RawRefreshToken(
                sub = "user-123",
                iss = "https://wrong.issuer.com",
                exp = 1234567890L,
                jti = "token-id-123"
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "iss" && it.code == "INVALID_VALUE" })
            assertTrue(failure.errors.any {
                it.message.contains("https://auth.myapp.com") &&
                it.message.contains("https://wrong.issuer.com")
            })
        }

        @Test
        @DisplayName("should fail when exp is null")
        fun testNullExp() {
            // Given
            val raw = RawRefreshToken(
                sub = "user-123",
                iss = "https://auth.myapp.com",
                exp = null,
                jti = "token-id-123"
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "exp" && it.code == "REQUIRED" })
        }

        @Test
        @DisplayName("should fail when jti is blank")
        fun testBlankJti() {
            // Given
            val raw = RawRefreshToken(
                sub = "user-123",
                iss = "https://auth.myapp.com",
                exp = 1234567890L,
                jti = ""
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "jti" && it.code == "BLANK" })
        }

        @Test
        @DisplayName("should collect all validation errors")
        fun testMultipleErrors() {
            // Given
            val raw = RawRefreshToken(
                sub = null,
                iss = "wrong-issuer",
                exp = null,
                jti = ""
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.size >= 3, "Should have at least 3 errors")
            assertTrue(failure.errors.any { it.field == "sub" })
            assertTrue(failure.errors.any { it.field == "iss" })
            assertTrue(failure.errors.any { it.field == "exp" })
            assertTrue(failure.errors.any { it.field == "jti" })
        }
    }
}

@DisplayName("AccessTokenValidator Tests")
class AccessTokenValidatorTest {

    private val claimsValidator = ClaimsValidator()
    private val validator = AccessTokenValidator(claimsValidator)

    @Nested
    @DisplayName("Valid Access Tokens")
    inner class ValidTokens {

        @Test
        @DisplayName("should validate access token with valid claims")
        fun testValidToken() {
            // Given
            val raw = RawAccessToken(
                sub = "user-123",
                iss = "https://auth.myapp.com",
                exp = 1234567890L,
                claims = RawClaims(tenantId = "tenant-123", role = "admin")
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Valid)
            val valid = result as ValidationResult.Valid
            assertEquals("user-123", valid.value.sub)
            assertEquals("https://auth.myapp.com", valid.value.iss)
            assertEquals(1234567890L, valid.value.exp)
            assertEquals("tenant-123", valid.value.claims.tenantId)
            assertEquals("admin", valid.value.claims.role)
        }
    }

    @Nested
    @DisplayName("Invalid Access Tokens")
    inner class InvalidTokens {

        @Test
        @DisplayName("should fail when sub is missing")
        fun testMissingSub() {
            // Given
            val raw = RawAccessToken(
                sub = null,
                iss = "https://auth.myapp.com",
                exp = 1234567890L,
                claims = RawClaims(tenantId = "tenant-123", role = "admin")
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "sub" && it.code == "REQUIRED" })
        }

        @Test
        @DisplayName("should fail when iss is wrong value")
        fun testWrongIssuer() {
            // Given
            val raw = RawAccessToken(
                sub = "user-123",
                iss = "https://wrong.issuer.com",
                exp = 1234567890L,
                claims = RawClaims(tenantId = "tenant-123", role = "admin")
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "iss" && it.code == "INVALID_VALUE" })
        }

        @Test
        @DisplayName("should fail when claims are null")
        fun testNullClaims() {
            // Given
            val raw = RawAccessToken(
                sub = "user-123",
                iss = "https://auth.myapp.com",
                exp = 1234567890L,
                claims = null
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "claims" && it.code == "REQUIRED" })
        }

        @Test
        @DisplayName("should propagate nested validation errors with field path")
        fun testNestedClaimsErrors() {
            // Given
            val raw = RawAccessToken(
                sub = "user-123",
                iss = "https://auth.myapp.com",
                exp = 1234567890L,
                claims = RawClaims(tenantId = null, role = "")
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.any { it.field == "claims.tenantId" })
            assertTrue(failure.errors.any { it.field == "claims.role" })
        }

        @Test
        @DisplayName("should collect errors from both token and nested claims")
        fun testMultipleLevelErrors() {
            // Given
            val raw = RawAccessToken(
                sub = "",
                iss = "wrong-issuer",
                exp = null,
                claims = RawClaims(tenantId = null, role = null)
            )

            // When
            val result = validator.validate(raw)

            // Then
            assertTrue(result is ValidationResult.Failure)
            val failure = result as ValidationResult.Failure
            assertTrue(failure.errors.size >= 4, "Should have errors from both levels")

            // Token-level errors
            assertTrue(failure.errors.any { it.field == "sub" })
            assertTrue(failure.errors.any { it.field == "iss" })
            assertTrue(failure.errors.any { it.field == "exp" })

            // Nested claims errors
            assertTrue(failure.errors.any { it.field == "claims.tenantId" })
            assertTrue(failure.errors.any { it.field == "claims.role" })
        }
    }
}

