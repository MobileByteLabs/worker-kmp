package io.github.mobilebytelabs.worker.web

data class WebWorkManagerConfig(
    val constraintCheckIntervalMs: Long = 5_000,
    /** Set to `false` to skip IndexedDB persistence (work survives only for the current page session). */
    val enablePersistence: Boolean = true,
    /** Name of the IndexedDB database. Override when multiple apps share the same origin. */
    val persistenceDbName: String = "worker-kmp",
) {
    companion object {
        val DEFAULT = WebWorkManagerConfig()
    }
}
