package com.kss.proj.springmvccoroutines.validator

import org.springframework.stereotype.Service

@Service
class RequestValidationService {

    private val validators = mapOf(
        "flat" to MapValidator(Schemas.flatRequest),
        "nested" to MapValidator(Schemas.nestedRequest),
    )

    fun validate(schemaKey: String, body: Map<String, Any?>): ValidationResult =
        validators[schemaKey]?.validate(body)
            ?: error("No schema registered for key '$schemaKey'")
}