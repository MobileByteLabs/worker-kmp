package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.NetworkType
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

// ── Fake workers ─────────────────────────────────────────────────────────────

class SuccessWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork() = WorkResult.success(workDataOf("result" to "done"))
}

class FailWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork() = WorkResult.failure("intentional failure")
}

class RetryThenSucceedWorker(context: WorkerContext) : CoroutineWorker(context) {
    companion object {
        var callCount = 0
    }
    override suspend fun doWork(): WorkResult {
        callCount++
        return if (callCount < 3) WorkResult.retry("not ready") else WorkResult.success()
    }
}

class ProgressReportingWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        setProgress(WorkProgress(25))
        setProgress(WorkProgress(75))
        setProgress(WorkProgress(100))
        return WorkResult.success()
    }
}

class SlowWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        delay(10_000)
        return WorkResult.success()
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val inMemoryConfig = DesktopWorkManagerConfig.IN_MEMORY

private fun fakeFactory(workerClass: String, context: WorkerContext): CoroutineWorker = when (workerClass) {
    SuccessWorker::class.qualifiedName -> SuccessWorker(context)
    FailWorker::class.qualifiedName -> FailWorker(context)
    RetryThenSucceedWorker::class.qualifiedName -> RetryThenSucceedWorker(context)
    ProgressReportingWorker::class.qualifiedName -> ProgressReportingWorker(context)
    SlowWorker::class.qualifiedName -> SlowWorker(context)
    else -> throw IllegalArgumentException("Unknown worker: $workerClass")
}

private object FakeFactory : DesktopWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext) = fakeFactory(workerClass, context)
}

private fun workManager() = DesktopWorkManager(config = inMemoryConfig, workerFactory = FakeFactory)

// ── Tests ─────────────────────────────────────────────────────────────────────

class DesktopWorkManagerTest {

    @Test
    fun enqueue_successWorker_transitionsToSucceeded() = runTest {
        val wm = workManager()
        val request = OneTimeWorkRequestBuilder<SuccessWorker>(SuccessWorker::class.qualifiedName!!).build()
        val id = wm.enqueue(request)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        val info = wm.getWorkInfoById(id)
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
        assertEquals("done", info?.outputData?.getString("result"))
    }

