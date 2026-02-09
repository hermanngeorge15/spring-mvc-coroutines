package com.kss.proj.springmvccoroutines.validator

data class ValidationIssue(
    val path: String,
    val message: String,
    val severity: Severity
)
