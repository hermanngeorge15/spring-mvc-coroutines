package com.kss.proj.springmvccoroutines.example

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Demonstrates masking via Zalando Logbook (HTTP request/response interceptor).
 *
 * How it works:
 *   - Logbook intercepts HTTP requests and responses BEFORE they reach the controller.
 *   - It logs the full request/response as structured JSON at TRACE level.
 *   - Sensitive fields in headers and JSON bodies are masked automatically based on:
 *     1. application-logbook.yml obfuscate config (headers, params, json-body-fields)
 *     2. LogbookConfiguration bean with custom JsonPathBodyFilters (partial masking)
 *
 * Run with: ./gradlew bootRun --args='--spring.profiles.active=logbook'
 *
 * Masking behavior (in Logbook's TRACE logs):
 *   - Headers: Authorization, X-API-Key, Cookie -> "XXX"
 *   - Body fields: password, token, ssn, cvv -> "XXX" (config) or "***REDACTED***" (bean)
 *   - Body fields: email -> "***@domain.com", phone -> "***1234" (custom BodyFilter bean)
 *   - Body fields: creditCardNumber -> "4111-****-****-1111" (custom BodyFilter bean)
 *   - Query params: password, token -> "XXX"
 *
 * NOTE: The controller itself does NOT do any masking. All masking happens
 * in the Logbook pipeline before the log is written.
 */
@RestController
@RequestMapping("/api/logbook")
class LogbookDemoController {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Login - Logbook masks "password" and "email" in the request body,
     * and "token" in the response body.
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        log.info("Login attempt for user: {}", request.username)

        return ResponseEntity.ok(
            LoginResponse(
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U",
                refreshToken = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4",
                expiresIn = 3600
            )
        )
    }

    /**
     * Payment - Logbook masks "creditCardNumber", "cvv", and "email" in the request body.
     */
    @PostMapping("/payment")
    fun processPayment(@RequestBody request: PaymentRequest): ResponseEntity<PaymentResponse> {
        log.info("Processing payment of {} {}", request.amount, request.currency)

        return ResponseEntity.ok(
            PaymentResponse(
                transactionId = "TXN-${System.currentTimeMillis()}",
                status = "APPROVED",
                maskedCard = "${request.creditCardNumber.take(4)}****${request.creditCardNumber.takeLast(4)}"
            )
        )
    }

    /**
     * Profile - Logbook masks "ssn", "apiKey", "email", and "phone" in the request body.
     */
    @PostMapping("/profile")
    fun updateProfile(@RequestBody request: UserProfileRequest): ResponseEntity<UserProfileResponse> {
        log.info("Updating profile for: {}", request.name)

        return ResponseEntity.ok(
            UserProfileResponse(id = "USR-001", name = request.name, status = "UPDATED")
        )
    }

    /**
     * Custom tokens - Logbook masks "myCustomeToken1" and "mycustomtoken2" in the request body.
     */
    @PostMapping("/custom-tokens")
    fun customTokens(@RequestBody request: CustomTokenRequest): ResponseEntity<CustomTokenResponse> {
        log.info("Custom tokens request for service: {}", request.service)

        return ResponseEntity.ok(
            CustomTokenResponse(status = "PROCESSED", service = request.service)
        )
    }

    /**
     * Protected - Logbook masks Authorization and X-API-Key headers.
     */
    @GetMapping("/protected")
    fun protectedEndpoint(
        @RequestHeader("Authorization", required = false) authHeader: String?
    ): ResponseEntity<Map<String, String>> {
        log.info("Accessing protected resource")

        return ResponseEntity.ok(mapOf("message" to "Access granted", "resource" to "secret-data"))
    }
}
