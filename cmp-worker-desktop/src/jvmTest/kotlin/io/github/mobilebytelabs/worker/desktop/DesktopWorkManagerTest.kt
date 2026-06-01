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

private fun workManager(persistence: DesktopWorkPersistence = NoOpDesktopWorkPersistence) =
    DesktopWorkManager(config = inMemoryConfig, workerFactory = FakeFactory, persistence = persistence)

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

    // ── Persistence ────────────────────────────────────────────────────────────

    @Test
    fun persistence_enqueuedWorkRestoredOnRestart() = runTest {
        val sharedPersistence = InMemoryDesktopWorkPersistence()

        val wm1 = workManager(sharedPersistence)
        val request = OneTimeWorkRequestBuilder<SlowWorker>(SlowWorker::class.qualifiedName!!).build()
        wm1.enqueue(request)
        // Wait until work is persisted in ENQUEUED state before "restart"
        eventually { sharedPersistence.loadAll().any { it.id == request.id } }
        wm1.shutdown()

        val wm2 = workManager(sharedPersistence)
        eventually { wm2.getWorkInfoById(request.id)?.state != null }
        val restored = wm2.getWorkInfoById(request.id)
        assertNotNull(restored, "Work should be restored from persistence after restart")
        assertEquals(request.id, restored.id)
        wm2.shutdown()
    }

    @Test
    fun persistence_runningWorkRestoredAsEnqueued() = runTest {
        val sharedPersistence = InMemoryDesktopWorkPersistence()
        // Manually simulate a RUNNING entry in persistence (as if the JVM crashed mid-work)
        sharedPersistence.save(
            WorkInfo(
                id = Uuid.random(),
                state = WorkInfo.State.RUNNING,
                tags = setOf("crashed"),
            ),
        )
        val wm = workManager(sharedPersistence)
        eventually { wm.getWorkInfosByTag("crashed").first().isNotEmpty() }
        val info = wm.getWorkInfosByTag("crashed").first().first()
        assertEquals(WorkInfo.State.ENQUEUED, info.state, "RUNNING state should be reset to ENQUEUED on restore")
        wm.shutdown()
    }

    @Test
    fun persistence_succeededWork_removedFromPersistence() = runTest {
        val sharedPersistence = InMemoryDesktopWorkPersistence()
        val wm = workManager(sharedPersistence)
        val request = OneTimeWorkRequestBuilder<SuccessWorker>(SuccessWorker::class.qualifiedName!!).build()
        val id = wm.enqueue(request)
        // CI runners (especially under load) can take significant wall-clock time for
        // the worker to transition to SUCCEEDED + the post-success persistence cleanup
        // to settle. History: 2s → 10s (a7cf360) → 20s (this commit, kover-100-coverage
        // PR #31 first run flake at the 10s ceiling). Local + warm CI typically clears
        // in <500ms.
        eventually(timeoutMs = 20_000, description = "work $id reaches SUCCEEDED") {
            wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED
        }
        eventually(timeoutMs = 20_000, description = "persistence cleared of succeeded work $id") {
            sharedPersistence.loadAll().none { it.id == id }
        }
        wm.shutdown()
    }

    @Test
    fun persistence_cancelledWork_removedFromPersistence() = runTest {
        val sharedPersistence = InMemoryDesktopWorkPersistence()
        val wm = workManager(sharedPersistence)
        val request = OneTimeWorkRequestBuilder<SlowWorker>(SlowWorker::class.qualifiedName!!).build()
        val id = wm.enqueue(request)
        eventually { sharedPersistence.loadAll().any { it.id == id } }
        wm.cancelWorkById(id)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.CANCELLED }
        eventually { sharedPersistence.loadAll().none { it.id == id } }
        assertTrue(
            sharedPersistence.loadAll().none {
                it.id == id
            },
            "Cancelled work should be deleted from persistence",
        )
        wm.shutdown()
    }

    @Test
    fun persistence_disabled_noOp() = runTest {
        // NoOpDesktopWorkPersistence (IN_MEMORY config) — workManager() default
        val wm = workManager()
        val request = OneTimeWorkRequestBuilder<SuccessWorker>(SuccessWorker::class.qualifiedName!!).build()
        val id = wm.enqueue(request)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        // No crash = persistence no-op works correctly
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
        wm.shutdown()
    }
}

// ── Test utilities ────────────────────────────────────────────────────────────

/**
 * Poll [condition] every [intervalMs] up to [timeoutMs]; throw [AssertionError] on
 * timeout. Previously this helper returned silently when the deadline expired —
 * combined with a following `assertX(...)` checking the same condition, it produced
 * confusing "AssertionError at line N" failures that masked the underlying timeout.
 * Loud-on-timeout makes CI flakes (slow runners, missed deadline) immediately
 * diagnosable. Callers that paired this with a following assertion are now
 * redundantly-safe — the timeout AssertionError fires first.
 */
private suspend fun eventually(
    timeoutMs: Long = 2_000,
    intervalMs: Long = 50,
    description: String = "condition",
    condition: suspend () -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        // Thread.sleep uses real OS time, not virtual coroutine time, so Dispatchers.IO
        // delays in DesktopWorkManager (retries, periodic intervals) can actually progress.
        withContext(Dispatchers.IO) { Thread.sleep(intervalMs) }
    }
    throw AssertionError("eventually(timeoutMs=$timeoutMs) timed out waiting for: $description")
}
