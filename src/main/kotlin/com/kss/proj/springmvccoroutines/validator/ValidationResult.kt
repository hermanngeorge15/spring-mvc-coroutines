package com.kss.proj.springmvccoroutines.validator

sealed interface ValidationResult {
    val auditContext: AuditContext
    val issues: List<ValidationIssue>

    data class Success(
        val enrichedMap: Map<String, Any?>,
        override val auditContext: AuditContext,
        override val issues: List<ValidationIssue> = emptyList()
    ) : ValidationResult

    data class Failure(
        override val auditContext: AuditContext,
        override val issues: List<ValidationIssue>
    ) : ValidationResult


}