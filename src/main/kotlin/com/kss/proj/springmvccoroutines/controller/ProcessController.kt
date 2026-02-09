package com.kss.proj.springmvccoroutines.controller

import com.kss.proj.springmvccoroutines.validator.RequestValidationService
import com.kss.proj.springmvccoroutines.validator.ValidationResult
import com.kss.proj.springmvccoroutines.validator.getByPath
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/process")
class ProcessController(
    private val validationService: RequestValidationService,
) {

    @PostMapping
    fun handle(@RequestBody body: Map<String, Any?>): ResponseEntity<*> {
        return when (val result = validationService.validate("nested", body)) {
            is ValidationResult.Success -> {
                val enriched = result.enrichedMap
                val audit = result.auditContext

                log.info(
                    "Processing userId={}, role={}, audit={}",
                    enriched.getByPath("data.userId"),
                    enriched.getByPath("data.role"),
                    audit.fields,
                )

                // Pass enriched map (with defaults applied) to your business logic
                val output = processBusinessLogic(enriched)

                ResponseEntity.ok(mapOf("status" to "processed", "result" to output))
            }

            is ValidationResult.Failure -> {
                log.warn(
                    "Nested request failed: issues={}, audit={}",
                    result.issues,
                    result.auditContext.fields,
                )
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

    private fun processBusinessLogic(data: Map<String, Any?>): String {
        // your actual processing here
        return "done"
    }

    companion object{
        private val log = org.slf4j.LoggerFactory.getLogger(ProcessController::class.java)
    }
}