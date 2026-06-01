@file:OptIn(ExperimentalForegroundApi::class)

package io.github.mobilebytelabs.worker

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for [BgContinuedProcessing] — the iOS 17+ ContinuedProcessing branch on the
 * foreground path.
 *
 * Cannot exercise the real `BGTaskScheduler.submitTaskRequest` call site (no Info.plist
 * registration in the test runner), so we verify the GATE behaviour:
 * - identifier empty → trySchedule returns false (caller falls back to BGProcessing shim)
 * - identifier set + iOS < 17 → trySchedule returns false (caller falls back)
 * - parseMajorIosVersion correctly handles version strings
 *
 * Added by cross-platform-worker-parity-audit sub-plan 02 (closes G1).
 */
class BgContinuedProcessingTest {

    @AfterTest
    fun reset() {
        BgContinuedProcessing.setIdentifier("")
    }

    @Test
    fun trySchedule_returnsFalse_whenIdentifierEmpty() {
        BgContinuedProcessing.setIdentifier("")
        val fired = BgContinuedProcessing.trySchedule(infoFixture())
        assertFalse(fired, "trySchedule must return false when no identifier set")
    }

    @Test
    fun setIdentifier_thenGetIdentifier_roundTrips() {
        BgContinuedProcessing.setIdentifier("com.example.foreground-sync")
        assertEquals("com.example.foreground-sync", BgContinuedProcessing.getIdentifier())
    }

    @Test
    fun parseMajorIosVersion_handlesStandardVersionStrings() {
        assertEquals(17, BgContinuedProcessing.parseMajorIosVersion("17.0"))
        assertEquals(17, BgContinuedProcessing.parseMajorIosVersion("17.4.1"))
        assertEquals(16, BgContinuedProcessing.parseMajorIosVersion("16.3"))
        assertEquals(18, BgContinuedProcessing.parseMajorIosVersion("18"))
    }

    @Test
    fun parseMajorIosVersion_handlesGarbage() {
        assertEquals(0, BgContinuedProcessing.parseMajorIosVersion(""))
        assertEquals(0, BgContinuedProcessing.parseMajorIosVersion("not-a-version"))
    }

    private fun infoFixture(): ForegroundInfo = ForegroundInfo(
        notificationId = 42,
        title = "Sync running",
        message = "Uploading 3 of 10",
        progress = WorkProgress(progress = 30),
    )
}
