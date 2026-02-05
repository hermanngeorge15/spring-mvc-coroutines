package com.kss.proj.springmvccoroutines.logging

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.zalando.logbook.BodyFilter
import org.zalando.logbook.json.JsonPathBodyFilters

@Configuration
@Profile("logbook")
class LogbookConfiguration {

    @Bean
    fun bodyFilter(): BodyFilter {
        val filters = listOf(
            JsonPathBodyFilters.jsonPath("$.password").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.token").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.apiKey").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.secret").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.ssn").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.nationalId").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.cvv").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.creditCardNumber").replace { value ->
                if (value.length >= 8) "${value.take(4)}-****-****-${value.takeLast(4)}"
                else "***REDACTED***"
            },
            JsonPathBodyFilters.jsonPath("$.cardNumber").replace { value ->
                if (value.length >= 8) "${value.take(4)}-****-****-${value.takeLast(4)}"
                else "***REDACTED***"
            },
            JsonPathBodyFilters.jsonPath("$.email").replace { value ->
                "***@${value.substringAfter("@", "***")}"
            },
            JsonPathBodyFilters.jsonPath("$.phone").replace { value ->
                "***${value.takeLast(4)}"
            }
        )

        return filters.reduce { acc, filter -> BodyFilter.merge(acc, filter) }
    }
}
