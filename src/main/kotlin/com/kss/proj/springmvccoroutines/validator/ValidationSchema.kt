package com.kss.proj.springmvccoroutines.validator

data class ValidationSchema(
    val fields: List<FieldRule>,
    val groups: List<GroupRule>
)
