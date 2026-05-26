@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.BackoffPolicy
import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.NetworkType
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

class WebWorkManagerTest {

    private fun workManager(
        constraintEvaluator: WebConstraintEvaluator = WebConstraintEvaluator { true },
        persistence: WebWorkPersistence = InMemoryWorkPersistence(),
    ) = WebWorkManager(
        workerFactory = TestWebWorkerFactory,
        constraintEvaluator = constraintEvaluator,
        persistence = persistence,
    )

    @Test
    fun enqueue_returnsId() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(
            SuccessWebWorker::class.simpleName!!,
        ).build()
        val id = wm.enqueue(req)
        assertEquals(req.id, id)
    }

    @Test
    fun enqueue_successWorker_transitionsToSucceeded() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(
            SuccessWebWorker::class.simpleName!!,
        ).build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_failWorker_transitionsToFailed() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<FailWebWorker>(
            FailWebWorker::class.simpleName!!,
        ).build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.FAILED }
        assertEquals(WorkInfo.State.FAILED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_retryWorker_retriesAndSucceeds() = runTest {
        RetryThenSucceedWebWorker.callCount = 0
        val wm = workManager()
        val shortRetry = RetryConfig(
            maxAttempts = 3,
            initialDelay = 10.milliseconds,
            backoffPolicy = BackoffPolicy.LINEAR,
        )
        val req = OneTimeWorkRequestBuilder<RetryThenSucceedWebWorker>(
            RetryThenSucceedWebWorker::class.simpleName!!,
        ).setBackoffCriteria(BackoffPolicy.LINEAR, shortRetry).build()
        val id = wm.enqueue(req)
        eventually(timeoutMs = 3_000) { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
        assertTrue(RetryThenSucceedWebWorker.callCount >= 3)
    }

    @Test
    fun cancelWorkById_stopsEnqueuedWork() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<SlowWebWorker>(
            SlowWebWorker::class.simpleName!!,
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
        val tag = "web-sync"
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(
            SuccessWebWorker::class.simpleName!!,
        ).addTag(tag).build()
        wm.enqueue(req)
        eventually { wm.getWorkInfoById(req.id)?.state == WorkInfo.State.SUCCEEDED }
        val items = wm.getWorkInfosByTag(tag).first()
        assertTrue(items.any { it.id == req.id })
    }

    @Test
    fun cancelAllWorkByTag_cancelsAll() = runTest {
        val wm = workManager()
        val tag = "batch-web"
        val ids = (1..3).map {
            val req = OneTimeWorkRequestBuilder<SlowWebWorker>(
                SlowWebWorker::class.simpleName!!,
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
        val factory = object : WebWorkerFactory {
            override fun create(
                workerClass: String,
                context: WorkerContext,
            ): io.github.mobilebytelabs.worker.CoroutineWorker {
                received = context.inputData
                return SuccessWebWorker(context)
            }
        }
        val wm = WebWorkManager(workerFactory = factory)
        val data = WorkData("key" to "value", "num" to 42)
        val req = oneTimeWorkRequest<SuccessWebWorker> { setInputData(data) }
        wm.enqueue(req)
        eventually { received != null }
        assertEquals("value", received?.getString("key"))
    }

    @Test
    fun progressReportingWorker_updatesProgress() = runTest {
        val wm = workManager()
        val req = OneTimeWorkRequestBuilder<ProgressWebWorker>(
            ProgressWebWorker::class.simpleName!!,
        ).build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(100, wm.getWorkInfoById(id)?.progress?.progress)
    }

    @Test
    fun enqueueUniquePeriodicWork_executesMultipleTimes() = runTest {
        val wm = workManager()
        val req = PeriodicWorkRequestBuilder<SuccessWebWorker>(
            SuccessWebWorker::class.simpleName!!,
            repeatInterval = 50.milliseconds,
        ).build()
        wm.enqueueUniquePeriodicWork("periodic-web", ExistingPeriodicWorkPolicy.REPLACE, req)
        eventually(timeoutMs = 2_000) {
            (wm.getWorkInfoById(req.id)?.runAttemptCount ?: 0) >= 2
        }
        val count = wm.getWorkInfoById(req.id)?.runAttemptCount ?: 0
        assertTrue(count >= 2, "Expected ≥2 runs, got $count")
        wm.cancelWorkById(req.id)
    }

    // ── Constraint evaluation ─────────────────────────────────────────────────

    @Test
    fun enqueue_withNetworkNotRequired_executesImmediately() = runTest {
        val wm = workManager(constraintEvaluator = WebConstraintEvaluator { true })
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints { setRequiredNetworkType(NetworkType.NOT_REQUIRED) })
            .build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_withUnsatisfiedThenSatisfiedConstraint_eventuallyExecutes() = runTest {
        var checkCount = 0
        val fastConfig = WebWorkManagerConfig(constraintCheckIntervalMs = 10)
        val evaluator = WebConstraintEvaluator { checkCount++ >= 2 }
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            config = fastConfig,
            constraintEvaluator = evaluator,
        )
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!).build()
        val id = wm.enqueue(req)
        eventually(timeoutMs = 3_000) { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
        assertTrue(checkCount >= 3, "Evaluator should have been checked at least 3 times, got $checkCount")
    }

    @Test
    fun enqueue_withConstraintAlwaysFailing_staysEnqueued() = runTest {
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            config = WebWorkManagerConfig(constraintCheckIntervalMs = 50),
            constraintEvaluator = WebConstraintEvaluator { false },
        )
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!).build()
        val id = wm.enqueue(req)
        delay(200)
        val state = wm.getWorkInfoById(id)?.state
        assertTrue(state == WorkInfo.State.ENQUEUED, "Expected ENQUEUED while constraints unmet, got $state")
        wm.shutdown()
    }

    @Test
    fun enqueue_withBatteryNotLow_constraintSatisfied_executes() = runTest {
        val wm = workManager(constraintEvaluator = WebConstraintEvaluator { true })
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints { setRequiresBatteryNotLow(true) })
            .build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_withBatteryNotLow_constraintFailed_staysEnqueued() = runTest {
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            config = WebWorkManagerConfig(constraintCheckIntervalMs = 50),
            constraintEvaluator = WebConstraintEvaluator { constraints ->
                !constraints.requiresBatteryNotLow
            },
        )
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints { setRequiresBatteryNotLow(true) })
            .build()
        val id = wm.enqueue(req)
        delay(200)
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(id)?.state)
        wm.shutdown()
    }

    @Test
    fun enqueue_withChargingRequired_constraintSatisfied_executes() = runTest {
        val wm = workManager(constraintEvaluator = WebConstraintEvaluator { true })
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints { setRequiresCharging(true) })
            .build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_withStorageNotLow_constraintSatisfied_executes() = runTest {
        val wm = workManager(constraintEvaluator = WebConstraintEvaluator { true })
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints { setRequiresStorageNotLow(true) })
            .build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_withMultipleConstraints_allSatisfied_executes() = runTest {
        val wm = workManager(constraintEvaluator = WebConstraintEvaluator { true })
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints {
                setRequiredNetworkType(NetworkType.CONNECTED)
                setRequiresBatteryNotLow(true)
                setRequiresStorageNotLow(true)
            })
            .build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_withMultipleConstraints_oneUnsatisfied_staysEnqueued() = runTest {
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            config = WebWorkManagerConfig(constraintCheckIntervalMs = 50),
            constraintEvaluator = WebConstraintEvaluator { constraints ->
                // network ok, but battery always low
                !constraints.requiresBatteryNotLow
            },
        )
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints {
                setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                setRequiresBatteryNotLow(true)
            })
            .build()
        val id = wm.enqueue(req)
        delay(200)
        assertEquals(WorkInfo.State.ENQUEUED, wm.getWorkInfoById(id)?.state)
        wm.shutdown()
    }

    @Test
    fun persistence_save_isCalledOnEnqueue() = runTest {
        val persistence = InMemoryWorkPersistence()
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            constraintEvaluator = WebConstraintEvaluator { true },
            persistence = persistence,
        )
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!).build()
        val id = wm.enqueue(req)
        // Wait for work to finish (all fire-and-forget persistence coroutines will have run)
        eventually { wm.getWorkInfoById(id)?.isFinished == true }
        eventually { persistence.saveHistory.any { it.id == req.id } }
        assertTrue(persistence.saveHistory.any { it.id == req.id })
    }

    @Test
    fun persistence_delete_isCalledOnTerminalState() = runTest {
        val persistence = InMemoryWorkPersistence()
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            constraintEvaluator = WebConstraintEvaluator { true },
            persistence = persistence,
        )
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!).build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        eventually { persistence.deleted.contains(id) }
        assertTrue(persistence.deleted.contains(id))
    }

    @Test
    fun persistence_restore_reEnqueuesInterruptedWork() = runTest {
        val persistence = InMemoryWorkPersistence()
        // Pre-seed persistence with a RUNNING work (simulating page reload mid-execution)
        val id = Uuid.random()
        persistence.seed(WorkInfo(id = id, state = WorkInfo.State.RUNNING, tags = setOf("restore-test")))
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            constraintEvaluator = WebConstraintEvaluator { true },
            persistence = persistence,
        )
        // Give restore time to run
        eventually { wm.getWorkInfoById(id) != null }
        val info = wm.getWorkInfoById(id)
        assertEquals(WorkInfo.State.ENQUEUED, info?.state)
    }

    @Test
    fun persistence_restore_keepsFinalStates() = runTest {
        val persistence = InMemoryWorkPersistence()
        val id = Uuid.random()
        persistence.seed(WorkInfo(id = id, state = WorkInfo.State.SUCCEEDED, tags = setOf("history")))
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            constraintEvaluator = WebConstraintEvaluator { true },
            persistence = persistence,
        )
        eventually { wm.getWorkInfoById(id) != null }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
    }

    @Test
    fun enqueue_withUnsatisfiedConstraint_executesWhenConstraintSatisfied_viaEvaluator() = runTest {
        // Verifies that work waits on constraints and eventually executes when they are met.
        // (The online-watcher path on JVM uses emptyFlow so polling is the mechanism here.)
        var evaluateCount = 0
        val wm = WebWorkManager(
            workerFactory = TestWebWorkerFactory,
            config = WebWorkManagerConfig(constraintCheckIntervalMs = 50),
            constraintEvaluator = WebConstraintEvaluator { evaluateCount++ >= 3 },
        )
        val req = OneTimeWorkRequestBuilder<SuccessWebWorker>(SuccessWebWorker::class.simpleName!!)
            .setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
            .build()
        val id = wm.enqueue(req)
        eventually { wm.getWorkInfoById(id)?.state == WorkInfo.State.SUCCEEDED }
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoById(id)?.state)
        assertTrue(evaluateCount >= 3)
    }

    @Test
    fun enqueueUniquePeriodicWork_keep_returnsExistingIdWithoutCreatingNew() = runTest {
        val wm = workManager()
        val req1 = PeriodicWorkRequestBuilder<SlowWebWorker>(
            SlowWebWorker::class.simpleName!!,
            repeatInterval = 5.minutes,
        ).build()
        val req2 = PeriodicWorkRequestBuilder<SlowWebWorker>(
            SlowWebWorker::class.simpleName!!,
            repeatInterval = 5.minutes,
        ).build()
        val id1 = wm.enqueueUniquePeriodicWork("keep-work", ExistingPeriodicWorkPolicy.KEEP, req1)
        // KEEP: second enqueue must return the first id unchanged
        val id2 = wm.enqueueUniquePeriodicWork("keep-work", ExistingPeriodicWorkPolicy.KEEP, req2)
        assertEquals(id1, id2, "KEEP should return the existing work id")
        // req2 must never have been started
        assertNull(wm.getWorkInfoById(req2.id))
        wm.shutdown()
    }

    @Test
    fun enqueueUniquePeriodicWork_update_replacesExistingWork() = runTest {
        val wm = workManager()
        val req1 = PeriodicWorkRequestBuilder<SlowWebWorker>(
            SlowWebWorker::class.simpleName!!,
            repeatInterval = 5.minutes,
        ).build()
        val req2 = PeriodicWorkRequestBuilder<SuccessWebWorker>(
            SuccessWebWorker::class.simpleName!!,
            repeatInterval = 5.minutes,
        ).build()
        wm.enqueueUniquePeriodicWork("update-work", ExistingPeriodicWorkPolicy.REPLACE, req1)
        eventually { wm.getWorkInfoById(req1.id)?.state == WorkInfo.State.RUNNING }
        // UPDATE cancels the existing work and enqueues the new one
        wm.enqueueUniquePeriodicWork("update-work", ExistingPeriodicWorkPolicy.UPDATE, req2)
        eventually { wm.getWorkInfoById(req1.id)?.isFinished == true }
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(req1.id)?.state)
        wm.shutdown()
    }

    @Test
    fun isWebWorkManagerSupported_returnsFalseOnJvm() = runTest {
        // JVM actual always returns false — this test only runs on the jvmTest source set.
        // The function returns true on JS/WasmJs targets where it is used in production.
        assertEquals(false, isWebWorkManagerSupported())
    }

    @Test
    fun enqueueUniquePeriodicWork_replace_cancelsExisting() = runTest {
        val wm = workManager()
        // Use SlowWebWorker so req1 is still running when req2 triggers REPLACE cancel
        val req1 = PeriodicWorkRequestBuilder<SlowWebWorker>(
            SlowWebWorker::class.simpleName!!,
            repeatInterval = 5.minutes,
        ).build()
        val req2 = PeriodicWorkRequestBuilder<SlowWebWorker>(
            SlowWebWorker::class.simpleName!!,
            repeatInterval = 5.minutes,
        ).build()
        wm.enqueueUniquePeriodicWork("unique-work", ExistingPeriodicWorkPolicy.REPLACE, req1)
        // Wait until req1 is running before enqueuing req2 with REPLACE
        eventually { wm.getWorkInfoById(req1.id)?.state == WorkInfo.State.RUNNING }
        wm.enqueueUniquePeriodicWork("unique-work", ExistingPeriodicWorkPolicy.REPLACE, req2)
        eventually { wm.getWorkInfoById(req1.id)?.isFinished == true }
        assertEquals(WorkInfo.State.CANCELLED, wm.getWorkInfoById(req1.id)?.state)
        wm.shutdown()
    }
}

