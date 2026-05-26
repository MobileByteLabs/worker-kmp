package io.github.mobilebytelabs.worker

/**
 * System conditions that must all be satisfied before a [WorkRequest] is allowed to execute.
 *
 * Construct with the DSL operator or [Builder]:
 * ```kotlin
 * val constraints = Constraints {
 *     setRequiredNetworkType(NetworkType.UNMETERED)
 *     setRequiresBatteryNotLow(true)
 * }
 * ```
 *
 * Use [Constraints.NONE] when no conditions are required (the default for new requests).
 *
 * Platform note: constraint evaluation is best-effort on iOS and Web. Only Android enforces
 * all constraints natively via `androidx.work`.
 *
 * @property requiredNetworkType the network connectivity level required before the worker runs.
 * @property requiresCharging `true` if the device must be charging.
 * @property requiresDeviceIdle `true` if the device must be idle (Android API 23+; ignored elsewhere).
 * @property requiresBatteryNotLow `true` if the battery must be above the system low-battery threshold.
 * @property requiresStorageNotLow `true` if available storage must be above the system low-storage threshold.
 */
class Constraints private constructor(
    val requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
    val requiresStorageNotLow: Boolean = false,
) {
    companion object {
        /** No constraints — work may run immediately regardless of device state. */
        val NONE: Constraints = Constraints()

        /** Creates a [Constraints] instance using the builder DSL. */
        operator fun invoke(block: Builder.() -> Unit): Constraints = Builder().apply(block).build()
    }

    /** Builder for [Constraints]. Prefer the [invoke] DSL operator for concise construction. */
    class Builder {
        private var requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED
        private var requiresCharging: Boolean = false
        private var requiresDeviceIdle: Boolean = false
        private var requiresBatteryNotLow: Boolean = false
        private var requiresStorageNotLow: Boolean = false

        /** Sets the network connectivity level required before the worker starts. */
        fun setRequiredNetworkType(networkType: NetworkType): Builder = apply { requiredNetworkType = networkType }

        /** Requires the device to be connected to a charger before the worker starts. */
        fun setRequiresCharging(requiresCharging: Boolean): Builder = apply { this.requiresCharging = requiresCharging }

        /**
         * Requires the device to be in idle mode before the worker starts.
         *
         * Effective on Android API 23+; ignored on other platforms.
         */
        fun setRequiresDeviceIdle(requiresDeviceIdle: Boolean): Builder =
            apply { this.requiresDeviceIdle = requiresDeviceIdle }

        /** Requires the battery to be above the system low-battery threshold. */
        fun setRequiresBatteryNotLow(requiresBatteryNotLow: Boolean): Builder =
            apply { this.requiresBatteryNotLow = requiresBatteryNotLow }

        /** Requires available storage to be above the system low-storage threshold. */
        fun setRequiresStorageNotLow(requiresStorageNotLow: Boolean): Builder =
            apply { this.requiresStorageNotLow = requiresStorageNotLow }

        /** Constructs the immutable [Constraints]. */
        fun build(): Constraints = Constraints(
            requiredNetworkType = requiredNetworkType,
            requiresCharging = requiresCharging,
            requiresDeviceIdle = requiresDeviceIdle,
            requiresBatteryNotLow = requiresBatteryNotLow,
            requiresStorageNotLow = requiresStorageNotLow,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Constraints) return false
        return requiredNetworkType == other.requiredNetworkType &&
            requiresCharging == other.requiresCharging &&
            requiresDeviceIdle == other.requiresDeviceIdle &&
            requiresBatteryNotLow == other.requiresBatteryNotLow &&
            requiresStorageNotLow == other.requiresStorageNotLow
    }

    override fun hashCode(): Int {
        var result = requiredNetworkType.hashCode()
        result = 31 * result + requiresCharging.hashCode()
        result = 31 * result + requiresDeviceIdle.hashCode()
        result = 31 * result + requiresBatteryNotLow.hashCode()
        result = 31 * result + requiresStorageNotLow.hashCode()
        return result
    }
}

/**
 * Connectivity level required for a [WorkRequest] to execute.
 *
 * Passed to [Constraints.Builder.setRequiredNetworkType].
 */
enum class NetworkType {
    /** No network constraint; work may run offline. */
    NOT_REQUIRED,

    /** Any active network connection is sufficient. */
    CONNECTED,

    /** Requires a Wi-Fi or other unmetered connection. */
    UNMETERED,

    /** Requires a connection that is not roaming. */
    NOT_ROAMING,

    /** Requires a metered (e.g. cellular) connection. */
    METERED,
}

/**
 * Conflict resolution policy for [WorkManager.enqueueUniquePeriodicWork].
 *
 * Controls what happens when a periodic work unit with the same unique name already exists.
 */
enum class ExistingPeriodicWorkPolicy {
    /** Leave the existing work unchanged; the new request is discarded. */
    KEEP,

    /** Cancel the existing work and enqueue the new request in its place. */
    REPLACE,

    /** Update the existing work's constraints and interval, preserving run history. */
    UPDATE,
}
