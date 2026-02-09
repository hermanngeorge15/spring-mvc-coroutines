package com.kss.proj.springmvccoroutines.validator

data class FieldRule(
    val path: String,
    val category: FieldCategory,
    val severity: Severity = Severity.CRITICAL,
    val defaultValue: Any? = null,
)