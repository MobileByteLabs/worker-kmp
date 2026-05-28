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
     * Used when [enableBackgroundSync] or [enablePeriodicBackgroundSync] is `true`. */
    val serviceWorkerScript: String = "/worker-kmp-sw.js",
    /**
     * Set to `true` to register a Browser Periodic Background Sync tag for periodic
     * workers. Requires a Service Worker at [serviceWorkerScript] AND the origin to be
     * a PWA installed by the user (browsers gate the API behind install heuristics).
     * Falls back to polling/timer when the API is unavailable.
     *
     * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
     */
    val enablePeriodicBackgroundSync: Boolean = false,
) {
    companion object {
        val DEFAULT = WebWorkManagerConfig()
    }
}
