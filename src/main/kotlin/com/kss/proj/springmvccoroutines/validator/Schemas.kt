package com.kss.proj.springmvccoroutines.validator

import com.kss.proj.springmvccoroutines.validator.rulebuilder.validationSchema

object Schemas {
    val flatRequest = validationSchema {
        field("token") { required() }
        field("requestId") { required() }
        allOf("aud", "baut") {
            severity(Severity.CRITICAL)
            message("Auth fields missing")
        }
    }

    val nestedRequest = validationSchema {
        field("token") { required() }
        field("data.userId") { required() }
        field("data.role") { defaultsTo("USER") }
        field("data.features.beta") { defaultsTo(false) }

        allOf("raut", "aud", "baut") {
            severity(Severity.CRITICAL)
            message("Auth fields missing")
        }

        field("audit1") { audit() }
        field("data.audit2") { audit() }
    }
}