package io.github.mobilebytelabs.worker

class Constraints private constructor(
    val requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
    val requiresStorageNotLow: Boolean = false
) {
    companion object {
        val NONE: Constraints = Constraints()

        operator fun invoke(block: Builder.() -> Unit): Constraints = Builder().apply(block).build()
    }

    class Builder {
        private var requiredNetworkType: NetworkType = NetworkType.NOT_REQUIRED
        private var requiresCharging: Boolean = false
        private var requiresDeviceIdle: Boolean = false
        private var requiresBatteryNotLow: Boolean = false
        private var requiresStorageNotLow: Boolean = false

        fun setRequiredNetworkType(networkType: NetworkType): Builder =
            apply { requiredNetworkType = networkType }

        fun setRequiresCharging(requiresCharging: Boolean): Builder =
            apply { this.requiresCharging = requiresCharging }

        fun setRequiresDeviceIdle(requiresDeviceIdle: Boolean): Builder =
            apply { this.requiresDeviceIdle = requiresDeviceIdle }

        fun setRequiresBatteryNotLow(requiresBatteryNotLow: Boolean): Builder =
            apply { this.requiresBatteryNotLow = requiresBatteryNotLow }

        fun setRequiresStorageNotLow(requiresStorageNotLow: Boolean): Builder =
            apply { this.requiresStorageNotLow = requiresStorageNotLow }

        fun build(): Constraints = Constraints(
            requiredNetworkType = requiredNetworkType,
            requiresCharging = requiresCharging,
            requiresDeviceIdle = requiresDeviceIdle,
            requiresBatteryNotLow = requiresBatteryNotLow,
            requiresStorageNotLow = requiresStorageNotLow
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

enum class NetworkType {
    NOT_REQUIRED,
    CONNECTED,
    UNMETERED,
    NOT_ROAMING,
    METERED
}

enum class ExistingPeriodicWorkPolicy {
    KEEP,
    REPLACE,
    UPDATE
}
