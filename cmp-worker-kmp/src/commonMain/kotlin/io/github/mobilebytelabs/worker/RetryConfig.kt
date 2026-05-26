package io.github.mobilebytelabs.worker

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 1.minutes,
    val maxDelay: Duration = 1.hours,
    val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    val multiplier: Double = 2.0,
) {
    companion object {
        val DEFAULT = RetryConfig()
        val AGGRESSIVE = RetryConfig(maxAttempts = 10, initialDelay = 30.seconds)
        val CONSERVATIVE = RetryConfig(maxAttempts = 2, initialDelay = 5.minutes)
    }
}

enum class BackoffPolicy {
    EXPONENTIAL,
    LINEAR,
}
