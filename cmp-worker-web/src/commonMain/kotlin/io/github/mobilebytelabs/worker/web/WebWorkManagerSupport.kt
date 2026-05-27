package io.github.mobilebytelabs.worker.web

/**
 * Returns `true` when the current runtime environment can run [WebWorkManager].
 *
 * Always `true` on Kotlin/JS and Kotlin/Wasm targets (browser or Node.js).
 * Always `false` on JVM (used for test infrastructure only — no real web runtime).
 *
 * Use this for progressive enhancement before wiring [webWorkManagerFactory] into Koin:
 * ```kotlin
 * if (isWebWorkManagerSupported()) {
 *     startKoin { modules(workKoinModule(factory = webWorkManagerFactory(), workers = ...)) }
 * } else {
 *     // Fall back to platform-native scheduling or disable background work.
 * }
 * ```
 */
expect fun isWebWorkManagerSupported(): Boolean
