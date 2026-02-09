package com.kss.proj.springmvccoroutines.validator

data class AuditContext(
    val fields: Map<String, Any?>
) {
    fun get(key: String): Any? = fields[key]
    fun isNotEmpty(): Boolean = fields.isNotEmpty()
}