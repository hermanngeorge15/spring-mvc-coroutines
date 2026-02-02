package com.kss.proj.springmvccoroutines.validator

import com.kss.proj.springmvccoroutines.dto.ValidationError

fun interface Rule<T> {
    fun check(value: T): List<ValidationError>
}