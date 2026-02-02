package com.kss.proj.springmvccoroutines.validator

import com.kss.proj.springmvccoroutines.dto.ValidationError

/**
 * Validation rule factory functions
 * These functions create reusable validation rules that can be composed in validators
 */

/**
 * Creates a rule that checks if a string field is not blank (not null, empty, or whitespace)
 */
fun <T> notBlank(field: String, extract: (T) -> String?): Rule<T> = Rule { value ->
    when (val v = extract(value)) {
        null -> listOf(ValidationError(field, "$field is required", "REQUIRED"))
        else -> if (v.isBlank()) listOf(ValidationError(field, "$field must not be blank", "BLANK"))
        else emptyList()
    }
}

/**
 * Creates a rule that checks if a field is not null
 */
fun <T> notNull(field: String, extract: (T) -> Any?): Rule<T> = Rule { value ->
    if (extract(value) == null)
        listOf(ValidationError(field, "$field is required", "REQUIRED"))
    else emptyList()
}

/**
 * Creates a rule that checks if a string field matches an expected value
 */
fun <T> matchesValue(field: String, expected: String, extract: (T) -> String?): Rule<T> = Rule { value ->
    val actual = extract(value)
    if (actual != null && actual != expected)
        listOf(ValidationError(field, "$field must be '$expected', got: '$actual'", "INVALID_VALUE"))
    else emptyList()
}

/**
 * Creates a rule that validates a nested object using its own validator
 * Errors from the nested validator are prefixed with the field path
 */
fun <T, NESTED, V> nested(
    field: String,
    extract: (T) -> NESTED?,
    validator: Validator<NESTED, V>
): Rule<T> = Rule { value ->
    val raw = extract(value) ?: return@Rule listOf(ValidationError(field, "$field is required", "REQUIRED"))

    when (val result = validator.validate(raw)) {
        is ValidationResult.Valid -> emptyList()
        is ValidationResult.Failure -> result.errors.map { error ->
            error.copy(field = "$field.${error.field}")
        }
    }
}


