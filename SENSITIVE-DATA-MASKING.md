# Sensitive Data Masking - Usage Guide

## Overview

This project implements **defense-in-depth** sensitive data masking using three complementary layers:

| Layer | What it masks | How |
|---|---|---|
| **Zalando Logbook** | HTTP request/response bodies, headers, query params | Intercepts HTTP traffic before logging |
| **MaskingJsonGeneratorDecorator** | Structured log fields (`kv()`, `fields()`, `Markers.append()`) | Intercepts Jackson JSON serialization |
| **MaskingPatternLayout** | All text log output | Regex-based safety net on final log string |

All three layers are always active. No Spring profiles needed.

## Dependencies

```kotlin
// build.gradle.kts
implementation("org.zalando:logbook-spring-boot-starter:3.9.0")
implementation("net.logstash.logback:logstash-logback-encoder:8.0")
```

## Architecture

```
HTTP Request
    |
    v
[Logbook Interceptor]  -->  logs masked request body/headers at INFO level
    |
    v
[Controller]
    |--- log.info("...", kv("password", value))       --> MaskingJsonGeneratorDecorator masks field
    |--- log.info("...", fields(requestObject))        --> MaskingJsonGeneratorDecorator masks fields
    |--- log.info(Markers.append("req", obj), "...")   --> MaskingJsonGeneratorDecorator masks fields
    |
    v
[MaskingPatternLayout]  -->  regex safety net on all text output
    |
    v
Console Output (human-readable, all sensitive data masked)
```

---

## Configuration

### application.yml

Logbook field-level obfuscation (replaces values with `XXX`):

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
      - refreshToken
      - apiKey
      - secret
      - creditCardNumber
      - cardNumber
      - cvv
      - ssn
      - nationalId
      - myCustomeToken1
      - mycustomtoken2
```

To add a new field to Logbook obfuscation, append it to the relevant list.
Fields in `json-body-fields` are replaced with `XXX` in HTTP request/response logs.

### logback-spring.xml

```xml
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="com.kss.proj.springmvccoroutines.logging.MaskingPatternLayout">
                <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </layout>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

No special logger configuration is needed for Logbook because a custom `HttpLogWriter` bean logs at INFO level.

---

## Masking Components

### 1. LogbookConfiguration

**File:** `logging/LogbookConfiguration.kt`

Two beans:

