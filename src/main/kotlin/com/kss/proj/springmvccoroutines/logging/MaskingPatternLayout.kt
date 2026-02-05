package com.kss.proj.springmvccoroutines.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import java.util.regex.Pattern

class MaskingPatternLayout : PatternLayout() {

    private val maskingRules = listOf(
        // Tokens and API Keys
        MaskingRule(
            pattern = Pattern.compile(
                "(\"?(?:token|api[_-]?key|authorization|bearer|secret|credential)\"?\\s*[:=]\\s*)\"?([^\"\\s,}\\]]+)\"?",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1\"***REDACTED***\""
        ),
        // Passwords
        MaskingRule(
            pattern = Pattern.compile(
                "(\"?password\"?\\s*[:=]\\s*)\"?([^\"\\s,}\\]]+)\"?",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1\"***REDACTED***\""
        ),
        // Credit Card Numbers (preserve first 4 and last 4)
        MaskingRule(
            pattern = Pattern.compile(
                "\\b(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})\\b"
            ),
            replacement = "$1-****-****-$4"
        ),
        // Email Addresses (partial masking)
        MaskingRule(
            pattern = Pattern.compile(
                "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
            ),
            replacement = "***@$2"
        ),
        // JWT Tokens
        MaskingRule(
            pattern = Pattern.compile(
                "eyJ[a-zA-Z0-9_-]*\\.eyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*"
            ),
            replacement = "***JWT_REDACTED***"
        ),
        // Bearer Token in Header format
        MaskingRule(
            pattern = Pattern.compile(
                "(Bearer\\s+)[A-Za-z0-9._-]+",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1***REDACTED***"
        )
    )

    override fun doLayout(event: ILoggingEvent): String {
        var message = super.doLayout(event)

        maskingRules.forEach { rule ->
            message = rule.pattern.matcher(message).replaceAll(rule.replacement)
        }

        return message
    }

    private data class MaskingRule(
        val pattern: Pattern,
        val replacement: String
    )
}
