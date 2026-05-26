package io.github.mobilebytelabs.worker.test

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid

class TestWorkManagerTest {

    private fun wm() = TestWorkManager()

    @Test
    fun enqueue_recordsRequest() = runTest {
        val wm = wm()
        val req = request("WorkerA")
        wm.enqueue(req)
        assertEquals(1, wm.enqueuedRequests.size)
        assertEquals(req, wm.enqueuedRequests.first())
    }

    @Test
    fun enqueue_setsInitialStateEnqueued() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_returnsRequestId() = runTest {
        val wm = wm()
        val req = request("WorkerA")
        assertEquals(req.id, wm.enqueue(req))
    }

    @Test
    fun getWorkInfoById_returnsNullForUnknown() = runTest {
        assertNull(wm().getWorkInfoById(Uuid.random()))
    }

    @Test
    fun simulateWorkRunning_setsRunningState() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.simulateWorkRunning(id)
        assertEquals(WorkInfo.State.RUNNING, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun simulateWorkSuccess_setsSucceededWithOutput() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.simulateWorkSuccess(id, workDataOf("result" to "done"))
        val info = wm.getWorkInfoById(id)
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
        assertEquals("done", info?.outputData?.getString("result"))
    }

    @Test
    fun simulateWorkFailure_setsFailedState() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.simulateWorkFailure(id)
        assertEquals(WorkInfo.State.FAILED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun simulateWorkProgress_updatesProgress() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.simulateWorkProgress(id, WorkProgress(75))
        assertEquals(75, wm.getWorkInfoById(id)?.progress?.progress)
    }

    @Test
    fun simulateWorkRetry_incrementsAttemptCount() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.simulateWorkRetry(id)
        wm.simulateWorkRetry(id)
        assertEquals(2, wm.getWorkInfoById(id)?.runAttemptCount)
    }

    @Test
    fun cancelWorkById_setsCancelledState() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.cancelWorkById(id)
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun cancelWorkById_noopIfAlreadyFinished() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.simulateWorkSuccess(id)
        wm.cancelWorkById(id)
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun cancelAllWorkByTag_cancelsMatchingWork() = runTest {
        val wm = wm()
        val tag = "sync"
        val id1 = wm.enqueue(request("WorkerA", tag))
        val id2 = wm.enqueue(request("WorkerB", tag))
        val id3 = wm.enqueue(request("WorkerC"))
        wm.cancelAllWorkByTag(tag)
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(id1)?.state)
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(id2)?.state)
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(id3)?.state)
    }

    @Test
    fun getWorkInfosByTag_flowEmitsMatchingItems() = runTest {
        val wm = wm()
        val tag = "upload"
        val req = request("WorkerA", tag)
        wm.enqueue(req)
        val items = wm.getWorkInfosByTag(tag).first()
        assertTrue(items.any { it.id == req.id })
    }

    @Test
    fun getWorkInfosByTag_flowUpdatesOnStateChange() = runTest {
        val wm = wm()
        val tag = "upload"
        val id = wm.enqueue(request("WorkerA", tag))
        wm.simulateWorkSuccess(id)
        val items = wm.getWorkInfosByTag(tag).first()
        assertEquals(WorkInfo.State.SUCCEEDED, items.first { it.id == id }.state)
    }

    @Test
    fun enqueueUniquePeriodicWork_recordsNameAndRequest() = runTest {
        val wm = wm()
        val req = periodicRequest("WorkerA")
        wm.enqueueUniquePeriodicWork("cleanup", ExistingPeriodicWorkPolicy.REPLACE, req)
        assertEquals(1, wm.enqueuedRequests.size)
        assertTrue(wm.uniqueWorkNames.contains("cleanup"))
    }

    @Test
    fun enqueueUniquePeriodicWork_replace_cancelsPrevious() = runTest {
        val wm = wm()
        val req1 = periodicRequest("WorkerA")
        val req2 = periodicRequest("WorkerA")
        wm.enqueueUniquePeriodicWork("cleanup", ExistingPeriodicWorkPolicy.REPLACE, req1)
        wm.enqueueUniquePeriodicWork("cleanup", ExistingPeriodicWorkPolicy.REPLACE, req2)
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(req1.id)?.state)
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(req2.id)?.state)
    }

    @Test
    fun hasWorkWithTag_returnsTrueWhenPresent() = runTest {
        val wm = wm()
        wm.enqueue(request("WorkerA", "important"))
        assertTrue(wm.hasWorkWithTag("important"))
        assertFalse(wm.hasWorkWithTag("other"))
    }

    @Test
    fun workCountWithTag_returnsCorrectCount() = runTest {
        val wm = wm()
        wm.enqueue(request("WorkerA", "sync"))
        wm.enqueue(request("WorkerB", "sync"))
        wm.enqueue(request("WorkerC"))
        assertEquals(2, wm.workCountWithTag("sync"))
    }

    @Test
    fun lastEnqueuedRequest_returnsLastRequest() = runTest {
        val wm = wm()
        val req1 = request("WorkerA")
        val req2 = request("WorkerB")
        wm.enqueue(req1)
        wm.enqueue(req2)
        assertEquals(req2, wm.lastEnqueuedRequest)
    }

    @Test
    fun reset_clearsAllState() = runTest {
        val wm = wm()
        val id = wm.enqueue(request("WorkerA"))
        wm.simulateWorkSuccess(id)
        wm.reset()
        assertTrue(wm.enqueuedRequests.isEmpty())
        assertNull(wm.getWorkInfoById(id))
        assertFalse(wm.hasWorkWithTag("any"))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun request(workerClass: String, tag: String? = null) =
        OneTimeWorkRequestBuilder<FakeWorker>(workerClass)
            .also { if (tag != null) it.addTag(tag) }
            .build()

    private fun periodicRequest(workerClass: String) =
        PeriodicWorkRequestBuilder<FakeWorker>(workerClass, repeatInterval = 1.hours).build()
}

private class FakeWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork() = WorkResult.success()
}
