# Sensitive Data Masking in Spring Boot + ELK Stack

This document outlines the recommended approach for logging request/response and business events while ensuring automatic masking/redaction of sensitive data before logs reach Elasticsearch.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Layer 1: Application Layer (Logback)](#layer-1-application-layer-logback)
- [Layer 2: Filebeat Processors](#layer-2-filebeat-processors)
- [Layer 3: Logstash Filters](#layer-3-logstash-filters)
- [Recommended Implementation](#recommended-implementation)
- [Configuration Reference](#configuration-reference)
- [Testing](#testing)

---

## Overview

When logging for observability, certain fields contain sensitive data that must never reach log aggregation systems:

| Category | Examples |
|----------|----------|
| Authentication | Tokens, API keys, passwords, secrets |
| Personal Data | Email addresses, phone numbers, national IDs |
| Financial | Credit card numbers, bank accounts, CVV |
| Healthcare | Medical record numbers, diagnoses |

**Principle**: Mask at the earliest possible point (application layer) with secondary protection in the pipeline.

---

## Architecture

```
┌─────────────────┐    ┌───────────┐    ┌───────────┐    ┌───────────────┐
│   Application   │───▶│ Filebeat  │───▶│ Logstash  │───▶│ Elasticsearch │
│    (Logback)    │    │           │    │           │    │               │
└─────────────────┘    └───────────┘    └───────────┘    └───────────────┘
        │                    │               │
   Primary masking     Secondary        Optional
   (structured JSON)   validation       enrichment
```

**Defense in Depth Strategy**:
1. **Application Layer** — Primary defense, handles 95% of masking
2. **Filebeat** — Safety net for known sensitive fields
3. **Logstash** — Complex transformations if needed

---

## Layer 1: Application Layer (Logback)

This is where the heavy lifting should happen. Three main approaches are available.

### Option A: Zalando Logbook (HTTP Request/Response)

Best for masking HTTP request/response bodies and headers.

**Dependencies**:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>logbook-spring-boot-starter</artifactId>
    <version>3.9.0</version>
</dependency>
```

**Configuration** (`application.yml`):

```yaml
logbook:
  filter:
    enabled: true
  format:
    style: json
  obfuscate:
    headers:
      - Authorization
      - X-API-Key
      - X-Auth-Token
      - Cookie
      - Set-Cookie
    parameters:
      - password
      - token
      - apiKey
      - secret
    json-body-fields:
      - password
      - token
      - apiKey
      - secret
      - creditCardNumber
      - cvv
      - ssn
      - nationalId
```

**Custom Body Filter** (for complex masking rules):

```kotlin
@Configuration
class LogbookConfiguration {

    @Bean
    fun bodyFilter(): BodyFilter {
        return BodyFilter.merge(
            // JSON field masking
            JsonPathBodyFilters.jsonPath("$.password").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.token").replace("***REDACTED***"),
            JsonPathBodyFilters.jsonPath("$.creditCard.number").replace { 
                it.take(4) + "-****-****-" + it.takeLast(4) 
            },
            JsonPathBodyFilters.jsonPath("$.email").replace { 
                "***@" + it.substringAfter("@") 
            }
        )
    }
}
```

### Option B: Custom Logback Layout (General Logs)

For masking sensitive data in all log messages (not just HTTP).

**Implementation**:

```kotlin
package com.example.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import java.util.regex.Pattern

class MaskingPatternLayout : PatternLayout() {

    private val maskingRules = listOf(
        // Tokens and API Keys
        MaskingRule(
            pattern = Pattern.compile(
                "(\"?(?:token|api[_-]?key|authorization|bearer|secret|credential)\"?\\s*[:=]\\s*)\"?([^\"\\s,}\\]]+)\"?",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1\"***REDACTED***\""
        ),
        // Passwords
        MaskingRule(
            pattern = Pattern.compile(
                "(\"?password\"?\\s*[:=]\\s*)\"?([^\"\\s,}\\]]+)\"?",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1\"***REDACTED***\""
        ),
        // Credit Card Numbers (with partial visibility)
        MaskingRule(
            pattern = Pattern.compile(
                "\\b(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})\\b"
            ),
            replacement = "$1-****-****-$4"
        ),
        // Email Addresses (partial masking)
        MaskingRule(
            pattern = Pattern.compile(
                "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
            ),
            replacement = "***@$2"
        ),
        // JWT Tokens
        MaskingRule(
            pattern = Pattern.compile(
                "eyJ[a-zA-Z0-9_-]*\\.eyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*"
            ),
            replacement = "***JWT_REDACTED***"
        ),
        // Bearer Token in Header format
        MaskingRule(
            pattern = Pattern.compile(
                "(Bearer\\s+)[A-Za-z0-9._-]+",
                Pattern.CASE_INSENSITIVE
            ),
            replacement = "$1***REDACTED***"
        )
    )

    override fun doLayout(event: ILoggingEvent): String {
        var message = super.doLayout(event)
        
        maskingRules.forEach { rule ->
            message = rule.pattern.matcher(message).replaceAll(rule.replacement)
        }
        
        return message
    }

    private data class MaskingRule(
        val pattern: Pattern,
        val replacement: String
    )
}
```

**Logback Configuration** (`logback-spring.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>
    
    <!-- Console Appender with Masking -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="com.example.logging.MaskingPatternLayout">
                <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </layout>
        </encoder>
    </appender>

    <!-- File Appender with Masking (for Filebeat) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/${appName}/${appName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/${appName}/${appName}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="com.example.logging.MaskingPatternLayout">
                <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </layout>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### Option C: Logstash Logback Encoder with Custom Decorator

For JSON-formatted logs with field-level masking.

**Dependencies**:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.0</version>
</dependency>
```

**Custom JSON Generator Decorator**:

```kotlin
package com.example.logging

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate
import net.logstash.logback.decorate.JsonGeneratorDecorator

class MaskingJsonGeneratorDecorator : JsonGeneratorDecorator {

    private val sensitiveFields = setOf(
        "password", "token", "apikey", "api_key", "secret",
        "authorization", "credential", "bearer", "accesstoken",
        "access_token", "refreshtoken", "refresh_token", "cvv",
        "creditcardnumber", "credit_card_number", "ssn", "nationalid"
    )

    private val partialMaskFields = mapOf(
        "email" to { value: String -> "***@${value.substringAfter("@", "***")}" },
        "phone" to { value: String -> "***${value.takeLast(4)}" },
        "creditcard" to { value: String -> 
            if (value.length >= 8) "${value.take(4)}****${value.takeLast(4)}" else "***"
        }
    )

    override fun decorate(generator: JsonGenerator): JsonGenerator {
        return MaskingJsonGenerator(generator, sensitiveFields, partialMaskFields)
    }

    private class MaskingJsonGenerator(
        delegate: JsonGenerator,
        private val sensitiveFields: Set<String>,
        private val partialMaskFields: Map<String, (String) -> String>
    ) : JsonGeneratorDelegate(delegate, false) {

        private var currentFieldName: String? = null

        override fun writeFieldName(name: String) {
            currentFieldName = name.lowercase()
            super.writeFieldName(name)
        }

        override fun writeString(text: String?) {
            val masked = when {
                text == null -> null
                sensitiveFields.contains(currentFieldName) -> "***REDACTED***"
                partialMaskFields.containsKey(currentFieldName) -> 
                    partialMaskFields[currentFieldName]?.invoke(text) ?: text
                else -> text
            }
            super.writeString(masked)
        }
    }
}
```

**Logback Configuration** (`logback-spring.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>
    
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <jsonGeneratorDecorator class="com.example.logging.MaskingJsonGeneratorDecorator"/>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <customFields>{"application":"${appName}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

---

## Layer 2: Filebeat Processors

Secondary defense layer in Filebeat configuration.

**Configuration** (`filebeat.yml`):

```yaml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/app/*.log
    json.keys_under_root: true
    json.add_error_key: true
    json.message_key: message

processors:
  # Drop known sensitive fields entirely
  - drop_fields:
      fields:
        - password
        - token
        - apiKey
        - api_key
        - secret
        - authorization
        - creditCardNumber
        - cvv
        - ssn
      ignore_missing: true

  # Regex-based masking for patterns that might slip through
  - script:
      lang: javascript
      id: mask_sensitive_data
      source: |
        function process(event) {
          var message = event.Get("message");
          if (message) {
            // Mask JWT tokens
            message = message.replace(/eyJ[a-zA-Z0-9_-]*\.eyJ[a-zA-Z0-9_-]*\.[a-zA-Z0-9_-]*/g, "***JWT_REDACTED***");
            
            // Mask Bearer tokens
            message = message.replace(/(Bearer\s+)[A-Za-z0-9._-]+/gi, "$1***REDACTED***");
            
            // Mask credit card numbers
            message = message.replace(/\b(\d{4})[\s-]?\d{4}[\s-]?\d{4}[\s-]?(\d{4})\b/g, "$1-****-****-$2");
            
            // Mask password fields in JSON
            message = message.replace(/"password"\s*:\s*"[^"]*"/gi, '"password":"***REDACTED***"');
            
            // Mask token fields in JSON
            message = message.replace(/"token"\s*:\s*"[^"]*"/gi, '"token":"***REDACTED***"');
            
            event.Put("message", message);
          }
          return event;
        }

  # Add metadata
  - add_host_metadata:
      when.not.contains.tags: forwarded
  - add_cloud_metadata: ~

output.logstash:
  hosts: ["logstash:5044"]
```

---

## Layer 3: Logstash Filters

Optional additional processing layer.

**Configuration** (`logstash.conf`):

```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  # Remove any remaining sensitive fields
  mutate {
    remove_field => ["password", "token", "apiKey", "secret", "authorization"]
  }

  # Ruby filter for complex masking scenarios
  ruby {
    code => '
      msg = event.get("message")
      if msg.is_a?(String)
        # Mask any remaining JWT tokens
        msg = msg.gsub(/eyJ[a-zA-Z0-9_-]*\.eyJ[a-zA-Z0-9_-]*\.[a-zA-Z0-9_-]*/, "***JWT_REDACTED***")
        
        # Mask Bearer tokens
        msg = msg.gsub(/Bearer\s+[A-Za-z0-9._-]+/i, "Bearer ***REDACTED***")
        
        # Mask password patterns
        msg = msg.gsub(/"password"\s*:\s*"[^"]*"/i, "\"password\":\"***REDACTED***\"")
        
        event.set("message", msg)
      end
    '
  }

  # Parse JSON if message is JSON formatted
  if [message] =~ /^\{.*\}$/ {
    json {
      source => "message"
      target => "parsed"
      skip_on_invalid_json => true
    }
    
    # Remove sensitive fields from parsed JSON
    mutate {
      remove_field => [
        "[parsed][password]",
        "[parsed][token]",
        "[parsed][apiKey]",
        "[parsed][secret]"
      ]
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "logs-%{[application]}-%{+YYYY.MM.dd}"
  }
}
```

---

## Recommended Implementation

For a Spring Boot application with the ELK stack, we recommend:

| Layer | Tool | Purpose |
|-------|------|---------|
| Application | Zalando Logbook | HTTP request/response masking |
| Application | Custom MaskingPatternLayout | General log message masking |
| Filebeat | drop_fields + script processor | Safety net for known fields |
| Logstash | Optional | Complex transformations only if needed |

### Quick Start Setup

1. Add dependencies to `pom.xml`:

```xml
<dependencies>
    <!-- HTTP Request/Response Logging with Masking -->
    <dependency>
        <groupId>org.zalando</groupId>
        <artifactId>logbook-spring-boot-starter</artifactId>
        <version>3.9.0</version>
    </dependency>
    
    <!-- JSON Logging for ELK -->
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>8.0</version>
    </dependency>
</dependencies>
```

2. Configure `application.yml` for Logbook
3. Implement `MaskingPatternLayout` or `MaskingJsonGeneratorDecorator`
4. Configure `logback-spring.xml`
5. Update Filebeat configuration with safety processors

---

## Configuration Reference

### Sensitive Data Categories

```yaml
# Recommended fields to always mask
sensitive-fields:
  authentication:
    - password
    - token
    - api_key
    - apiKey
    - secret
    - credential
    - authorization
    - bearer
    - access_token
    - refresh_token
    - client_secret
    
  personal:
    - ssn
    - social_security_number
    - national_id
    - passport_number
    - drivers_license
    
  financial:
    - credit_card_number
    - card_number
    - cvv
    - cvc
    - bank_account
    - routing_number
    
  partial-mask:  # Show partial data for debugging
    - email        # ***@domain.com
    - phone        # ***1234
    - ip_address   # 192.168.***
```

### Environment-Specific Configuration

```yaml
# application-dev.yml
logging:
  masking:
    enabled: true
    strict: false  # Log field names but not values

# application-prod.yml  
logging:
  masking:
    enabled: true
    strict: true   # Full masking, no field name hints
```

---

## Testing

### Unit Test for MaskingPatternLayout

```kotlin
class MaskingPatternLayoutTest {

    private val layout = MaskingPatternLayout().apply {
        pattern = "%msg"
        context = LoggerFactory.getILoggerFactory() as LoggerContext
        start()
    }

    @Test
    fun `should mask password in JSON`() {
        val event = createLoggingEvent("""{"username":"john","password":"secret123"}""")
        val result = layout.doLayout(event)
        
        assertThat(result).contains("***REDACTED***")
        assertThat(result).doesNotContain("secret123")
    }

    @Test
    fun `should mask JWT tokens`() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
        val event = createLoggingEvent("Token: $jwt")
        val result = layout.doLayout(event)
        
        assertThat(result).contains("***JWT_REDACTED***")
        assertThat(result).doesNotContain("eyJ")
    }

    @Test
    fun `should partially mask credit card`() {
        val event = createLoggingEvent("Card: 4111-1111-1111-1111")
        val result = layout.doLayout(event)
        
        assertThat(result).contains("4111-****-****-1111")
    }

    @Test
    fun `should partially mask email`() {
        val event = createLoggingEvent("Email: john.doe@example.com")
        val result = layout.doLayout(event)
        
        assertThat(result).contains("***@example.com")
        assertThat(result).doesNotContain("john.doe")
    }

    private fun createLoggingEvent(message: String): ILoggingEvent {
        return LoggingEvent().apply {
            this.message = message
            level = Level.INFO
        }
    }
}
```

### Integration Test

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class LogMaskingIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should mask sensitive data in HTTP logs`() {
        val logCaptor = LogCaptor.forRoot()

        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"test","password":"supersecret"}""")
        )

        val logs = logCaptor.logs.joinToString("\n")
        
        assertThat(logs).doesNotContain("supersecret")
        assertThat(logs).contains("***REDACTED***")
    }
}
```

---

## References

- [Zalando Logbook](https://github.com/zalando/logbook)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [Filebeat Processors](https://www.elastic.co/guide/en/beats/filebeat/current/filtering-and-enhancing-data.html)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