    @Test
    fun enqueue_failWorker_transitionsToFailed() = runTest {
        val wm = workManager()
        val request = OneTimeWorkRequestBuilder<FailWorker>(FailWorker::class.qualifiedName!!).build()
        val id = wm.enqueue(request)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.FAILED }
        assertEquals(WorkInfo.State.FAILED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_retryWorker_retriesAndSucceeds() = runTest {
        RetryThenSucceedWorker.callCount = 0
        val wm = workManager()
        val shortRetry = io.github.mobilebytelabs.worker.RetryConfig(
            maxAttempts = 3,
            initialDelay = 50.milliseconds,
            backoffPolicy = io.github.mobilebytelabs.worker.BackoffPolicy.LINEAR,
        )
        val request = OneTimeWorkRequestBuilder<RetryThenSucceedWorker>(
            RetryThenSucceedWorker::class.qualifiedName!!,
        ).setBackoffCriteria(io.github.mobilebytelabs.worker.BackoffPolicy.LINEAR, shortRetry).build()
        val id = wm.enqueue(request)
        eventually(timeoutMs = 3_000) { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
        assertTrue(RetryThenSucceedWorker.callCount >= 3)
    }

    @Test
    fun cancelWorkById_stopsRunningWorker() = runTest {
        val wm = workManager()
        val request = OneTimeWorkRequestBuilder<SlowWorker>(SlowWorker::class.qualifiedName!!).build()
        val id = wm.enqueue(request)
        delay(50)
        wm.cancelWorkById(id)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.CANCELLED }
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun getWorkInfoById_returnsNullForUnknown() = runTest {
        val wm = workManager()
        assertNull(wm.getWorkInfoById(Uuid.random()))
    }

    @Test
    fun getWorkInfosByTag_flowEmitsOnStateChange() = runTest {
        val wm = workManager()
        val tag = "data-sync"
        val request = OneTimeWorkRequestBuilder<SuccessWorker>(SuccessWorker::class.qualifiedName!!)
            .addTag(tag)
            .build()
        wm.enqueue(request)
        eventually { wm.getWorkInfoById(request.id)?.state == WorkInfo.State.SUCCEEDED }
        val items = wm.getWorkInfosByTag(tag).first()
        assertTrue(items.any { it.id == request.id })
    }

    @Test
    fun progressReportingWorker_updatesProgress() = runTest {
        val wm = workManager()
        val request = OneTimeWorkRequestBuilder<ProgressReportingWorker>(
            ProgressReportingWorker::class.qualifiedName!!,
        ).build()
        val id = wm.enqueue(request)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        // After completion the last progress set was 100
        assertEquals(100, wm.getWorkInfoById(id)?.progress?.progress)
    }

    @Test
    fun periodicWork_executesMultipleTimes() = runTest {
        val wm = workManager()
        val tag = "periodic-sync"
        val request = PeriodicWorkRequestBuilder<SuccessWorker>(
            SuccessWorker::class.qualifiedName!!,
            repeatInterval = 100.milliseconds,
        ).addTag(tag).build()
        wm.enqueueUniquePeriodicWork("periodic-sync", ExistingPeriodicWorkPolicy.REPLACE, request)
        eventually(timeoutMs = 2_000) { (wm.getWorkInfoById(request.id)?.runAttemptCount ?: 0) >= 2 }
        val attemptCount = wm.getWorkInfoById(request.id)?.runAttemptCount ?: 0
        assertTrue(attemptCount >= 2, "Expected at least 2 runs, got $attemptCount")
        wm.cancelWorkById(request.id)
    }

    @Test
    fun cancelAllWorkByTag_cancelsAllMatchingWork() = runTest {
        val wm = workManager()
        val tag = "batch"
        val ids = (1..3).map {
            val req = OneTimeWorkRequestBuilder<SlowWorker>(SlowWorker::class.qualifiedName!!)
                .addTag(tag)
                .build()
            wm.enqueue(req)
        }
        wm.cancelAllWorkByTag(tag)
        ids.forEach { id ->
            eventually { wm.getWorkInfoById(id)?.isFinished == true }
        }
        ids.forEach { id ->
            assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(id)?.state)
        }
    }

    @Test
    fun workData_inputPassedToWorker() = runTest {
        var receivedData: WorkData? = null
        val captureFactory = object : DesktopWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker {
                receivedData = context.inputData
                return SuccessWorker(context)
            }
        }
        val wm = DesktopWorkManager(config = inMemoryConfig, workerFactory = captureFactory)
        val input = workDataOf("key" to "value123")
        val request = OneTimeWorkRequestBuilder<SuccessWorker>(SuccessWorker::class.qualifiedName!!)
            .setInputData(input)
            .build()
        wm.enqueue(request)
        eventually { receivedData != null }
        assertEquals("value123", receivedData?.getString("key"))
    }

    @Test
    fun backoffDelay_exponentialGrowth() {
        val config = io.github.mobilebytelabs.worker.RetryConfig(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            backoffPolicy = io.github.mobilebytelabs.worker.BackoffPolicy.EXPONENTIAL,
        )
        assertEquals(1_000, backoffDelay(config, 0).inWholeMilliseconds)
        assertEquals(2_000, backoffDelay(config, 1).inWholeMilliseconds)
        assertEquals(4_000, backoffDelay(config, 2).inWholeMilliseconds)
    }

    @Test
    fun backoffDelay_linearGrowth() {
        val config = io.github.mobilebytelabs.worker.RetryConfig(
            initialDelay = 1.seconds,
            backoffPolicy = io.github.mobilebytelabs.worker.BackoffPolicy.LINEAR,
        )
        assertEquals(1_000, backoffDelay(config, 0).inWholeMilliseconds)
        assertEquals(2_000, backoffDelay(config, 1).inWholeMilliseconds)
        assertEquals(3_000, backoffDelay(config, 2).inWholeMilliseconds)
    }

    @Test
    fun backoffDelay_capsAtMaxDelay() {
        val config = io.github.mobilebytelabs.worker.RetryConfig(
            initialDelay = 1.minutes,
            maxDelay = 5.seconds,
            backoffPolicy = io.github.mobilebytelabs.worker.BackoffPolicy.EXPONENTIAL,
        )
        assertTrue(backoffDelay(config, 0).inWholeMilliseconds <= 5_000)
    }
}

// ── Test utilities ────────────────────────────────────────────────────────────

private suspend fun eventually(timeoutMs: Long = 2_000, intervalMs: Long = 50, condition: suspend () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        // Thread.sleep uses real OS time, not virtual coroutine time, so Dispatchers.IO
        // delays in DesktopWorkManager (retries, periodic intervals) can actually progress.
        withContext(Dispatchers.IO) { Thread.sleep(intervalMs) }
    }
}
