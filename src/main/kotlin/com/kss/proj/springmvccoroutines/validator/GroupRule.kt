package com.kss.proj.springmvccoroutines.validator

data class GroupRule(
    val paths: List<String>,
    val severity: Severity = Severity.CRITICAL,
    val message: String = "Group validation failed"
)