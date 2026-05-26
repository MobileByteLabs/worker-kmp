package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.PlatformWorkManager

/**
 * Registers DesktopWorkManager as the platform WorkManager implementation.
 * Call once in your application's `main()` before any work is enqueued.
 *
 * ```kotlin
 * fun main() {
 *     initializeWorkerDesktop()
 *     // ...
 * }
 * ```
 */
fun initializeWorkerDesktop(config: DesktopWorkManagerConfig = DesktopWorkManagerConfig.DEFAULT) {
    PlatformWorkManager.configure(DesktopWorkManager(config))
}
