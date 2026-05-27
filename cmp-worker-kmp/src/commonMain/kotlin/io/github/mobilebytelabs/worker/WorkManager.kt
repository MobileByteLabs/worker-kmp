package io.github.mobilebytelabs.worker

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/**
 * Central coordinator for background work across platforms.
 *
 * `WorkManager` schedules, tracks, and cancels [CoroutineWorker] tasks described by
 * [WorkRequest] instances. Use [oneTimeWorkRequest] or [periodicWorkRequest] to build
 * requests, then pass them to [enqueue] or [enqueueUniquePeriodicWork].
 *
 * Observe running work via [getWorkInfosByTag] (reactive) or poll with [getWorkInfoById]
 * (snapshot). All suspend functions are safe to call from any coroutine context.
 *
 * Platform notes:
 * - **Android** — backed by `androidx.work.WorkManager`; full OS scheduling + persistence.
 * - **iOS** — NSUserDefaults persistence by default; opt-in OS scheduling via
 *   `BGTaskScheduler` (`enableBackgroundTasks = true` in `IosWorkManagerConfig`).
 *   Requires `@OptIn(ExperimentalWorkerApi::class)`.
 * - **Desktop (JVM)** — file-based persistence under `~/.worker-kmp` by default; no OS scheduling.
 * - **Web (JS/WasmJs)** — IndexedDB persistence; opt-in Browser Background Sync API
 *   (`enableBackgroundSync = true` in `WebWorkManagerConfig`).
 *   Requires `@OptIn(ExperimentalWorkerApi::class)`.
 * - Query capabilities at runtime: [platformBackgroundCapabilities].
 *
 * Example:
 * ```kotlin
 * val id = workManager.enqueue(
 *     oneTimeWorkRequest<SyncWorker> {
 *         setInputData(workDataOf("url" to endpoint))
 *         addTag("sync")
 *     }
 * )
 * workManager.getWorkInfosByTag("sync").collect { infos ->
 *     infos.forEach { println("${it.id}: ${it.state}") }
 * }
 * ```
 *
 * @see CoroutineWorker
 * @see WorkRequest
 * @see WorkInfo
 */
interface WorkManager {
    /**
     * Enqueues a one-time work request and returns its assigned [Uuid].
     *
     * If a request with the same [WorkRequest.id] is already running or enqueued,
     * this call is a no-op and returns the existing ID.
     *
     * @param request the work to schedule.
     * @return the [Uuid] that identifies this work unit; use it with [getWorkInfoById] or
     *   [cancelWorkById].
     * @throws WorkEnqueueException if the platform scheduler rejects the request.
     */
    suspend fun enqueue(request: OneTimeWorkRequest): Uuid

    /**
     * Enqueues a periodic work request with a uniqueness name and conflict policy.
     *
     * Only one periodic work unit with [uniqueWorkName] may exist at a time.
     * [existingPeriodicWorkPolicy] controls what happens when one already exists:
     * - [ExistingPeriodicWorkPolicy.KEEP] — leave the existing work unchanged.
     * - [ExistingPeriodicWorkPolicy.REPLACE] — cancel and re-enqueue with the new request.
     * - [ExistingPeriodicWorkPolicy.UPDATE] — update constraints/interval while preserving state.
     *
     * @param uniqueWorkName globally unique name for this periodic task.
     * @param existingPeriodicWorkPolicy conflict resolution when a task with the same name exists.
     * @param request the periodic work to schedule.
     * @return the [Uuid] of the active (possibly pre-existing) work unit.
     * @throws WorkEnqueueException if the platform scheduler rejects the request.
     */
    suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid

    /**
     * Cancels the work identified by [id].
     *
     * No-op when the work is already in a terminal state ([WorkInfo.State.SUCCEEDED],
     * [WorkInfo.State.FAILED], or [WorkInfo.State.CANCELLED]).
     *
     * @param id the [Uuid] returned by [enqueue] or [enqueueUniquePeriodicWork].
     */
    suspend fun cancelWorkById(id: Uuid)

    /**
     * Cancels all work that was tagged with [tag].
     *
     * Already-finished work is ignored. Work currently running may finish or be interrupted
     * depending on the platform implementation.
     *
     * @param tag the tag string previously passed to [OneTimeWorkRequestBuilder.addTag] or
     *   [PeriodicWorkRequestBuilder.addTag].
     */
    suspend fun cancelAllWorkByTag(tag: String)

    /**
     * Returns a hot [Flow] that emits the current list of [WorkInfo] items matching [tag]
     * whenever their state changes.
     *
     * The flow never completes on its own — cancel the collecting coroutine to stop observing.
     * The first emission contains the current snapshot; subsequent emissions reflect transitions
     * such as ENQUEUED → RUNNING → SUCCEEDED.
     *
     * @param tag the tag to filter by.
     * @return a cold-to-hot shared flow of [WorkInfo] lists.
     */
    fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>>

    /**
     * Returns a point-in-time snapshot of the [WorkInfo] for the given [id], or `null` if
     * no work with that ID is known to this manager.
     *
     * @param id the [Uuid] returned by [enqueue] or [enqueueUniquePeriodicWork].
     * @return the current [WorkInfo], or `null` if not found.
     */
    suspend fun getWorkInfoById(id: Uuid): WorkInfo?
}
