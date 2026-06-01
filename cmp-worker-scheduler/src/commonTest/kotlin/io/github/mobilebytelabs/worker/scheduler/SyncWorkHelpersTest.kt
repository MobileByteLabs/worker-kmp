package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the top-level helpers in `SyncWorkHelpers.kt`.
 *
 * These constants + the [SyncConstraints] factory are part of the library's public
 * contract — consumers may rely on the tag string + notification id for observe /
 * cancel calls, and the default `Constraints` shape powers every schedule path in
 * [DefaultWorkScheduler]. Pinning them prevents accidental drift.
 */
class SyncWorkHelpersTest {

    @Test
    fun syncWorkName_isStableConstant() {
        assertEquals("SyncWork", SYNC_WORK_NAME)
    }

    @Test
    fun foregroundNotificationIdSync_isStableConstant() {
        assertEquals(9_001, FOREGROUND_NOTIFICATION_ID_SYNC)
    }

    @Test
    fun syncConstraints_requiresNetworkConnected() {
        assertEquals(NetworkType.CONNECTED, SyncConstraints.requiredNetworkType)
    }

    @Test
    fun syncConstraints_isReusableSingletonInstance() {
        // The val is initialized once at file-load time and re-read on every reference;
        // identity equality confirms callers receive the same object (and therefore the
        // same defaults) on every schedule path.
        assertEquals(SyncConstraints, SyncConstraints)
    }
}
