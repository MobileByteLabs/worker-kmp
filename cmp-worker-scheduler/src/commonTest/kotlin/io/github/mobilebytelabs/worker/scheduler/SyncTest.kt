package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the [Sync.initialize] convenience entrypoint — verifies it dispatches a
 * [WorkMode.Background] one-time sync of the typed [W] worker via the injected
 * [WorkScheduler] / [WorkManager] chain.
 */
class SyncTest {

    @Test
    fun initialize_enqueuesBackgroundDataSync_ofTypedWorker() = runTest {
        val wm = FakeWorkManager()
        val scheduler = DefaultWorkScheduler(
            workManager = wm,
            persister = SyncStatePersister(),
            scope = CoroutineScope(UnconfinedTestDispatcher()),
        )

        Sync.initialize<FakeSyncWorker>(scheduler)

        assertEquals(1, wm.enqueuedRequests.size)
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        // Reified worker class name flows through to the OneTimeWorkRequest
        assertEquals("FakeSyncWorker", request.workerClass)
        // Default background mode → no expedited policy set
        assertEquals(null, request.expeditedPolicy)
        // Sync.initialize always tags with SYNC_WORK_NAME
        assertTrue(SYNC_WORK_NAME in request.tags)
    }

    @Test
    fun initialize_dispatchesViaSchedulerFacade_notRawWorkManager() = runTest {
        // Indirect-call assertion: the scheduler is the only writer to FakeWorkManager.
        // If Sync.initialize ever bypassed the scheduler and called wm.enqueue() directly,
        // the request tags / class name wiring would differ. This test pins the
        // "Sync.initialize is a thin wrapper over enqueueDataSync<W>(mode=Background)" contract.
        val wm = FakeWorkManager()
        val scheduler = DefaultWorkScheduler(
            workManager = wm,
            persister = SyncStatePersister(),
            scope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        Sync.initialize<FakeAnalyticsWorker>(scheduler)
        val request = wm.enqueuedRequests.single() as OneTimeWorkRequest
        assertEquals("FakeAnalyticsWorker", request.workerClass)
    }
}
