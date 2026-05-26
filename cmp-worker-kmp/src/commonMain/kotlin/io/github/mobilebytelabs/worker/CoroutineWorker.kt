package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

/**
 * Base class for all background tasks managed by [WorkManager].
 *
 * Subclass this and override [doWork] to implement your work logic. Return one of the
 * [WorkResult] factory values to signal the outcome to the scheduler.
 *
 * Instances are created by the platform-specific worker factory on each execution attempt.
 * Do not hold references across attempts — treat each [doWork] invocation as independent.
 *
 * Example:
 * ```kotlin
 * class SyncWorker(context: WorkerContext) : CoroutineWorker(context) {
 *     override suspend fun doWork(): WorkResult {
 *         val url = inputData.getString("url") ?: return WorkResult.failure("missing url")
 *         setProgress(WorkProgress.of(0))
 *         fetch(url)
 *         setProgress(WorkProgress.COMPLETE)
 *         return WorkResult.success()
 *     }
 * }
 * ```
 *
 * @param context platform-provided context carrying the request's ID, input data, and tags,
 *   and the [setProgress] channel.
 * @see WorkResult
 * @see WorkProgress
 * @see WorkData
 */
abstract class CoroutineWorker(protected val context: WorkerContext) {
    /** The unique ID of the [WorkRequest] that spawned this worker. */
    val id: Uuid get() = context.id

    /** Key-value data supplied via [WorkRequest.inputData] at enqueue time. */
    val inputData: WorkData get() = context.inputData

    /** Tags attached to the originating [WorkRequest]. */
    val tags: Set<String> get() = context.tags

    /**
     * Reports intermediate progress to observers of [WorkManager.getWorkInfosByTag].
     *
     * Call this at meaningful checkpoints so callers can render progress bars or status text.
     * Has no effect after [doWork] returns.
     *
     * @param progress a [WorkProgress] value in the range 0–100.
     */
    suspend fun setProgress(progress: WorkProgress) = context.setProgress(progress)

    /**
     * Performs the background work for this worker.
     *
     * This function is called on a coroutine scoped to the worker's lifetime. It may be
     * cancelled at any time when the work is cancelled externally; ensure any resources
     * are released via `try/finally` or structured concurrency.
     *
     * Return values:
     * - [WorkResult.Success] — work finished; optional output data is stored in [WorkInfo.outputData].
     * - [WorkResult.Failure] — work failed permanently; will not retry regardless of [RetryConfig].
     * - [WorkResult.Retry] — transient failure; the scheduler will re-run according to [RetryConfig].
     *
     * @return the outcome of this execution attempt.
     */
    abstract suspend fun doWork(): WorkResult
}