**`bodyFilter()`** - Custom `BodyFilter` with partial masking (instead of Logbook's full `XXX` replacement):

| Field | Masking | Example |
|---|---|---|
| `password`, `token`, `apiKey`, `secret`, `ssn`, `nationalId`, `cvv` | Full redaction | `***REDACTED***` |
| `creditCardNumber`, `cardNumber` | First 4 + last 4 | `4111-****-****-1111` |
| `email` | Domain preserved | `***@example.com` |
| `phone` | Last 4 digits | `***7890` |

**`writer()`** - Custom `HttpLogWriter` that logs at **INFO** level instead of the default TRACE:

```kotlin
@Bean
fun writer(): HttpLogWriter {
    val logger = LoggerFactory.getLogger("org.zalando.logbook.Logbook")
    return object : HttpLogWriter {
        override fun isActive() = logger.isInfoEnabled
        override fun write(precorrelation: Precorrelation, request: String) = logger.info(request)
        override fun write(correlation: Correlation, response: String) = logger.info(response)
    }
}
```

> **Note:** In Logbook 3.x, `DefaultHttpLogWriter` has no constructor to change the log level. This custom writer replaces it.

### 2. MaskingPatternLayout

**File:** `logging/MaskingPatternLayout.kt`

Regex-based safety net applied to **all** text log output. Catches anything the other layers might miss.

| Pattern | Example input | Masked output |
|---|---|---|
| Tokens/API keys/secrets | `"token": "abc123"` | `"token": "***REDACTED***"` |
| Passwords | `"password": "secret"` | `"password": "***REDACTED***"` |
| Credit card numbers | `4111 1111 1111 1111` | `4111-****-****-1111` |
| Email addresses | `john@example.com` | `***@example.com` |
| JWT tokens | `eyJhbG...` (full JWT) | `***JWT_REDACTED***` |
| Bearer tokens | `Bearer eyJhbG...` | `Bearer ***REDACTED***` |

### 3. MaskingJsonGeneratorDecorator

**File:** `logging/MaskingJsonGeneratorDecorator.kt`

Intercepts Jackson JSON serialization during structured logging. Masks fields by name when using `StructuredArguments.kv()`, `StructuredArguments.fields()`, or `Markers.append()`.

Sensitive fields (full redaction): `password`, `token`, `apikey`, `api_key`, `secret`, `authorization`, `credential`, `bearer`, `accesstoken`, `access_token`, `refreshtoken`, `refresh_token`, `cvv`, `creditcardnumber`, `credit_card_number`, `ssn`, `nationalid`

Partial mask fields: `email`, `phone`, `creditcard`

> **Note:** This decorator is only active when `logback-spring.xml` uses a `LogstashEncoder` with `<jsonGeneratorDecorator>`. In the current text-output configuration it is not active, but is available if you switch to JSON output (see [Switching to JSON Output](#switching-to-json-output)).

---

## Demo Controllers

### LogbookDemoController (`/api/logbook/*`)

Controller code does **no masking**. Logbook handles it automatically at the HTTP layer.

| Endpoint | What gets masked |
|---|---|
| `POST /api/logbook/login` | Request: `password`, `email`. Response: `token`, `refreshToken` |
| `POST /api/logbook/payment` | Request: `creditCardNumber`, `cvv`, `email` |
| `POST /api/logbook/profile` | Request: `ssn`, `apiKey`, `email`, `phone` |
| `POST /api/logbook/custom-tokens` | Request: `myCustomeToken1`, `mycustomtoken2` |
| `GET /api/logbook/protected` | Headers: `Authorization`, `X-API-Key` |

### LogbackDemoController (`/api/logback/*`)

Demonstrates three structured logging approaches (for use with JSON output + `MaskingJsonGeneratorDecorator`):

| Endpoint | Approach | Usage |
|---|---|---|
| `POST /api/logback/login` | `kv()` per field | `log.info("Login: {}, {}", kv("email", email), kv("password", pw))` |
| `POST /api/logback/payment` | `fields(object)` | `log.info("Payment: {}", fields(request))` |
| `POST /api/logback/profile` | `Markers.append()` | `log.info(Markers.append("request", obj), "Profile update")` |
| `GET /api/logback/protected` | `kv()` | `log.info("Access: {}", kv("authorization", header))` |

---

## Running and Testing

### Start the application

```bash
./gradlew bootRun
```

The app starts on port **9999**.

### Test with IntelliJ HTTP Client

Open the files in `http/` directory and run requests using the `dev` environment:

- `http/logbook-demo.http` - tests Logbook HTTP-level masking
- `http/logback-demo.http` - tests structured logging masking

Environment file: `http/http-client.env.json` (`host: localhost:9999`)

### Test with curl

**Login** (password + email masked in log, token + refreshToken masked in response log):

```bash
curl -s -X POST http://localhost:9999/api/logbook/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john.doe","password":"SuperSecret123","email":"john.doe@example.com"}'
```

Expected log output:

```
INFO  org.zalando.logbook.Logbook - {"origin":"remote","type":"request",...,
  "body":{"username":"john.doe","password":"***REDACTED***","email":"***@example.com"}}

INFO  org.zalando.logbook.Logbook - {"origin":"local","type":"response",...,
  "body":{"token":"***REDACTED***","refreshToken":"***REDACTED***","expiresIn":3600}}
```

**Payment** (credit card partially masked, cvv + email masked):

```bash
curl -s -X POST http://localhost:9999/api/logbook/payment \
  -H "Content-Type: application/json" \
  -d '{"amount":99.99,"currency":"USD","creditCardNumber":"4111111111111111","cvv":"123","cardholderName":"John Doe","email":"john.doe@example.com"}'
```

**Profile** (ssn + apiKey fully masked, email + phone partially masked):

```bash
curl -s -X POST http://localhost:9999/api/logbook/profile \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe","email":"jane.doe@example.com","phone":"+1234567890","ssn":"123-45-6789","apiKey":"sk-abc123secret456"}'
```

**Custom tokens** (custom field names masked):

```bash
curl -s -X POST http://localhost:9999/api/logbook/custom-tokens \
  -H "Content-Type: application/json" \
  -d '{"service":"payment-gateway","myCustomeToken1":"abc-secret-token-value-123","mycustomtoken2":"xyz-another-secret-456","payload":"some-non-sensitive-data"}'
```

**Protected endpoint** (Authorization header masked):

```bash
curl -s http://localhost:9999/api/logbook/protected \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.abc123"
```

**Protected endpoint** (X-API-Key header masked):

```bash
curl -s http://localhost:9999/api/logbook/protected \
  -H "X-API-Key: my-super-secret-api-key-12345"
```

---

## Adding New Sensitive Fields

### To mask a new field in HTTP request/response bodies

1. Add the field name to `logbook.obfuscate.json-body-fields` in `application.yml`
2. (Optional) Add a custom `JsonPathBodyFilter` in `LogbookConfiguration.bodyFilter()` for partial masking instead of full `XXX` replacement

### To mask a new field in structured log statements

1. Add the lowercase field name to `sensitiveFields` in `MaskingJsonGeneratorDecorator`
2. Or add it to `partialMaskFields` with a masking lambda

### To mask a new pattern in text log output

1. Add a new `MaskingRule` in `MaskingPatternLayout.maskingRules`

---

## Switching to JSON Output

To use structured JSON output (useful for log aggregation tools like ELK, Datadog, Splunk):

Replace the `CONSOLE` appender in `logback-spring.xml`:

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <jsonGeneratorDecorator
            class="com.kss.proj.springmvccoroutines.logging.MaskingJsonGeneratorDecorator"/>
    </encoder>
</appender>
```

This activates the `MaskingJsonGeneratorDecorator` for field-level masking in JSON structured logs. When using JSON output, all three logging approaches (`kv()`, `fields()`, `Markers.append()`) in `LogbackDemoController` will have their sensitive fields masked automatically.

---

## Key Files

| File | Purpose |
|---|---|
| `logging/LogbookConfiguration.kt` | Custom BodyFilter (partial masking) + HttpLogWriter (INFO level) |
| `logging/MaskingPatternLayout.kt` | Regex-based text masking safety net |
| `logging/MaskingJsonGeneratorDecorator.kt` | Jackson field-level masking for structured JSON logs |
| `example/LogbookDemoController.kt` | Demo: automatic HTTP masking (no controller code needed) |
| `example/LogbackDemoController.kt` | Demo: structured logging with `kv()`, `fields()`, `Markers.append()` |
| `example/DemoContracts.kt` | Shared DTOs (LoginRequest, PaymentRequest, etc.) |
| `resources/application.yml` | Logbook obfuscation config |
| `resources/logback-spring.xml` | Logback appender + layout config |
| `http/logbook-demo.http` | IntelliJ HTTP requests for Logbook demo |
| `http/logback-demo.http` | IntelliJ HTTP requests for Logback demo |
| `http/http-client.env.json` | Environment variables (`host: localhost:9999`) |
| `logging/HttpLoggingFilter.kt` | Pure Logback HTTP filter (alternative to Logbook) |
| `resources/application-logback-only.yml` | Profile config for pure Logback mode |

---

## Alternative: Pure Logback (No Logbook)

If you prefer not to use Logbook, you can use the built-in `HttpLoggingFilter` which captures HTTP request/response using Spring's `ContentCachingRequestWrapper` and `ContentCachingResponseWrapper`.

### Enable Pure Logback Mode

```bash
./gradlew bootRun --args='--spring.profiles.active=logback-only'
```

Or set in `application.yml`:

```yaml
logbook:
  filter:
    enabled: false
```

### How It Works

| Component | Role |
|---|---|
| `HttpLoggingFilter` | Captures HTTP request/response, logs to `http.logging` category |
| `MaskingPatternLayout` | Applies regex masking to all log output (including HTTP logs) |

### Log Output Format

```
>>> REQUEST: POST /api/logbook/login | Headers: {..., authorization="***REDACTED***"} | Body: {"username":"john.doe","password":"***REDACTED***","email":"***@example.com"}
<<< RESPONSE: 200 | Duration: 190ms | Headers: {} | Body: {"token":"***REDACTED***","refreshToken":"***REDACTED***","expiresIn":3600}
```

### Features

- Masks sensitive headers: `Authorization`, `X-API-Key`, `X-Auth-Token`, `Cookie`, `Set-Cookie`
- Skips `/actuator`, `/health`, `/favicon.ico` paths
- Limits body logging to 10,000 characters
- All body masking handled by `MaskingPatternLayout` regex rules

### When to Use

| Use Logbook | Use Pure Logback |
|---|---|
| Need structured JSON HTTP logs | Prefer simple text HTTP logs |
| Want field-level config in YAML | Want regex-based masking only |
| Need partial masking (first/last 4 chars) | Full redaction is acceptable |
| Complex obfuscation rules | Simple masking requirements |

### HttpLoggingFilter Source

**File:** `logging/HttpLoggingFilter.kt`

```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = ["logbook.filter.enabled"], havingValue = "false")
class HttpLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger("http.logging")

    private val sensitiveHeaders = setOf(
        "authorization", "x-api-key", "x-auth-token", "cookie", "set-cookie"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
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
    // ... logging methods
}
```
