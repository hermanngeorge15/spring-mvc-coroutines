package com.kss.proj.springmvccoroutines.logging

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate
import net.logstash.logback.decorate.JsonGeneratorDecorator

class MaskingJsonGeneratorDecorator : JsonGeneratorDecorator {

    private val sensitiveFields = setOf(
        "password", "token", "apikey", "api_key", "secret",
        "authorization", "credential", "bearer", "accesstoken",
        "access_token", "refreshtoken", "refresh_token", "cvv",
        "creditcardnumber", "credit_card_number", "ssn", "nationalid"
    )

    private val partialMaskFields = mapOf<String, (String) -> String>(
        "email" to { value -> "***@${value.substringAfter("@", "***")}" },
        "phone" to { value -> "***${value.takeLast(4)}" },
        "creditcard" to { value ->
            if (value.length >= 8) "${value.take(4)}****${value.takeLast(4)}" else "***"
        }
    )

    override fun decorate(generator: JsonGenerator): JsonGenerator {
        return MaskingJsonGenerator(generator, sensitiveFields, partialMaskFields)
    }

    private class MaskingJsonGenerator(
        delegate: JsonGenerator,
        private val sensitiveFields: Set<String>,
        private val partialMaskFields: Map<String, (String) -> String>
    ) : JsonGeneratorDelegate(delegate, false) {

        private var currentFieldName: String? = null

        override fun writeFieldName(name: String) {
            currentFieldName = name.lowercase()
            super.writeFieldName(name)
        }

        override fun writeString(text: String?) {
            val masked = when {
                text == null -> null
                sensitiveFields.contains(currentFieldName) -> "***REDACTED***"
                partialMaskFields.containsKey(currentFieldName) ->
                    partialMaskFields[currentFieldName]?.invoke(text) ?: text
                else -> text
            }
            super.writeString(masked)
        }
    }
}
