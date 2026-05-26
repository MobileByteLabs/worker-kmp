package io.github.mobilebytelabs.worker.test

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

/**
 * In-memory [WorkManager] for use in tests.
 *
 * Work is never executed automatically. Call [simulateWorkSuccess], [simulateWorkFailure],
 * [simulateWorkProgress], or [simulateWorkRunning] to drive state transitions manually.
 *
 * ```kotlin
 * val workManager = TestWorkManager()
 *
 * // enqueue
 * val id = workManager.enqueue(OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker").build())
 *
 * // verify
 * assertEquals(1, workManager.enqueuedRequests.size)
 *
 * // drive to success
 * workManager.simulateWorkSuccess(id, workDataOf("count" to 42))
 * assertEquals(WorkInfo.State.SUCCEEDED, workManager.getWorkInfoById(id)?.state)
 * ```
 */
class TestWorkManager : WorkManager {

    private val _enqueuedRequests = mutableListOf<WorkRequest>()
    private val _uniqueWorkNames = mutableListOf<String>()
    private val store = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    /** All requests passed to [enqueue] or [enqueueUniquePeriodicWork], in order. */
    val enqueuedRequests: List<WorkRequest> get() = _enqueuedRequests.toList()

    /** Unique work names registered via [enqueueUniquePeriodicWork], in order. */
    val uniqueWorkNames: List<String> get() = _uniqueWorkNames.toList()

    // ── WorkManager ───────────────────────────────────────────────────────────

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        _enqueuedRequests.add(request)
        putInfo(WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags))
        return request.id
    }

    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest
    ): Uuid {
        if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.REPLACE) {
            store.value.values
                .filter { uniqueWorkName in it.tags && !it.isFinished }
                .forEach { cancelWorkById(it.id) }
        }
        _enqueuedRequests.add(request)
        _uniqueWorkNames.add(uniqueWorkName)
        putInfo(WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags + uniqueWorkName))
        return request.id
    }

    override suspend fun cancelWorkById(id: Uuid) {
        update(id) { if (!isFinished) copy(state = WorkInfo.State.CANCELLED) else this }
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        store.value = store.value.mapValues { (_, info) ->
            if (tag in info.tags && !info.isFinished) info.copy(state = WorkInfo.State.CANCELLED)
            else info
        }
    }

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> =
        store.map { map -> map.values.filter { tag in it.tags } }

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = store.value[id]

    // ── Test control ──────────────────────────────────────────────────────────

    /** Transitions work to RUNNING. */
    fun simulateWorkRunning(id: Uuid) =
        update(id) { copy(state = WorkInfo.State.RUNNING) }

    /** Transitions work to SUCCEEDED with optional output data. */
    fun simulateWorkSuccess(id: Uuid, outputData: WorkData = WorkData.EMPTY) =
        update(id) { copy(state = WorkInfo.State.SUCCEEDED, outputData = outputData) }

    /** Transitions work to FAILED. */
    fun simulateWorkFailure(id: Uuid) =
        update(id) { copy(state = WorkInfo.State.FAILED) }

    /** Updates the progress of work without changing its state. */
    fun simulateWorkProgress(id: Uuid, progress: WorkProgress) =
        update(id) { copy(progress = progress) }

    /** Transitions work back to ENQUEUED (simulates a retry). */
    fun simulateWorkRetry(id: Uuid) =
        update(id) { copy(state = WorkInfo.State.ENQUEUED, runAttemptCount = runAttemptCount + 1) }

    /**
     * Resets all state — clears enqueued requests, unique work names, and all work info.
     * Call in `@AfterTest` / `@BeforeTest` to isolate tests.
     */
    fun reset() {
        _enqueuedRequests.clear()
        _uniqueWorkNames.clear()
        store.value = emptyMap()
    }

    // ── Convenience queries ───────────────────────────────────────────────────

    /** Returns true if any request with the given tag was enqueued. */
    fun hasWorkWithTag(tag: String): Boolean =
        store.value.values.any { tag in it.tags }

    /** Returns the number of requests enqueued with the given tag. */
    fun workCountWithTag(tag: String): Int =
        store.value.values.count { tag in it.tags }

    /** Returns the last enqueued request, or null if none. */
    val lastEnqueuedRequest: WorkRequest? get() = _enqueuedRequests.lastOrNull()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun putInfo(info: WorkInfo) {
        store.value += info.id to info
    }

    private fun update(id: Uuid, transform: WorkInfo.() -> WorkInfo) {
        val info = store.value[id] ?: return
        store.value += id to info.transform()
    }
}
