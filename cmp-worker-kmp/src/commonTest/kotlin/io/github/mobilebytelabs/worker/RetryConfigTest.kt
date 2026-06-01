package io.github.mobilebytelabs.worker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Coverage tests for [RetryConfig] and [BackoffPolicy]. Companions DEFAULT / AGGRESSIVE
 * are partially exercised by [WorkManagerTest]; this file completes the surface (CONSERVATIVE,
 * custom constructor, copy(), every BackoffPolicy entry, data-class equals).
 */
class RetryConfigTest {

    @Test
    fun default_companion_hasDocumentedDefaults() {
        val c = RetryConfig.DEFAULT
        assertEquals(3, c.maxAttempts)
        assertEquals(1.minutes, c.initialDelay)
        assertEquals(1.hours, c.maxDelay)
        assertEquals(BackoffPolicy.EXPONENTIAL, c.backoffPolicy)
        assertEquals(2.0, c.multiplier)
    }

    @Test
    fun aggressive_companion_hasDocumentedDefaults() {
        val c = RetryConfig.AGGRESSIVE
        assertEquals(10, c.maxAttempts)
        assertEquals(30.seconds, c.initialDelay)
        assertTrue(c.maxAttempts > RetryConfig.DEFAULT.maxAttempts)
    }

    @Test
    fun conservative_companion_hasDocumentedDefaults() {
        val c = RetryConfig.CONSERVATIVE
        assertEquals(2, c.maxAttempts)
        assertEquals(5.minutes, c.initialDelay)
        assertTrue(c.maxAttempts < RetryConfig.DEFAULT.maxAttempts)
    }

    @Test
    fun constructor_acceptsAllCustomValues() {
        val c = RetryConfig(
            maxAttempts = 5,
            initialDelay = 10.seconds,
            maxDelay = 10.minutes,
            backoffPolicy = BackoffPolicy.LINEAR,
            multiplier = 1.5,
        )
        assertEquals(5, c.maxAttempts)
        assertEquals(10.seconds, c.initialDelay)
        assertEquals(10.minutes, c.maxDelay)
        assertEquals(BackoffPolicy.LINEAR, c.backoffPolicy)
        assertEquals(1.5, c.multiplier)
    }

    @Test
    fun constructor_defaultsMatchDefaultCompanion() {
        assertEquals(RetryConfig.DEFAULT, RetryConfig())
    }

    @Test
    fun copy_overridesSingleField() {
        val base = RetryConfig.DEFAULT
        val copied = base.copy(maxAttempts = 7)
        assertEquals(7, copied.maxAttempts)
        assertEquals(base.initialDelay, copied.initialDelay)
        assertEquals(base.backoffPolicy, copied.backoffPolicy)
        assertNotEquals(base, copied)
    }

    @Test
    fun dataClass_equalsAndHashCode() {
        val a = RetryConfig(maxAttempts = 4, initialDelay = 2.seconds)
        val b = RetryConfig(maxAttempts = 4, initialDelay = 2.seconds)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun dataClass_toString_containsClassNameAndFields() {
        val s = RetryConfig.DEFAULT.toString()
        assertTrue("RetryConfig" in s)
        assertTrue("maxAttempts" in s)
    }

    @Test
    fun dataClass_componentN_returnsFields() {
        val c = RetryConfig(
            maxAttempts = 6,
            initialDelay = 4.seconds,
            maxDelay = 8.minutes,
            backoffPolicy = BackoffPolicy.LINEAR,
            multiplier = 3.0,
        )
        assertEquals(6, c.component1())
        assertEquals(4.seconds, c.component2())
        assertEquals(8.minutes, c.component3())
        assertEquals(BackoffPolicy.LINEAR, c.component4())
        assertEquals(3.0, c.component5())
    }

    // --- BackoffPolicy enum ----------------------------------------------

    @Test
    fun backoffPolicy_entries_hasTwoValues() {
        assertEquals(2, BackoffPolicy.entries.size)
        assertTrue(BackoffPolicy.EXPONENTIAL in BackoffPolicy.entries)
        assertTrue(BackoffPolicy.LINEAR in BackoffPolicy.entries)
    }

    @Test
    fun backoffPolicy_valueOf_roundTrip() {
        for (entry in BackoffPolicy.entries) {
            assertEquals(entry, BackoffPolicy.valueOf(entry.name))
        }
    }
}
