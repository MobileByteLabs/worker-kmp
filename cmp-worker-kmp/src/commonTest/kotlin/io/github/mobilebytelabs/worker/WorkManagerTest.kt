package io.github.mobilebytelabs.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class WorkManagerTest {

    private val workManager = TestWorkManager()

    // --- Enqueue ---

    @Test
    fun enqueue_returnsRequestId() = runTest {
        val request = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build()
        val id = workManager.enqueue(request)
        assertEquals(request.id, id)
    }

    @Test
    fun enqueue_appearsInEnqueuedRequests() = runTest {
        val request = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build()
        workManager.enqueue(request)
        assertTrue(request in workManager.enqueuedRequests)
    }

    @Test
    fun enqueue_workInfoIsEnqueued() = runTest {
        val request = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build()
        val id = workManager.enqueue(request)
        assertNotNull(workManager.getWorkInfoById(id))
        assertEquals(WorkInfo.State.ENQUEUED, workManager.getWorkInfoById(id)?.state)
    }

    // --- Cancel ---

    @Test
    fun cancelWorkById_marksAsCancelled() = runTest {
        val request = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build()
        val id = workManager.enqueue(request)
        workManager.cancelWorkById(id)
        assertTrue(id in workManager.cancelledIds)
        assertEquals(WorkInfo.State.CANCELLED, workManager.getWorkInfoById(id)?.state)
    }

    @Test
    fun cancelAllWorkByTag_cancelsMatchingOnly() = runTest {
        val syncTag = "sync"
        val req1 = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").addTag(syncTag).build()
        val req2 = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").addTag(syncTag).build()
        val req3 = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").addTag("other").build()
        workManager.enqueue(req1)
        workManager.enqueue(req2)
        workManager.enqueue(req3)
        workManager.cancelAllWorkByTag(syncTag)
        assertEquals(WorkInfo.State.CANCELLED, workManager.getWorkInfoById(req1.id)?.state)
        assertEquals(WorkInfo.State.CANCELLED, workManager.getWorkInfoById(req2.id)?.state)
        assertEquals(WorkInfo.State.ENQUEUED, workManager.getWorkInfoById(req3.id)?.state)
    }

    // --- Lookup ---

    @Test
    fun getWorkInfoById_returnsNullForUnknownId() = runTest {
        assertNull(workManager.getWorkInfoById(Uuid.random()))
    }

    // --- Flow observation ---

    @Test
    fun getWorkInfosByTag_emitsMatchingItems() = runTest {
        val tag = "upload"
        val req = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").addTag(tag).build()
        workManager.enqueue(req)
        val items = workManager.getWorkInfosByTag(tag).first()
        assertEquals(1, items.size)
        assertEquals(req.id, items.first().id)
    }

    @Test
    fun getWorkInfosByTag_emitsEmptyForUnknownTag() = runTest {
        val items = workManager.getWorkInfosByTag("nonexistent").first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun getWorkInfosByTag_updatesOnStateChange() = runTest {
        val tag = "sync"
        val req = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").addTag(tag).build()
        val id = workManager.enqueue(req)
        workManager.simulateSuccess(id)
        val items = workManager.getWorkInfosByTag(tag).first()
        assertEquals(WorkInfo.State.SUCCEEDED, items.first().state)
    }

    // --- State transitions ---

    @Test
    fun simulateSuccess_transitionsToSucceeded() = runTest {
        val id = workManager.enqueue(OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build())
        workManager.simulateSuccess(id)
        assertEquals(WorkInfo.State.SUCCEEDED, workManager.getWorkInfoById(id)?.state)
    }

    @Test
    fun simulateFailure_transitionsToFailed() = runTest {
        val id = workManager.enqueue(OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build())
        workManager.simulateFailure(id)
        assertEquals(WorkInfo.State.FAILED, workManager.getWorkInfoById(id)?.state)
    }

    @Test
    fun simulateProgress_updatesProgressField() = runTest {
        val id = workManager.enqueue(OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build())
        workManager.simulateProgress(id, WorkProgress(42))
        assertEquals(42, workManager.getWorkInfoById(id)?.progress?.progress)
    }

    // --- WorkResult ---

    @Test
    fun workResult_successHasEmptyOutputDataByDefault() {
        val result = WorkResult.success()
        assertIs<WorkResult.Success>(result)
        assertEquals(WorkData.EMPTY, result.outputData)
    }

    @Test
    fun workResult_failureCarriesMessage() {
        val result = WorkResult.failure("network error")
        assertIs<WorkResult.Failure>(result)
        assertEquals("network error", result.message)
    }

    @Test
    fun workResult_retryCarriesReason() {
        val result = WorkResult.retry("rate limited")
        assertIs<WorkResult.Retry>(result)
        assertEquals("rate limited", result.reason)
    }

    // --- WorkData ---

    @Test
    fun workData_storesAndRetrievesTypedValues() {
        val data = workDataOf("name" to "sync", "count" to 5, "enabled" to true)
        assertEquals("sync", data.getString("name"))
        assertEquals(5, data.getInt("count"))
        assertTrue(data.getBoolean("enabled"))
    }

    @Test
    fun workData_returnsDefaultsForMissingKeys() {
        val data = WorkData.EMPTY
        assertNull(data.getString("missing"))
        assertEquals(0, data.getInt("missing"))
        assertFalse(data.getBoolean("missing"))
    }

    @Test
    fun workData_hasKeyReturnsFalseForMissing() {
        assertFalse(WorkData.EMPTY.hasKey("absent"))
    }

    // --- Constraints ---

    @Test
    fun constraints_noneHasNoRequirements() {
        val c = Constraints.NONE
        assertEquals(NetworkType.NOT_REQUIRED, c.requiredNetworkType)
        assertFalse(c.requiresCharging)
        assertFalse(c.requiresBatteryNotLow)
        assertFalse(c.requiresDeviceIdle)
        assertFalse(c.requiresStorageNotLow)
    }

    @Test
    fun constraints_builderSetsValues() {
        val c = Constraints {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
            setRequiresBatteryNotLow(true)
        }
        assertEquals(NetworkType.CONNECTED, c.requiredNetworkType)
        assertTrue(c.requiresCharging)
        assertTrue(c.requiresBatteryNotLow)
    }

    // --- RetryConfig ---

    @Test
    fun retryConfig_defaultValues() {
        assertEquals(3, RetryConfig.DEFAULT.maxAttempts)
        assertEquals(BackoffPolicy.EXPONENTIAL, RetryConfig.DEFAULT.backoffPolicy)
        assertEquals(2.0, RetryConfig.DEFAULT.multiplier)
    }

    @Test
    fun retryConfig_aggressiveHasMoreAttempts() {
        assertTrue(RetryConfig.AGGRESSIVE.maxAttempts > RetryConfig.DEFAULT.maxAttempts)
    }

    // --- WorkInfo ---

    @Test
    fun workInfo_isFinishedForTerminalStates() {
        val id = Uuid.random()
        assertTrue(WorkInfo(id = id, state = WorkInfo.State.SUCCEEDED).isFinished)
        assertTrue(WorkInfo(id = id, state = WorkInfo.State.FAILED).isFinished)
        assertTrue(WorkInfo(id = id, state = WorkInfo.State.CANCELLED).isFinished)
        assertFalse(WorkInfo(id = id, state = WorkInfo.State.RUNNING).isFinished)
        assertFalse(WorkInfo(id = id, state = WorkInfo.State.ENQUEUED).isFinished)
        assertFalse(WorkInfo(id = id, state = WorkInfo.State.BLOCKED).isFinished)
    }

    // --- WorkProgress ---

    @Test
    fun workProgress_noneIsIndeterminate() {
        assertTrue(WorkProgress.NONE.isIndeterminate)
        assertFalse(WorkProgress.NONE.isComplete)
    }

    @Test
    fun workProgress_completeMarksDone() {
        assertTrue(WorkProgress.COMPLETE.isComplete)
        assertFalse(WorkProgress.COMPLETE.isIndeterminate)
    }

    @Test
    fun workProgress_midpoint() {
        val p = WorkProgress(50)
        assertFalse(p.isIndeterminate)
        assertFalse(p.isComplete)
        assertEquals(50, p.progress)
    }

    // --- WorkContinuation ---

    @Test
    fun workContinuation_chainEnqueuesAllSteps() = runTest {
        val step1 = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build()
        val step2 = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").build()
        fun CoroutineScope.autoSucceed(id: Uuid) = launch {
            while (workManager.getWorkInfoById(id) == null) delay(1)
            workManager.simulateSuccess(id)
        }
        autoSucceed(step1.id)
        autoSucceed(step2.id)
        val ids = workManager.beginWith(step1).then(step2).enqueue()
        assertEquals(2, ids.size)
        assertEquals(2, workManager.enqueuedRequests.size)
    }

    // --- Tags ---

    @Test
    fun request_tagsArePreservedInWorkInfo() = runTest {
        val request = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker")
            .addTag("upload")
            .addTag("media")
            .build()
        val id = workManager.enqueue(request)
        val info = workManager.getWorkInfoById(id)
        assertTrue("upload" in (info?.tags ?: emptySet()))
        assertTrue("media" in (info?.tags ?: emptySet()))
    }
}

class FakeWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}
