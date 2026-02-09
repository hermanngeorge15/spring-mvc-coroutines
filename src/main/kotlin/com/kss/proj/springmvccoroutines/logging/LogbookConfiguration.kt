package com.kss.proj.springmvccoroutines.logging

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.BodyFilter
import org.zalando.logbook.Correlation
import org.zalando.logbook.HeaderFilter
import org.zalando.logbook.HttpLogWriter
import org.zalando.logbook.QueryFilter
import org.zalando.logbook.Precorrelation
import org.zalando.logbook.json.JsonPathBodyFilters
import org.slf4j.LoggerFactory
import java.security.MessageDigest

/**
 * Logbook configuration for HTTP request/response logging.
 * Enabled by default. Disable with: logbook.filter.enabled=false
 */
@Configuration
@ConditionalOnProperty(name = ["logbook.filter.enabled"], havingValue = "true", matchIfMissing = true)
class LogbookConfiguration {

    /**
     * SHA-256 hash function for masking sensitive values.
     * Returns first 16 chars of hex-encoded hash with prefix.
     */
    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(value.toByteArray())
        val hexHash = hash.joinToString("") { "%02x".format(it) }
        return "SHA256:${hexHash.take(16)}..."
    }

    /**
     * Custom header filter using SHA-256 hashing.
     */
    @Bean
    fun headerFilter(): HeaderFilter {
        val sensitiveHeaders = setOf(
            "authorization", "x-api-key", "x-auth-token", "cookie", "set-cookie"
        )
        return HeaderFilter { headers ->
            headers.apply { name, values ->
                if (sensitiveHeaders.contains(name.lowercase())) {
                    values.map { sha256(it) }
                } else {
                    values
                }
            }
        }
    }

    /**
     * Custom query parameter filter using SHA-256 hashing.
     */
    @Bean
    fun queryFilter(): QueryFilter {
        val sensitiveParams = setOf("password", "token", "apiKey", "secret", "key")
        return QueryFilter { query ->
            if (query.isNullOrEmpty()) return@QueryFilter query
            query.split("&").joinToString("&") { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2 && sensitiveParams.contains(parts[0].lowercase())) {
                    "${parts[0]}=${sha256(parts[1])}"
                } else {
                    param
                }
            }
        }
    }

    /**
     * Custom body filter using SHA-256 hashing for sensitive JSON fields.
     */
    @Bean
    fun bodyFilter(): BodyFilter {
        val filters = listOf(
            // Fields hashed with SHA-256
            JsonPathBodyFilters.jsonPath("$.password").replace { sha256(it) },
            JsonPathBodyFilters.jsonPath("$.token").replace { sha256(it) },
            JsonPathBodyFilters.jsonPath("$.refreshToken").replace { sha256(it) },
            JsonPathBodyFilters.jsonPath("$.apiKey").replace { sha256(it) },
            JsonPathBodyFilters.jsonPath("$.secret").replace { sha256(it) },
            JsonPathBodyFilters.jsonPath("$.ssn").replace { sha256(it) },
            JsonPathBodyFilters.jsonPath("$.nationalId").replace { sha256(it) },
            JsonPathBodyFilters.jsonPath("$.cvv").replace { sha256(it) },

            // Credit card: partial mask (first 4 + last 4) + SHA-256 of full number
            JsonPathBodyFilters.jsonPath("$.creditCardNumber").replace { value ->
                if (value.length >= 8) "${value.take(4)}****${value.takeLast(4)}|${sha256(value)}"
                else sha256(value)
            },
            JsonPathBodyFilters.jsonPath("$.cardNumber").replace { value ->
                if (value.length >= 8) "${value.take(4)}****${value.takeLast(4)}|${sha256(value)}"
                else sha256(value)
            },

            // Email: partial mask + SHA-256
            JsonPathBodyFilters.jsonPath("$.email").replace { value ->
                "***@${value.substringAfter("@", "***")}|${sha256(value)}"
            },

            // Phone: last 4 digits + SHA-256
            JsonPathBodyFilters.jsonPath("$.phone").replace { value ->
                "***${value.takeLast(4)}|${sha256(value)}"
            }
        )

        return filters.reduce { acc, filter -> BodyFilter.merge(acc, filter) }
    }

    @Bean
    fun writer(): HttpLogWriter {
        val logger = LoggerFactory.getLogger("org.zalando.logbook.Logbook")
        return object : HttpLogWriter {
            override fun isActive() = logger.isInfoEnabled
            override fun write(precorrelation: Precorrelation, request: String) = logger.info(request)
            override fun write(correlation: Correlation, response: String) = logger.info(response)
        }
    }
}