// ── Test workers ──────────────────────────────────────────────────────────────

class SuccessWebWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}

class FailWebWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.failure("fail")
}

class SlowWebWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        delay(30_000)
        return WorkResult.success()
    }
}

class ProgressWebWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        setProgress(WorkProgress(50))
        setProgress(WorkProgress(100))
        return WorkResult.success()
    }
}

class RetryThenSucceedWebWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        callCount++
        return if (callCount >= 3) WorkResult.success() else WorkResult.retry()
    }

    companion object {
        var callCount = 0
    }
}

// ── In-memory persistence test double ────────────────────────────────────────

internal class InMemoryWorkPersistence : WebWorkPersistence {
    private val mutex = Mutex()
    private val _saved = mutableListOf<WorkInfo>()
    private val _deleted = mutableSetOf<Uuid>()
    private val _saveHistory = mutableListOf<WorkInfo>()

    val deleted: Set<Uuid> get() = _deleted.toSet()
    val saveHistory: List<WorkInfo> get() = _saveHistory.toList()

    // Pre-seed persistence before constructing WebWorkManager (simulates prior page session)
    fun seed(info: WorkInfo) { _saved.add(info) }

    override suspend fun save(info: WorkInfo): Unit = mutex.withLock {
        _saved.removeAll { it.id == info.id }
        _saved.add(info)
        _saveHistory.add(info)
    }

