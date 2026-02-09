package com.kss.proj.springmvccoroutines.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import java.util.regex.Pattern

/**
 * Regex-based masking layout for Logback.
 * Acts as a safety net for any sensitive data that wasn't caught by other filters.
 *
 * NOTE: Values already containing "SHA256:" are skipped to avoid double-masking
 * when Logbook's custom filters have already applied SHA-256 hashing.
 */
class MaskingPatternLayout : PatternLayout() {

    // Regex to detect if value is already SHA-256 hashed
    private val sha256Pattern = Pattern.compile("SHA256:[a-f0-9]+")

    private val maskingRules = listOf(
        // Tokens and API Keys (skip if already SHA-256 hashed)
        MaskingRule(
            pattern = Pattern.compile(
                "(\"?(?:token|api[_-]?key|authorization|bearer|secret|credential)\"?\\s*[:=]\\s*)\"?([^\"\\s,}\\]]+)\"?",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1\"***REDACTED***\"",
            skipIfSha256 = true
        ),
        // Passwords (skip if already SHA-256 hashed)
        MaskingRule(
            pattern = Pattern.compile(
                "(\"?password\"?\\s*[:=]\\s*)\"?([^\"\\s,}\\]]+)\"?",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1\"***REDACTED***\"",
            skipIfSha256 = true
        ),
        // Credit Card Numbers (preserve first 4 and last 4) - always apply
        MaskingRule(
            pattern = Pattern.compile(
                "\\b(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})\\b"
            ),
            replacement = "$1-****-****-$4",
            skipIfSha256 = false
        ),
        // Email Addresses (partial masking) - skip if SHA-256 present nearby
        MaskingRule(
            pattern = Pattern.compile(
                "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
            ),
            replacement = "***@$2",
            skipIfSha256 = true
        ),
        // JWT Tokens - always mask these
        MaskingRule(
            pattern = Pattern.compile(
                "eyJ[a-zA-Z0-9_-]*\\.eyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*"
            ),
            replacement = "***JWT_REDACTED***",
            skipIfSha256 = false
        ),
        // Bearer Token in Header format (skip if already SHA-256 hashed)
        MaskingRule(
            pattern = Pattern.compile(
                "(Bearer\\s+)[A-Za-z0-9._-]+",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1***REDACTED***",
            skipIfSha256 = true
        )
    )

    override fun doLayout(event: ILoggingEvent): String {
        var message = super.doLayout(event)

        maskingRules.forEach { rule ->
            if (rule.skipIfSha256 && sha256Pattern.matcher(message).find()) {
                // Skip this rule if message already contains SHA-256 hashes
                // and rule is marked to skip in that case
                return@forEach
            }
            message = rule.pattern.matcher(message).replaceAll(rule.replacement)
        }

        return message
    }

    private data class MaskingRule(
        val pattern: Pattern,
        val replacement: String,
        val skipIfSha256: Boolean = false
    )
}
