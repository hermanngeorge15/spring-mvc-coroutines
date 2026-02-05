package com.kss.proj.springmvccoroutines.example

import net.logstash.logback.argument.StructuredArguments.kv
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
 * How it works:
 *   - Uses StructuredArguments.kv("fieldName", value) to emit sensitive data as
 *     separate JSON fields in the log output.
 *   - MaskingJsonGeneratorDecorator intercepts the JSON serialization and replaces
 *     sensitive field values before they are written.
 *
 * Run with: ./gradlew bootRun --args='--spring.profiles.active=json'
 *
 * Masking behavior:
 *   - Fields like "password", "token", "ssn", "cvv", "apiKey" -> "***REDACTED***"
 *   - "email" -> "***@domain.com" (partial)
 *   - "phone" -> "***1234" (partial, last 4)
 *
 * NOTE: In text mode (no json profile), StructuredArguments render as key=value
 * in the message string, and MaskingPatternLayout applies regex-based masking.
 */
@RestController
@RequestMapping("/api/logback")
class LogbackDemoController {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Login - masks password, email, and JWT token via structured JSON fields.
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
     * Payment - masks credit card number, CVV, and email.
     */
    @PostMapping("/payment")
    fun processPayment(@RequestBody request: PaymentRequest): ResponseEntity<PaymentResponse> {
        log.info("Processing payment: {}, {}",
            kv("amount", request.amount),
            kv("currency", request.currency)
        )
        log.info("Payment card details: {}, {}, {}",
            kv("creditCardNumber", request.creditCardNumber),
            kv("cvv", request.cvv),
            kv("email", request.email)
        )

        return ResponseEntity.ok(
            PaymentResponse(
                transactionId = "TXN-${System.currentTimeMillis()}",
                status = "APPROVED",
                maskedCard = "${request.creditCardNumber.take(4)}****${request.creditCardNumber.takeLast(4)}"
            )
        )
    }

    /**
     * Profile - masks SSN, email, phone, and apiKey.
     */
    @PostMapping("/profile")
    fun updateProfile(@RequestBody request: UserProfileRequest): ResponseEntity<UserProfileResponse> {
        log.info("Updating profile: {}", kv("name", request.name))
        log.info("Profile sensitive data: {}, {}, {}, {}",
            kv("ssn", request.ssn),
            kv("email", request.email),
            kv("phone", request.phone),
            kv("apiKey", request.apiKey)
        )

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
