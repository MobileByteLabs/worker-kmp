package io.github.mobilebytelabs.worker

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForegroundApi::class)
class ForegroundWorkerTest {

    @Test
    fun foregroundInfo_defaults_areSane() {
        val info = ForegroundInfo(notificationId = 1, title = "Test", message = "Working")
        assertEquals(0, info.progress.progress)
        assertNull(info.serviceType)
        assertNull(info.cancelAction)
    }

    @Test
    fun foregroundInfo_withServiceType_carriesField() {
        val info = ForegroundInfo(
            notificationId = 1,
            title = "Sync",
            message = "Working",
            serviceType = ForegroundServiceType.DATA_SYNC,
        )
        assertEquals(ForegroundServiceType.DATA_SYNC, info.serviceType)
    }

    @Test
    fun foregroundServiceType_has13Variants() {
        // Mirror Android 14's full set: dataSync, mediaPlayback, mediaProjection,
        // connectedDevice, phoneCall, camera, microphone, location, health,
        // remoteMessaging, shortService, specialUse, systemExempted
        assertEquals(13, ForegroundServiceType.entries.size)
    }

    @Test
    fun foregroundWorker_setForeground_doesNotThrow() = runTest {
        val context = TestWorkerContext()
        val worker = object : ForegroundWorker(context) {
            override suspend fun doWork(): WorkResult {
                setForeground(ForegroundInfo(1, "T", "M"))
                return WorkResult.success()
            }
        }
        // Stub runAsForeground logs only — no exception
        val result = worker.doWork()
        assertTrue(result is WorkResult.Success)
    }
}
