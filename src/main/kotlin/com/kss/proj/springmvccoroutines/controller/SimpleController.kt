package com.kss.proj.springmvccoroutines.controller

import com.kss.proj.springmvccoroutines.validator.RequestValidationService
import com.kss.proj.springmvccoroutines.validator.ValidationResult
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/simple")
class SimpleController(
    private val validationService: RequestValidationService,
) {

    @PostMapping
    fun handle(@RequestBody body: Map<String, Any?>): ResponseEntity<*> {
        return when (val result = validationService.validate("flat", body)) {
            is ValidationResult.Success -> {
                log.info("Flat request OK, audit={}", result.auditContext.fields)
                ResponseEntity.ok(mapOf("status" to "processed", "data" to result.enrichedMap))
            }

            is ValidationResult.Failure -> {
                log.warn("Flat request failed: {}", result.issues)
                ResponseEntity.badRequest().body(
                    mapOf(
                        "status" to "error",
                        "issues" to result.issues.map { mapOf("path" to it.path, "message" to it.message, "severity" to it.severity) },
                        "audit" to result.auditContext.fields,
                    )
                )
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimpleController::class.java)
    }
}
