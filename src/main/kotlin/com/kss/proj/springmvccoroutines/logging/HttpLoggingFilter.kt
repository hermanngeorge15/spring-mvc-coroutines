package com.kss.proj.springmvccoroutines.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.StandardCharsets

/**
 * HTTP request/response logging filter using pure Logback (no Logbook dependency).
 *
 * This filter:
 * 1. Wraps request/response to cache the body for logging
 * 2. Logs the full request (method, URI, headers, body)
 * 3. Logs the full response (status, headers, body, duration)
 * 4. Masking is handled by MaskingPatternLayout (regex-based)
 *
 * To use this instead of Logbook:
 * 1. Remove/disable LogbookConfiguration
 * 2. Remove logbook dependency or set logbook.filter.enabled=false
 * 3. This filter will handle HTTP logging with masking via MaskingPatternLayout
 */
/**
 * Enable with: logbook.filter.enabled=false (disables Logbook, enables this filter)
 * Or explicitly: http.logging.filter.enabled=true
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = ["logbook.filter.enabled"], havingValue = "false")
class HttpLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger("http.logging")

    // Headers to mask completely
    private val sensitiveHeaders = setOf(
        "authorization", "x-api-key", "x-auth-token", "cookie", "set-cookie"
    )

    // Skip logging for these paths
    private val excludedPaths = setOf(
        "/actuator", "/health", "/favicon.ico"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip excluded paths
        if (excludedPaths.any { request.requestURI.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logRequest(wrappedRequest)
            logResponse(wrappedResponse, duration)
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun logRequest(request: ContentCachingRequestWrapper) {
        val method = request.method
        val uri = request.requestURI
        val query = request.queryString?.let { "?$it" } ?: ""
        val headers = formatHeaders(request)
        val body = getRequestBody(request)

        log.info(
            ">>> REQUEST: {} {}{} | Headers: {} | Body: {}",
            method, uri, query, headers, body
        )
    }

    private fun logResponse(response: ContentCachingResponseWrapper, duration: Long) {
        val status = response.status
        val headers = formatResponseHeaders(response)
        val body = getResponseBody(response)

        log.info(
            "<<< RESPONSE: {} | Duration: {}ms | Headers: {} | Body: {}",
            status, duration, headers, body
        )
    }

    private fun formatHeaders(request: HttpServletRequest): String {
        val headers = mutableMapOf<String, String>()
        request.headerNames.asIterator().forEach { name ->
            val value = if (sensitiveHeaders.contains(name.lowercase())) {
                "***MASKED***"
            } else {
                request.getHeader(name)
            }
            headers[name] = value
        }
        return headers.toString()
    }

    private fun formatResponseHeaders(response: HttpServletResponse): String {
        val headers = mutableMapOf<String, String>()
        response.headerNames.forEach { name ->
            val value = if (sensitiveHeaders.contains(name.lowercase())) {
                "***MASKED***"
            } else {
                response.getHeader(name)
            }
            headers[name] = value
        }
        return headers.toString()
    }

    private fun getRequestBody(request: ContentCachingRequestWrapper): String {
        val content = request.contentAsByteArray
        return if (content.isNotEmpty()) {
            String(content, StandardCharsets.UTF_8).take(MAX_BODY_LENGTH)
        } else {
            "[empty]"
        }
    }

    private fun getResponseBody(response: ContentCachingResponseWrapper): String {
        val content = response.contentAsByteArray
        return if (content.isNotEmpty()) {
            String(content, StandardCharsets.UTF_8).take(MAX_BODY_LENGTH)
        } else {
            "[empty]"
        }
    }

    companion object {
        private const val MAX_BODY_LENGTH = 10_000
    }
}
