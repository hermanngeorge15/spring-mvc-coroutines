# Logging Resources - Documentation & Tutorials

## Table of Contents

- [Zalando Logbook](#zalando-logbook)
- [Logstash Logback Encoder](#logstash-logback-encoder)
- [Logback](#logback)
- [Related Tutorials](#related-tutorials)

---

## Zalando Logbook

An extensible Java library for HTTP request and response logging with built-in support for masking sensitive data.

### What It Does

- Intercepts and logs HTTP requests and responses automatically
- Masks sensitive data in headers, query parameters, and JSON bodies
- Integrates seamlessly with Spring Boot via auto-configuration
- Supports various output formats (JSON, cURL, HTTP)
- Works with Spring MVC, Spring WebFlux, and other frameworks

### Official Documentation

| Resource | Description |
|----------|-------------|
| [GitHub Repository](https://github.com/zalando/logbook) | Main repository with comprehensive README |
| [README.md](https://github.com/zalando/logbook/blob/main/README.md) | Full documentation with configuration options |
| [Releases](https://github.com/zalando/logbook/releases) | Version history and changelog |
| [Maven Central](https://mvnrepository.com/artifact/org.zalando/logbook-spring-boot-starter) | Dependency information |

### Key Sections in README

- **Filtering** - Include/exclude requests based on path, method, etc.
- **Formatting** - JSON, cURL, HTTP formats
- **Obfuscation** - Masking headers, parameters, and body fields
- **Writing** - Output to logger, stream, or custom writer
- **Spring Boot Auto-Configuration** - YAML/properties configuration
- **Strategy Pattern** - Customize logging behavior

### Tutorials & Articles

| Resource | Description |
|----------|-------------|
| [Baeldung - HTTP Logging with Logbook](https://www.baeldung.com/spring-logbook-http-logging) | Step-by-step Spring Boot integration |
| [JavaCodeGeeks - Logbook in Spring](https://www.javacodegeeks.com/http-request-and-response-logging-using-logbook-in-spring.html) | Practical examples with masking |
| [NashTech Blog - Logbook Integration](https://blog.nashtechglobal.com/introducing-zalando-logbook-and-how-to-integrate-it-with-spring-boot/) | Introduction and setup guide |
| [Medium - Quick Setup](https://medium.com/@pramodyahk/configure-request-response-logging-for-spring-boot-quickly-using-logbook-183503c4676c) | Quick configuration guide |
| [Medium - Zalando Logbook](https://medium.com/@sybrenbolandit/zalando-logbook-ee923c08e359) | Overview and use cases |
| [JavaCodeGeeks - WebFlux + ELK](https://www.javacodegeeks.com/2024/09/optimizing-spring-webflux-logging-with-zalando-logbook-and-elk.html) | WebFlux integration with ELK stack |

### Quick Reference

```yaml
# application.yml
logbook:
  filter.enabled: true
  format.style: json
  obfuscate:
    headers: [Authorization, X-API-Key]
    parameters: [password, token]
    json-body-fields: [password, token, ssn, creditCardNumber]
```

```kotlin
// Custom partial masking
@Bean
fun bodyFilter(): BodyFilter {
    return BodyFilter.merge(
        JsonPathBodyFilters.jsonPath("$.password").replace("***"),
        JsonPathBodyFilters.jsonPath("$.email").replace { "***@${it.substringAfter("@")}" }
    )
}
```

---

## Logstash Logback Encoder

Provides Logback encoders, layouts, and appenders to output logs in JSON format (for ELK stack, Datadog, Splunk, etc.).

### What It Does

- Outputs logs as structured JSON instead of plain text
- Supports StructuredArguments for adding custom fields to log events
- Allows custom JSON generation via JsonGeneratorDecorator
- Integrates with Logstash, Elasticsearch, and other log aggregation tools

### Official Documentation

| Resource | Description |
|----------|-------------|
| [GitHub Repository](https://github.com/logfellow/logstash-logback-encoder) | Main repository (moved to logfellow org) |
| [README.md](https://github.com/logfellow/logstash-logback-encoder/blob/main/README.md) | Comprehensive configuration guide |
| [Releases](https://github.com/logfellow/logstash-logback-encoder/releases) | Version history |
| [Maven Central](https://mvnrepository.com/artifact/net.logstash.logback/logstash-logback-encoder) | Dependency information |

### Key Sections in README

- **LogstashEncoder** - Main encoder for JSON output
- **Composite Encoders/Layouts** - Build custom JSON structures
- **Customizing JSON Factory/Generator** - JsonGeneratorDecorator for masking
- **Event-specific Custom Fields** - StructuredArguments and Markers
- **Async Appenders** - High-performance logging

### API Documentation

| Resource | Description |
|----------|-------------|
| [StructuredArguments Javadoc](https://javadoc.io/doc/net.logstash.logback/logstash-logback-encoder/latest/net/logstash/logback/argument/StructuredArguments.html) | API for structured logging |
| [StructuredArguments Source](https://github.com/logfellow/logstash-logback-encoder/blob/main/src/main/java/net/logstash/logback/argument/StructuredArguments.java) | Source code with examples |
| [Markers Source](https://github.com/logfellow/logstash-logback-encoder/blob/main/src/main/java/net/logstash/logback/marker/Markers.java) | Marker API source |

### Tutorials & Articles

| Resource | Description |
|----------|-------------|
| [INNOQ - Structured Logging](https://www.innoq.com/en/blog/2019/05/structured-logging/) | Deep dive into StructuredArguments |
| [Terse Logback - Structured Logging](https://tersesystems.github.io/terse-logback/1.0.0/structured-logging/) | Advanced patterns and best practices |
| [Bearded Developer - Benefits of Structured Logging](https://bearded-developer.com/posts/the-benefits-of-structured-logging/) | Why use structured logging |

### Quick Reference

```xml
<!-- logback-spring.xml -->
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <jsonGeneratorDecorator class="com.example.MaskingJsonGeneratorDecorator"/>
</encoder>
```

```kotlin
// StructuredArguments usage
import net.logstash.logback.argument.StructuredArguments.kv
import net.logstash.logback.argument.StructuredArguments.fields
import net.logstash.logback.marker.Markers

// Approach 1: kv() - each field is a separate JSON key
log.info("Login: {}, {}", kv("email", email), kv("password", password))

// Approach 2: fields() - all object fields become top-level JSON keys
log.info("Payment: {}", fields(paymentRequest))

// Approach 3: Markers.append() - object nested under a key
log.info(Markers.append("request", profileRequest), "Profile update")
```

---

## Logback

The successor to log4j, Logback is the logging framework used by Spring Boot.

### What It Does

- Core logging framework for Java applications
- Configurable via XML (logback.xml, logback-spring.xml)
- Supports appenders (console, file, rolling file, etc.)
- Extensible via custom layouts, encoders, and filters

### Official Documentation

| Resource | Description |
|----------|-------------|
| [Logback Home](https://logback.qos.ch/) | Official website |
| [Documentation Index](https://logback.qos.ch/documentation.html) | All documentation links |
| [Logback Manual](https://logback.qos.ch/manual/index.html) | Complete manual |
| [GitHub Repository](https://github.com/qos-ch/logback) | Source code |

### Manual Chapters

| Chapter | Description |
|---------|-------------|
| [Chapter 1: Introduction](https://logback.qos.ch/manual/introduction.html) | Overview and architecture |
| [Chapter 2: Architecture](https://logback.qos.ch/manual/architecture.html) | Logger, Appender, Layout |
| [Chapter 3: Configuration](https://logback.qos.ch/manual/configuration.html) | XML configuration guide |
| [Chapter 4: Appenders](https://logback.qos.ch/manual/appenders.html) | Console, File, Rolling appenders |
| [Chapter 5: Encoders](https://logback.qos.ch/manual/encoders.html) | Pattern encoding |
| [Chapter 6: Layouts](https://logback.qos.ch/manual/layouts.html) | Custom layouts (like MaskingPatternLayout) |
| [Chapter 7: Filters](https://logback.qos.ch/manual/filters.html) | Log filtering |

### Tutorials & Articles

| Resource | Description |
|----------|-------------|
| [Baeldung - Guide to Logback](https://www.baeldung.com/logback) | Comprehensive tutorial |
| [Better Stack - Java Logging with Logback](https://betterstack.com/community/guides/logging/java/logback/) | Modern best practices |

### Quick Reference

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="com.example.MaskingPatternLayout">
                <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </layout>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

```kotlin
// Custom PatternLayout for masking
class MaskingPatternLayout : PatternLayout() {
    override fun doLayout(event: ILoggingEvent): String {
        var message = super.doLayout(event)
        // Apply regex masking rules
        return message
    }
}
```

---

## Related Tutorials

### Spring Boot Logging

| Resource | Description |
|----------|-------------|
| [Spring Boot Logging Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging) | Official Spring Boot logging docs |
| [Baeldung - Spring Boot Logging](https://www.baeldung.com/spring-boot-logging) | Configuration and customization |

### ELK Stack Integration

| Resource | Description |
|----------|-------------|
| [Elastic - Filebeat](https://www.elastic.co/guide/en/beats/filebeat/current/index.html) | Filebeat documentation |
| [Elastic - Logstash](https://www.elastic.co/guide/en/logstash/current/index.html) | Logstash documentation |

### Security & Compliance

| Resource | Description |
|----------|-------------|
| [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html) | Security best practices |

---

## Version Compatibility

| Library | Version | Java | Spring Boot |
|---------|---------|------|-------------|
| Zalando Logbook | 3.9.0 | 17+ | 3.x |
| logstash-logback-encoder | 8.0 | 11+ | 2.x, 3.x |
| Logback | 1.5.x | 11+ | 3.x |

---

## Quick Start Checklist

1. Add dependencies to `build.gradle.kts`:
   ```kotlin
   implementation("org.zalando:logbook-spring-boot-starter:3.9.0")
   implementation("net.logstash.logback:logstash-logback-encoder:8.0")
   ```

2. Configure `application.yml` with Logbook obfuscation settings

3. Create `logback-spring.xml` with your preferred output format (text or JSON)

4. (Optional) Implement custom masking:
   - `MaskingPatternLayout` for regex-based text masking
   - `MaskingJsonGeneratorDecorator` for JSON field masking
   - `BodyFilter` bean for Logbook partial masking

5. Test with sample requests containing sensitive data
