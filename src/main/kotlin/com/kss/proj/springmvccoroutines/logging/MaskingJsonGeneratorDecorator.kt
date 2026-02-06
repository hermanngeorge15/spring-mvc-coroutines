package com.kss.proj.springmvccoroutines.logging

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.SerializableString
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
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

        override fun writeFieldName(name: SerializableString) {
            currentFieldName = name.value.lowercase()
            super.writeFieldName(name)
        }

        override fun writeString(text: String?) {
            super.writeString(maskValue(text))
        }

        override fun writeString(text: CharArray, offset: Int, len: Int) {
            val value = String(text, offset, len)
            val masked = maskValue(value)
            if (masked !== value) {
                super.writeString(masked)
            } else {
                super.writeString(text, offset, len)
            }
        }

        override fun writeString(text: SerializableString) {
            val masked = maskValue(text.value)
            if (masked != text.value) {
                super.writeString(masked)
            } else {
                super.writeString(text)
            }
        }

        override fun writeStringField(name: String, value: String) {
            writeFieldName(name)
            writeString(value)
        }

        /**
         * Override writeTree to manually traverse the JSON tree through our generator,
         * ensuring that every writeFieldName + writeString goes through our masking logic.
         * This is needed because the default writeTree can bypass the delegate's write methods.
         */
        override fun writeTree(tree: TreeNode?) {
            when (tree) {
                null -> writeNull()
                is ObjectNode -> writeObjectNode(tree)
                is ArrayNode -> writeArrayNode(tree)
                is JsonNode -> writeJsonLeaf(tree)
                else -> super.writeTree(tree)
            }
        }

        private fun writeObjectNode(node: ObjectNode) {
            writeStartObject()
            val fieldNames = node.fieldNames()
            while (fieldNames.hasNext()) {
                val name = fieldNames.next()
                writeFieldName(name)
                writeTree(node.get(name))
            }
            writeEndObject()
        }

        private fun writeArrayNode(node: ArrayNode) {
            writeStartArray()
            for (element in node) {
                writeTree(element)
            }
            writeEndArray()
        }

        private fun writeJsonLeaf(node: JsonNode) {
            when {
                node.isTextual -> writeString(node.textValue())
                node.isInt -> writeNumber(node.intValue())
                node.isLong -> writeNumber(node.longValue())
                node.isDouble -> writeNumber(node.doubleValue())
                node.isFloat -> writeNumber(node.floatValue())
                node.isBigDecimal -> writeNumber(node.decimalValue())
                node.isBigInteger -> writeNumber(node.bigIntegerValue())
                node.isBoolean -> writeBoolean(node.booleanValue())
                node.isNull -> writeNull()
                else -> super.writeTree(node)
            }
        }

        private fun maskValue(text: String?): String? {
            return when {
                text == null -> null
                sensitiveFields.contains(currentFieldName) -> "***REDACTED***"
                partialMaskFields.containsKey(currentFieldName) ->
                    partialMaskFields[currentFieldName]?.invoke(text) ?: text
                else -> text
            }
        }
    }
}
