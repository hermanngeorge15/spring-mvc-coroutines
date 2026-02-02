package com.kss.proj.springmvccoroutines.validator

import com.kss.proj.springmvccoroutines.dto.ValidationError


sealed interface ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>
    data class Failure(val errors: List<ValidationError>) : ValidationResult<Nothing>
}