    override suspend fun loadAll(): List<WorkInfo> = mutex.withLock { _saved.toList() }

    override suspend fun delete(id: Uuid): Unit = mutex.withLock {
        _saved.removeAll { it.id == id }
        _deleted.add(id)
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

private object TestWebWorkerFactory : WebWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): io.github.mobilebytelabs.worker.CoroutineWorker =
        when (workerClass) {
            SuccessWebWorker::class.simpleName -> SuccessWebWorker(context)
            FailWebWorker::class.simpleName -> FailWebWorker(context)
            SlowWebWorker::class.simpleName -> SlowWebWorker(context)
            ProgressWebWorker::class.simpleName -> ProgressWebWorker(context)
            RetryThenSucceedWebWorker::class.simpleName -> RetryThenSucceedWebWorker(context)
            else -> throw IllegalArgumentException("Unknown worker: $workerClass")
        }
}

// ── Test helper ───────────────────────────────────────────────────────────────

private suspend fun eventually(timeoutMs: Long = 2_000, intervalMs: Long = 30, condition: suspend () -> Boolean) {
    withContext(Dispatchers.Default) {
        val mark = kotlin.time.TimeSource.Monotonic.markNow()
        while (mark.elapsedNow().inWholeMilliseconds < timeoutMs) {
            if (condition()) return@withContext
            delay(intervalMs)
        }
    }
}
