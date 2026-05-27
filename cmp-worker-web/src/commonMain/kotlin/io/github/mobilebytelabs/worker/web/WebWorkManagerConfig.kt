package io.github.mobilebytelabs.worker.web

data class WebWorkManagerConfig(
    val constraintCheckIntervalMs: Long = 5_000,
    /** Set to `false` to skip IndexedDB persistence (work survives only for the current page session). */
    val enablePersistence: Boolean = true,
    /** Name of the IndexedDB database. Override when multiple apps share the same origin. */
    val persistenceDbName: String = "worker-kmp",
    /** Set to `true` to register a Browser Background Sync tag when network-constrained work is waiting.
     * Requires a Service Worker at [serviceWorkerScript]. Falls back to polling when the API is unavailable. */
    val enableBackgroundSync: Boolean = false,
    /** Path (relative to origin) where the worker-kmp Service Worker script is served.
     * Only used when [enableBackgroundSync] is `true`. */
    val serviceWorkerScript: String = "/worker-kmp-sw.js",
) {
    companion object {
        val DEFAULT = WebWorkManagerConfig()
    }
}
