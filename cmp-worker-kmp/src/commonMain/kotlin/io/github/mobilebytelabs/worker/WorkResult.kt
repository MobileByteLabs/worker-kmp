package io.github.mobilebytelabs.worker

/**
 * The outcome of a single [CoroutineWorker.doWork] execution attempt.
 *
 * Return one of the factory functions from your worker:
 * ```kotlin
 * override suspend fun doWork(): WorkResult {
 *     return try {
 *         val output = performWork()
 *         WorkResult.success(workDataOf("result" to output))
 *     } catch (e: TransientException) {
 *         WorkResult.retry(reason = e.message ?: "transient error")
 *     } catch (e: Exception) {
 *         WorkResult.failure(message = e.message ?: "fatal error")
 *     }
 * }
 * ```
 *
 * @see WorkInfo.State
 * @see RetryConfig
 */
sealed class WorkResult {
    /**
     * Work completed successfully.
     *
     * Transitions the work unit to [WorkInfo.State.SUCCEEDED].
     * [outputData] is stored in [WorkInfo.outputData] and available to observers.
     *
     * @property outputData optional result payload; defaults to [WorkData.EMPTY].
     */
    data class Success(val outputData: WorkData = WorkData.EMPTY) : WorkResult()

    /**
     * Work failed permanently and should not be retried.
     *
     * Transitions the work unit to [WorkInfo.State.FAILED].
     * [outputData] is stored in [WorkInfo.outputData] so callers can inspect the failure reason.
     *
     * @property message human-readable failure description; defaults to empty string.
     * @property outputData optional failure payload; defaults to [WorkData.EMPTY].
     */
    data class Failure(val message: String = "", val outputData: WorkData = WorkData.EMPTY) : WorkResult()

    /**
     * Work encountered a transient error and should be retried.
     *
     * The scheduler will re-run the worker according to the [RetryConfig] set on the original
     * [WorkRequest]. Once [RetryConfig.maxAttempts] is exhausted the work transitions to
     * [WorkInfo.State.FAILED].
     *
     * @property reason human-readable description of why a retry is needed; defaults to empty string.
     */
    data class Retry(val reason: String = "") : WorkResult()

    companion object {
        /** Creates a [Success] result with optional [outputData]. */
        fun success(outputData: WorkData = WorkData.EMPTY): WorkResult = Success(outputData)

        /** Creates a [Failure] result with optional human-readable [message]. */
        fun failure(message: String = ""): WorkResult = Failure(message)

        /** Creates a [Retry] result with optional human-readable [reason]. */
        fun retry(reason: String = ""): WorkResult = Retry(reason)
    }
}
