package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.BackoffPolicy
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.RetryConfig
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.oneTimeWorkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

class IosWorkManagerTest {

    private fun workManager() = IosWorkManager(workerFactory = TestIosWorkerFactory)

    @Test
    fun enqueue_returnsId() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<SuccessIosWorker>(
            SuccessIosWorker::class.simpleName!!
        ).build()
        val id = wm.enqueue(req)
        assertEquals(req.id, id)
    }

    @Test
    fun enqueue_successWorker_transitionsToSucceeded() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<SuccessIosWorker>(
            SuccessIosWorker::class.simpleName!!
        ).build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_failWorker_transitionsToFailed() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<FailIosWorker>(
            FailIosWorker::class.simpleName!!
        ).build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.FAILED }
        assertEquals(WorkInfo.State.FAILED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_retryWorker_retriesAndSucceeds() = runTest {
        RetryThenSucceedIosWorker.callCount = 0
        val wm = workManager()
        val shortRetry = RetryConfig(
            maxAttempts = 3,
            initialDelay = 10.milliseconds,
            backoffPolicy = BackoffPolicy.LINEAR
        )
        val req = OneTimeWorkRequestBuilder<RetryThenSucceedIosWorker>(
            RetryThenSucceedIosWorker::class.simpleName!!
        ).setBackoffCriteria(BackoffPolicy.LINEAR, shortRetry).build()
        val id = wm.enqueue(req)
        eventually(timeoutMs = 3_000) { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
        assertTrue(RetryThenSucceedIosWorker.callCount >= 3)
    }

    @Test
    fun cancelWorkById_stopsEnqueuedWork() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<SlowIosWorker>(
            SlowIosWorker::class.simpleName!!
        ).build()
        val id = wm.enqueue(req)
        wm.cancelWorkById(id)
        eventually { wm.getWorkInfoById(id)?.state?.isFinished == true }
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
        val tag = "ios-sync"
        val req = OneTimeWorkRequestBuilder<SuccessIosWorker>(
            SuccessIosWorker::class.simpleName!!
        ).addTag(tag).build()
        wm.enqueue(req)
        eventually { wm.getWorkInfoById(req.id)?.state == WorkInfo.State.SUCCEEDED }
        val items = wm.getWorkInfosByTag(tag).first()
        assertTrue(items.any { it.id == req.id })
    }

    @Test
    fun cancelAllWorkByTag_cancelsAll() = runTest {
        val wm = workManager()
        val tag = "batch-ios"
        val ids = (1..3).map {
            val req = OneTimeWorkRequestBuilder<SlowIosWorker>(
                SlowIosWorker::class.simpleName!!
            ).addTag(tag).build()
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
    fun enqueue_passesInputData() = runTest {
        var received: WorkData? = null
        val factory = object : IosWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker {
                received = context.inputData
                return SuccessIosWorker(context)
            }
        }
        val wm = IosWorkManager(workerFactory = factory)
        val data = WorkData("key" to "value", "num" to 42)
        val req = oneTimeWorkRequest<SuccessIosWorker> { setInputData(data) }
        wm.enqueue(req)
        eventually { received != null }
        assertEquals("value", received?.getString("key"))
    }

    @Test
    fun progressReportingWorker_updatesProgress() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<ProgressIosWorker>(
            ProgressIosWorker::class.simpleName!!
        ).build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(100, wm.getWorkInfoById(id)?.progress?.progress)
    }

    @Test
    fun enqueueUniquePeriodicWork_executesMultipleTimes() = runTest {
        val wm = workManager()
        val req = PeriodicWorkRequestBuilder<SuccessIosWorker>(
            SuccessIosWorker::class.simpleName!!,
            repeatInterval = 50.milliseconds
        ).build()
        wm.enqueueUniquePeriodicWork("periodic-ios", ExistingPeriodicWorkPolicy.REPLACE, req)
        eventually(timeoutMs = 2_000) {
            (wm.getWorkInfoById(req.id)?.runAttemptCount ?: 0) >= 2
        }
        val count = wm.getWorkInfoById(req.id)?.runAttemptCount ?: 0
        assertTrue(count >= 2, "Expected ≥2 runs, got $count")
        wm.cancelWorkById(req.id)
    }

    @Test
    fun enqueueUniquePeriodicWork_replace_cancelsExisting() = runTest {
        val wm = workManager()
        val req1 = PeriodicWorkRequestBuilder<SuccessIosWorker>(
            SuccessIosWorker::class.simpleName!!,
            repeatInterval = 5.minutes
        ).build()
        val req2 = PeriodicWorkRequestBuilder<SuccessIosWorker>(
            SuccessIosWorker::class.simpleName!!,
            repeatInterval = 5.minutes
        ).build()
        wm.enqueueUniquePeriodicWork("unique-ios", ExistingPeriodicWorkPolicy.REPLACE, req1)
        wm.enqueueUniquePeriodicWork("unique-ios", ExistingPeriodicWorkPolicy.REPLACE, req2)
        eventually { wm.getWorkInfoById(req1.id)?.isFinished == true }
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(req1.id)?.state)
    }
}

// ── Test workers ──────────────────────────────────────────────────────────────

class SuccessIosWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}

class FailIosWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.failure("fail")
}

class SlowIosWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        delay(30_000)
        return WorkResult.success()
    }
}

class ProgressIosWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        setProgress(WorkProgress(50))
        setProgress(WorkProgress(100))
        return WorkResult.success()
    }
}

class RetryThenSucceedIosWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        callCount++
        return if (callCount >= 3) WorkResult.success() else WorkResult.retry()
    }

    companion object {
        var callCount = 0
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

private object TestIosWorkerFactory : IosWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker {
        return when (workerClass) {
            SuccessIosWorker::class.simpleName -> SuccessIosWorker(context)
            FailIosWorker::class.simpleName -> FailIosWorker(context)
            SlowIosWorker::class.simpleName -> SlowIosWorker(context)
            ProgressIosWorker::class.simpleName -> ProgressIosWorker(context)
            RetryThenSucceedIosWorker::class.simpleName -> RetryThenSucceedIosWorker(context)
            else -> throw IllegalArgumentException("Unknown worker: $workerClass")
        }
    }
}

// ── Test helper ───────────────────────────────────────────────────────────────

private suspend fun eventually(
    timeoutMs: Long = 2_000,
    intervalMs: Long = 30,
    condition: suspend () -> Boolean
) {
    withContext(Dispatchers.Default) {
        val mark = kotlin.time.TimeSource.Monotonic.markNow()
        while (mark.elapsedNow().inWholeMilliseconds < timeoutMs) {
            if (condition()) return@withContext
            delay(intervalMs)
        }
    }
}
