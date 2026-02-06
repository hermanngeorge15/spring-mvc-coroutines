package com.kss.proj.springmvccoroutines.example

import net.logstash.logback.argument.StructuredArguments.fields
import net.logstash.logback.argument.StructuredArguments.kv
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Demonstrates masking via logstash-logback-encoder + MaskingJsonGeneratorDecorator.
 *
 * Three approaches shown:
 *
 *   1. kv() per field     - each field is a separate JSON key
 *   2. fields(object)     - ALL fields of the object become top-level JSON keys
 *   3. Markers.append()   - the object is serialized under a nested JSON key
 *
 * All three go through Jackson serialization -> MaskingJsonGeneratorDecorator intercepts
 * writeFieldName() + writeString() and masks sensitive values.
 *
 * Run with: ./gradlew bootRun --args='--spring.profiles.active=json'
 */
@RestController
@RequestMapping("/api/logback")
class LogbackDemoController {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * APPROACH 1: kv() per field
     * Each sensitive value becomes a separate top-level JSON field.
     *
     * Output: { "message": "...", "username": "john", "email": "***@example.com", "password": "***REDACTED***" }
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        log.info("Login attempt: {}, {}, {}",
            kv("username", request.username),
            kv("email", request.email),
            kv("password", request.password)
        )

        val response = LoginResponse(
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U",
            refreshToken = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4",
            expiresIn = 3600
        )

        log.info("Login successful, issued {}", kv("token", response.token))
        return ResponseEntity.ok(response)
    }

    /**
     * APPROACH 2: fields(object) - log the whole request body as top-level JSON fields
     * The object is serialized via Jackson -> decorator masks each field.
     *
     * Output: { "message": "...", "amount": 99.99, "currency": "USD",
     *           "creditCardNumber": "***REDACTED***", "cvv": "***REDACTED***",
     *           "cardholderName": "John Doe", "email": "***@example.com" }
     */
    @PostMapping("/payment")
    fun processPayment(@RequestBody request: PaymentRequest): ResponseEntity<PaymentResponse> {
        log.info("Payment request body: {}", fields(request))

        return ResponseEntity.ok(
            PaymentResponse(
                transactionId = "TXN-${System.currentTimeMillis()}",
                status = "APPROVED",
                maskedCard = "${request.creditCardNumber.take(4)}****${request.creditCardNumber.takeLast(4)}"
            )
        )
    }

    /**
     * APPROACH 3: Markers.append() - log the whole body under a nested "request" key
     * The object is serialized via Jackson under a named key -> decorator masks fields.
     *
     * Output: { "message": "...", "request": { "name": "Jane",
     *           "email": "***@example.com", "phone": "***7890",
     *           "ssn": "***REDACTED***", "apiKey": "***REDACTED***" } }
     */
    @PostMapping("/profile")
    fun updateProfile(@RequestBody request: UserProfileRequest): ResponseEntity<UserProfileResponse> {
        log.info(Markers.append("request", request), "Profile update request body")

        return ResponseEntity.ok(
            UserProfileResponse(id = "USR-001", name = request.name, status = "UPDATED")
        )
    }

    /**
     * Protected - masks Authorization header (Bearer + JWT).
     */
    @GetMapping("/protected")
    fun protectedEndpoint(
        @RequestHeader("Authorization", required = false) authHeader: String?
    ): ResponseEntity<Map<String, String>> {
        log.info("Protected resource access: {}", kv("authorization", authHeader ?: "none"))

        return ResponseEntity.ok(mapOf("message" to "Access granted", "resource" to "secret-data"))
    }
}
