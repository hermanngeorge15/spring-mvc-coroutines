package com.kss.proj.springmvccoroutines.validator.rulebuilder

import com.kss.proj.springmvccoroutines.validator.FieldCategory
import com.kss.proj.springmvccoroutines.validator.FieldRule
import com.kss.proj.springmvccoroutines.validator.GroupRule
import com.kss.proj.springmvccoroutines.validator.Severity
import com.kss.proj.springmvccoroutines.validator.ValidationSchema

class FieldRuleBuilder(
    private val path: String,
) {
    private var category: FieldCategory = FieldCategory.REQUIRED
    private var severity: Severity = Severity.CRITICAL
    private var defaultValue: Any? = null

    fun required(severity: Severity = Severity.CRITICAL) {
        this.category = FieldCategory.REQUIRED
        this.severity = severity
    }

    fun defaultsTo(value: Any){
        this.category = FieldCategory.DEFAULTABLE
        this.defaultValue = value
    }

    fun audit(){
        this.category = FieldCategory.AUDIT
        this.severity = Severity.INFO
    }

    fun build(): FieldRule = FieldRule(
        path = path,
        category = category,
        severity = severity,
        defaultValue = defaultValue
    )
}

class GroupRuleBuilder(
    private val paths: List<String>
) {
    private var severity: Severity = Severity.CRITICAL
    private var message: String = "Required fields missing: ${paths.joinToString()}"

    fun severity(s: Severity){
        this.severity = s
    }

    fun message(msg: String){
        this.message = msg
    }

    fun build(): GroupRule = GroupRule(
        paths = paths,
        severity = severity,
        message = message
    )
}

class SchemaBuilder{
    private val fields = mutableListOf<FieldRule>()
    private val groups = mutableListOf<GroupRule>()

    fun field(path: String, block: FieldRuleBuilder.() -> Unit = {}){
        fields += FieldRuleBuilder(path).apply(block).build()
    }

    fun allOf(vararg fields: String, block: GroupRuleBuilder.() -> Unit = {}) {
        groups += GroupRuleBuilder(fields.toList()).apply(block).build()
    }

    fun build(): ValidationSchema = ValidationSchema(
        fields = fields,
        groups = groups
    )
}

fun validationSchema(block: SchemaBuilder.() -> Unit): ValidationSchema = SchemaBuilder().apply(block).build()
