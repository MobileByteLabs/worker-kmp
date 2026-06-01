package io.github.mobilebytelabs.worker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [ForegroundNotSupportedException]'s public surface — single constructor,
 * 2 public read-only properties, RuntimeException supertype + formatted `message`.
 * Phase 8 closes the `0/3 covered` gap surfaced in the aggregate Kover report.
 */
@OptIn(ExperimentalForegroundApi::class)
class ForegroundNotSupportedExceptionTest {

    @Test
    fun constructor_setsProperties() {
        val e = ForegroundNotSupportedException(platform = "ios", reason = "BGTask only")
        assertEquals("ios", e.platform)
        assertEquals("BGTask only", e.reason)
    }

    @Test
    fun message_includesPlatform_andReason() {
        val e = ForegroundNotSupportedException("desktop", "no system tray")
        val m = e.message ?: ""
        assertTrue("desktop" in m)
        assertTrue("no system tray" in m)
    }

    @Test
    fun isRuntimeException_subtype() {
        val e: Throwable = ForegroundNotSupportedException("web", "service workers don't grant fg")
        assertTrue(e is RuntimeException)
    }
}
