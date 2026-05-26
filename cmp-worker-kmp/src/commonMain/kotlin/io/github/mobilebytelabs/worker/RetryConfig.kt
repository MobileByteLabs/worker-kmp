package io.github.mobilebytelabs.worker

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Retry strategy applied when [CoroutineWorker.doWork] returns [WorkResult.Retry].
 *
 * The delay before each retry attempt is computed as:
 * - **EXPONENTIAL**: `min(initialDelay * multiplier^(attempt - 1), maxDelay)`
 * - **LINEAR**: `min(initialDelay * attempt, maxDelay)`
 *
 * Once [maxAttempts] is exhausted the work transitions to [WorkInfo.State.FAILED].
 *
 * Use the predefined presets or construct a custom config:
 * ```kotlin
 * val config = RetryConfig(
 *     maxAttempts = 5,
 *     initialDelay = 10.seconds,
 *     maxDelay = 10.minutes,
 *     backoffPolicy = BackoffPolicy.LINEAR,
 * )
 * ```
 *
 * @property maxAttempts total number of execution attempts allowed, including the first run.
 * @property initialDelay delay before the first retry.
 * @property maxDelay upper bound on the computed delay between retries.
 * @property backoffPolicy algorithm used to grow the delay on successive attempts.
 * @property multiplier growth factor for [BackoffPolicy.EXPONENTIAL]; ignored for LINEAR.
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 1.minutes,
    val maxDelay: Duration = 1.hours,
    val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    val multiplier: Double = 2.0,
) {
    companion object {
        /** 3 attempts, 1-minute initial delay, exponential backoff. */
        val DEFAULT = RetryConfig()

        /** 10 attempts, 30-second initial delay — for time-sensitive tasks. */
        val AGGRESSIVE = RetryConfig(maxAttempts = 10, initialDelay = 30.seconds)

        /** 2 attempts, 5-minute initial delay — for tasks that rarely need a retry. */
        val CONSERVATIVE = RetryConfig(maxAttempts = 2, initialDelay = 5.minutes)
    }
}

/**
 * Algorithm used to compute the delay between retry attempts in [RetryConfig].
 */
enum class BackoffPolicy {
    /** Delay grows exponentially: `initialDelay * multiplier^(attempt - 1)`. */
    EXPONENTIAL,

    /** Delay grows linearly: `initialDelay * attempt`. */
    LINEAR,
}
