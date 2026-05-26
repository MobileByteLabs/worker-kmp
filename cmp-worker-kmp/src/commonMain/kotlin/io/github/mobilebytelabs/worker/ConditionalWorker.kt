package io.github.mobilebytelabs.worker

/**
 * A [CoroutineWorker] subclass that gates execution on a runtime condition check.
 *
 * Override [condition] to return `false` whenever the work should be skipped — for example,
 * when a feature flag is disabled, the user is not authenticated, or a required resource is
 * unavailable. The default [doWork] implementation calls [condition] once per execution attempt:
 * if it returns `false` the worker returns [WorkResult.failure] without invoking [doConditionalWork].
 *
 * For **transient** conditions (network unavailable, rate-limited API), return [WorkResult.retry]
 * from [doConditionalWork] instead — the scheduler retries according to the request's [RetryConfig].
 *
 * ```kotlin
 * class FeatureSyncWorker(context: WorkerContext) : ConditionalWorker(context) {
 *
 *     override suspend fun condition(): Boolean =
 *         FeatureFlags.isEnabled("premium_sync") && AuthManager.isSignedIn()
 *
 *     override suspend fun doConditionalWork(): WorkResult {
 *         return if (networkService.isReachable()) {
 *             val result = apiService.sync()
 *             WorkResult.success(workDataOf("synced" to result.count))
 *         } else {
 *             WorkResult.retry(reason = "network unavailable")
 *         }
 *     }
 * }
 * ```
 *
 * @param context platform-provided execution context (ID, inputData, tags, progress channel).
 * @see CoroutineWorker
 * @see WorkResult
 */
abstract class ConditionalWorker(context: WorkerContext) : CoroutineWorker(context) {

    /**
     * Returns `true` when the work should proceed; `false` to skip this execution attempt.
     *
     * Called once per attempt, before [doConditionalWork]. A `false` return causes the worker
     * to emit [WorkResult.failure] — the work transitions to [WorkInfo.State.FAILED] and will
     * not be retried regardless of [RetryConfig].
     *
     * To defer rather than permanently fail, throw an exception or return [WorkResult.retry]
     * from [doConditionalWork].
     */
    abstract suspend fun condition(): Boolean

    /**
     * Executes the actual work when [condition] returned `true`.
     *
     * Behaves identically to [CoroutineWorker.doWork] — return [WorkResult.success],
     * [WorkResult.failure], or [WorkResult.retry] as appropriate.
     */
    abstract suspend fun doConditionalWork(): WorkResult

    final override suspend fun doWork(): WorkResult {
        if (!condition()) return WorkResult.failure("Condition not satisfied — work skipped.")
        return doConditionalWork()
    }
}
