package io.github.mobilebytelabs.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class WorkContinuationTest {

    private val wm = TestWorkManager()

    private fun request(tag: String = "step") = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker").addTag(tag).build()

    /**
     * Launches a coroutine that watches for each [id] to appear in the store
     * (i.e. enqueued by the chain executor) and then simulates success. IDs are
     * processed in order, so step-2's ID is only awaited after step-1 is done.
     */
    private fun CoroutineScope.launchAutoSucceed(vararg specs: Pair<Uuid, WorkData>) = launch {
        for ((id, output) in specs) {
            while (wm.getWorkInfoById(id) == null) delay(1)
            wm.simulateSuccess(id, output)
        }
    }

    private fun CoroutineScope.launchAutoSucceed(vararg ids: Uuid) =
        launchAutoSucceed(*ids.map { it to WorkData.EMPTY }.toTypedArray())

    private fun CoroutineScope.launchAutoFail(id: Uuid) = launch {
        while (wm.getWorkInfoById(id) == null) delay(1)
        wm.simulateFailure(id)
    }

    private fun CoroutineScope.launchAutoCancel(id: Uuid) = launch {
        while (wm.getWorkInfoById(id) == null) delay(1)
        wm.cancelWorkById(id)
    }

    // ── Single step ───────────────────────────────────────────────────────────

    @Test
    fun beginWith_single_returnsOneId() = runTest {
        val req = request()
        launchAutoSucceed(req.id)

        val ids = wm.beginWith(req).enqueue()

        assertEquals(1, ids.size)
        assertEquals(req.id, ids[0])
    }

    // ── Sequential chain ──────────────────────────────────────────────────────

    @Test
    fun thenChain_allStepsEnqueued_onSuccess() = runTest {
        val a = request("a")
        val b = request("b")
        launchAutoSucceed(a.id, b.id)

        val ids = wm.beginWith(a).then(b).enqueue()

        assertEquals(2, ids.size)
        assertTrue(a.id in ids)
        assertTrue(b.id in ids)
    }

    @Test
    fun threeStepChain_allEnqueued() = runTest {
        val a = request("a")
        val b = request("b")
        val c = request("c")
        launchAutoSucceed(a.id, b.id, c.id)

        val ids = wm.beginWith(a).then(b).then(c).enqueue()

        assertEquals(3, ids.size)
    }

    // ── Halt on failure ───────────────────────────────────────────────────────

    @Test
    fun chainHalts_whenFirstStepFails() = runTest {
        val a = request("a")
        val b = request("b")
        launchAutoFail(a.id)

        val ids = wm.beginWith(a).then(b).enqueue()

        assertEquals(1, ids.size, "chain must stop after failed step")
        assertTrue(a.id in ids)
        assertFalse(b.id in ids, "b must not be enqueued when a fails")
    }

    @Test
    fun chainHalts_whenFirstStepCancelled() = runTest {
        val a = request("a")
        val b = request("b")
        launchAutoCancel(a.id)

        val ids = wm.beginWith(a).then(b).enqueue()

        assertEquals(1, ids.size)
        assertFalse(b.id in ids)
    }

    @Test
    fun chainHalts_whenMiddleStepFails() = runTest {
        val a = request("a")
        val b = request("b")
        val c = request("c")
        launchAutoSucceed(a.id)
        launchAutoFail(b.id)

        val ids = wm.beginWith(a).then(b).then(c).enqueue()

        assertEquals(2, ids.size)
        assertTrue(a.id in ids)
        assertTrue(b.id in ids)
        assertFalse(c.id in ids)
    }

    // ── Output data propagation ───────────────────────────────────────────────

    @Test
    fun outputData_fromFirstStep_mergedIntoSecondStepInput() = runTest {
        val a = request("a")
        val b = request("b")
        launchAutoSucceed(a.id to workDataOf("key" to "fromA"), b.id to WorkData.EMPTY)

        wm.beginWith(a).then(b).enqueue()

        val bRequest = wm.enqueuedRequests.filterIsInstance<OneTimeWorkRequest>()
            .first { it.id == b.id }
        assertEquals("fromA", bRequest.inputData.getString("key"))
    }

    @Test
    fun outputData_mergedAcrossMultipleSteps() = runTest {
        val a = request("a")
        val b = request("b")
        val c = request("c")
        launchAutoSucceed(
            a.id to workDataOf("a_key" to "a_val"),
            b.id to workDataOf("b_key" to "b_val"),
            c.id to WorkData.EMPTY,
        )

        wm.beginWith(a).then(b).then(c).enqueue()

        val cRequest = wm.enqueuedRequests.filterIsInstance<OneTimeWorkRequest>()
            .first { it.id == c.id }
        assertEquals("a_val", cRequest.inputData.getString("a_key"))
        assertEquals("b_val", cRequest.inputData.getString("b_key"))
    }

    @Test
    fun originalInputData_preservedWhenNoOutputFromPrevious() = runTest {
        val a = request("a")
        val b = OneTimeWorkRequestBuilder<FakeWorker>("FakeWorker")
            .setInputData(workDataOf("original" to "value"))
            .build()
        launchAutoSucceed(a.id, b.id)

        wm.beginWith(a).then(b).enqueue()

        val bRequest = wm.enqueuedRequests.filterIsInstance<OneTimeWorkRequest>()
            .first { it.id == b.id }
        assertEquals("value", bRequest.inputData.getString("original"))
    }

    // ── Parallel initial steps ────────────────────────────────────────────────

    @Test
    fun beginWithList_allEnqueuedConcurrently() = runTest {
        val a = request("a")
        val b = request("b")
        launchAutoSucceed(a.id, b.id)

        val ids = wm.beginWith(listOf(a, b)).enqueue()

        assertEquals(2, ids.size)
        assertTrue(a.id in ids)
        assertTrue(b.id in ids)
    }
}

class WorkContinuationConditionalWorkerTest {

    private class AlwaysRunWorker(context: WorkerContext) : ConditionalWorker(context) {
        override suspend fun condition(): Boolean = true
        override suspend fun doConditionalWork(): WorkResult = WorkResult.success(workDataOf("ran" to true))
    }

    private class NeverRunWorker(context: WorkerContext) : ConditionalWorker(context) {
        override suspend fun condition(): Boolean = false
        override suspend fun doConditionalWork(): WorkResult = WorkResult.success()
    }

    private class RetryOnConditionWorker(context: WorkerContext) : ConditionalWorker(context) {
        var callCount = 0
        override suspend fun condition(): Boolean = true
        override suspend fun doConditionalWork(): WorkResult {
            callCount++
            return if (callCount < 2) WorkResult.retry() else WorkResult.success()
        }
    }

    @Test
    fun conditionalWorker_runsWhenConditionTrue() = runTest {
        val worker = AlwaysRunWorker(TestWorkerContext())
        val result = worker.doWork()
        assertTrue(result is WorkResult.Success)
        assertEquals(true, (result as WorkResult.Success).outputData.getBoolean("ran"))
    }

    @Test
    fun conditionalWorker_skipsWhenConditionFalse() = runTest {
        val worker = NeverRunWorker(TestWorkerContext())
        val result = worker.doWork()
        assertTrue(result is WorkResult.Failure, "expected Failure but got $result")
    }

    @Test
    fun conditionalWorker_canRetryFromDoConditionalWork() = runTest {
        val worker = RetryOnConditionWorker(TestWorkerContext())
        val first = worker.doWork()
        assertTrue(first is WorkResult.Retry)
        val second = worker.doWork()
        assertTrue(second is WorkResult.Success)
    }
}
