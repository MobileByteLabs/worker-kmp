package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

/**
 * Platform-provided execution context passed to each [CoroutineWorker] instance.
 *
 * `WorkerContext` is an implementation detail of the platform adapters (Android, iOS, Desktop,
 * Web). Application code should not implement this interface directly; access its data
 * through the convenience properties on [CoroutineWorker].
 *
 * @see CoroutineWorker.id
 * @see CoroutineWorker.inputData
 * @see CoroutineWorker.tags
 * @see CoroutineWorker.setProgress
 */
interface WorkerContext {
    /** Unique ID of the [WorkRequest] that spawned this worker execution. */
    val id: Uuid

    /** Key-value data supplied via [WorkRequest.inputData] at enqueue time. */
    val inputData: WorkData

    /** Tags attached to the originating [WorkRequest]. */
    val tags: Set<String>

    /**
     * Reports intermediate progress to [WorkInfo.progress] observers.
     *
     * @param progress completion percentage and optional data payload.
     */
    suspend fun setProgress(progress: WorkProgress)
}
