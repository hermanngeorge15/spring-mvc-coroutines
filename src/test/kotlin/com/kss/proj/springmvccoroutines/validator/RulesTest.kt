package com.kss.proj.springmvccoroutines.validator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Validation Rules Tests")
class RulesTest {

    data class TestData(val name: String?, val value: String?, val count: Int?)

    @Nested
    @DisplayName("notBlank rule")
    inner class NotBlankRuleTest {

        @Test
        @DisplayName("should pass when field is not blank")
        fun testValidNotBlank() {
            val rule = notBlank<TestData>("name") { it.name }
            val data = TestData(name = "John Doe", value = null, count = null)
            val errors = rule.check(data)
            assertTrue(errors.isEmpty())
        }

        @Test
        @DisplayName("should fail when field is null")
        fun testNullValue() {
            val rule = notBlank<TestData>("name") { it.name }
            val data = TestData(name = null, value = null, count = null)
            val errors = rule.check(data)
            assertEquals(1, errors.size)
            assertEquals("name", errors[0].field)
            assertEquals("REQUIRED", errors[0].code)
        }

        @Test
        @DisplayName("should fail when field is blank")
        fun testBlankValue() {
            val rule = notBlank<TestData>("name") { it.name }
            val data = TestData(name = "   ", value = null, count = null)
            val errors = rule.check(data)
            assertEquals(1, errors.size)
            assertEquals("BLANK", errors[0].code)
        }
    }

    @Nested
    @DisplayName("notNull rule")
    inner class NotNullRuleTest {

        @Test
        @DisplayName("should pass when field is not null")
        fun testValidNotNull() {
            val rule = notNull<TestData>("count") { it.count }
            val data = TestData(name = null, value = null, count = 42)
            val errors = rule.check(data)
            assertTrue(errors.isEmpty())
        }

        @Test
        @DisplayName("should fail when field is null")
        fun testNullValue() {
            val rule = notNull<TestData>("count") { it.count }
            val data = TestData(name = null, value = null, count = null)
            val errors = rule.check(data)
            assertEquals(1, errors.size)
            assertEquals("REQUIRED", errors[0].code)
        }
    }

    @Nested
    @DisplayName("matchesValue rule")
    inner class MatchesValueRuleTest {

        @Test
        @DisplayName("should pass when values match")
        fun testMatchingValue() {
            val rule = matchesValue<TestData>("value", "expected") { it.value }
            val data = TestData(name = null, value = "expected", count = null)
            val errors = rule.check(data)
            assertTrue(errors.isEmpty())
        }

        @Test
        @DisplayName("should fail when values don't match")
        fun testNonMatchingValue() {
            val rule = matchesValue<TestData>("value", "expected") { it.value }
            val data = TestData(name = null, value = "actual", count = null)
            val errors = rule.check(data)
            assertEquals(1, errors.size)
            assertEquals("INVALID_VALUE", errors[0].code)
        }
    }
}

