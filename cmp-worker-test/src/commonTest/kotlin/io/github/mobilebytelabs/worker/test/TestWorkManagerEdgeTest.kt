package io.github.mobilebytelabs.worker.test

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.ExistingWorkPolicy
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

/**
 * Edge-case coverage for [TestWorkManager] — exercises the false branch of the
 * `REPLACE` conditional in `enqueueUniquePeriodicWork` (i.e. KEEP / UPDATE)
 * and the `cancelAllWorkByTag` no-op-on-finished branch.
 */
class TestWorkManagerEdgeTest {

    private fun periodicRequest() =
        PeriodicWorkRequestBuilder<FakeEdgeWorker>("WorkerE", repeatInterval = 1.hours).build()

    @Test
    fun enqueueUniquePeriodicWork_keepPolicy_doesNotCancelPrevious() = runTest {
        val wm = TestWorkManager()
        val first = periodicRequest()
        wm.enqueueUniquePeriodicWork("daily", ExistingPeriodicWorkPolicy.KEEP, first)
        val second = periodicRequest()
        wm.enqueueUniquePeriodicWork("daily", ExistingPeriodicWorkPolicy.KEEP, second)
        // Both stay ENQUEUED — KEEP path doesn't cancel.
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(first.id)?.state)
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(second.id)?.state)
    }

    @Test
    fun cancelAllWorkByTag_noopOnFinishedWork() = runTest {
        val wm = TestWorkManager()
        val id = wm.enqueue(
            io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder<FakeEdgeWorker>("W")
                .addTag("done")
                .build(),
        )
        wm.simulateWorkSuccess(id)
        wm.cancelAllWorkByTag("done")
        // Already terminal — stays SUCCEEDED.
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueueUniqueWork_replacePolicy_cancelsExisting() = runTest {
        val wm = TestWorkManager()
        val first = io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder<FakeEdgeWorker>("W").build()
        wm.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, first)
        val second = io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder<FakeEdgeWorker>("W").build()
        wm.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, second)
        // REPLACE path cancels the first one before enqueuing the second.
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(first.id)?.state)
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(second.id)?.state)
    }

    @Test
    fun enqueueUniqueWork_keepPolicy_doesNotCancelExisting() = runTest {
        val wm = TestWorkManager()
        val first = io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder<FakeEdgeWorker>("W").build()
        wm.enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, first)
        val second = io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder<FakeEdgeWorker>("W").build()
        wm.enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, second)
        // KEEP path leaves both ENQUEUED.
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(first.id)?.state)
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(second.id)?.state)
    }

    @Test
    fun update_onUnknownId_isSilent() = runTest {
        val wm = TestWorkManager()
        // Drive an internal `update(id) { ... }` against an unknown id by
        // invoking a simulate* helper with a random id — exercises the
        // `store.value[id] ?: return` early-out branch.
        wm.simulateWorkSuccess(kotlin.uuid.Uuid.random())
        assertNull(wm.getWorkInfoById(kotlin.uuid.Uuid.random()))
    }
}

private class FakeEdgeWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}
