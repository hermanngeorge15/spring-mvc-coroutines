package com.kss.proj.springmvccoroutines.validator

import com.kss.proj.springmvccoroutines.dto.ValidationError

open class Validator<RAW, VALIDATED>(
    private val rules: List<Rule<RAW>>,
    private val transform: (RAW) -> VALIDATED?
) {
    fun validate(raw: RAW): ValidationResult<VALIDATED> {
        val errors = rules.flatMap { it.check(raw) }

        if (errors.isNotEmpty()) {
            return ValidationResult.Failure(errors)
        }

        val validated = transform(raw) ?: return ValidationResult.Failure(
            listOf(ValidationError("_root", "Construction failed", "CONSTRUCTION_FAILED"))
        )

        return ValidationResult.Valid(validated)
    }
}