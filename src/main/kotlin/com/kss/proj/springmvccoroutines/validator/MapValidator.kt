package com.kss.proj.springmvccoroutines.validator

class MapValidator(
    private val schema: ValidationSchema
) {
    fun validate(input: Map<String, Any?>): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        var enriched = input

        // 1. Check individual field rules
        for (rule in schema.fields) {
            val value = input.getByPath(rule.path)

            when (rule.category) {
                FieldCategory.REQUIRED -> {
                    if (isBlankOrEmpty(value)) {
                        issues += ValidationIssue(
                            path = rule.path,
                            message = "Required field '${rule.path}' is missing or blank",
                            severity = rule.severity,
                        )
                    }
                }

                FieldCategory.DEFAULTABLE -> {
                    if (isBlankOrEmpty(value)) {
                        enriched = enriched.setByPath(rule.path, rule.defaultValue)
                        issues += ValidationIssue(
                            path = rule.path,
                            message = "Field '${rule.path}' was blank, applied default: ${rule.defaultValue}",
                            severity = Severity.INFO,
                        )
                    }
                }

                FieldCategory.AUDIT -> {
                    // Audit fields: no error, just collected below
                }
            }
        }

        // 2. Check group rules (allOf)
        for (group in schema.groups) {
            val missing = group.paths.filter { isBlankOrEmpty(input.getByPath(it)) }
            if (missing.isNotEmpty()) {
                issues += ValidationIssue(
                    path = missing.joinToString(", "),
                    message = "${group.message}: [${missing.joinToString()}]",
                    severity = group.severity,
                )
            }
        }

        // 3. Extract audit fields (best-effort)
        val auditFields = schema.fields
            .filter { it.category == FieldCategory.AUDIT }
            .associate { it.path to enriched.getByPath(it.path) }
        val audit = AuditContext(auditFields)

        // 4. Determine result — any CRITICAL or WARN issue → Failure
        val hasCritical = issues.any { it.severity == Severity.CRITICAL || it.severity == Severity.WARNING }

        return if (hasCritical) {
            ValidationResult.Failure(issues = issues, auditContext = audit)
        } else {
            ValidationResult.Success(enrichedMap = enriched, auditContext = audit, issues = issues)
        }
    }
}