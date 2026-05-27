package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.PlatformWorkManager

/**
 * Registers DesktopWorkManager as the platform WorkManager implementation.
 * Call once in your application's `main()` before any work is enqueued.
 *
 * ```kotlin
 * fun main() {
 *     initializeWorkerDesktop(
 *         workerFactory = object : DesktopWorkerFactory {
 *             override fun create(workerClass: String, context) = when (workerClass) {
 *                 "SyncWorker" -> SyncWorker(context)
 *                 else         -> error("Unknown worker: $workerClass")
 *             }
 *         },
 *     )
 * }
 * ```
 */
fun initializeWorkerDesktop(
    config: DesktopWorkManagerConfig = DesktopWorkManagerConfig.DEFAULT,
    workerFactory: DesktopWorkerFactory = ReflectionWorkerFactory,
) {
    PlatformWorkManager.configure(DesktopWorkManager(config, workerFactory))
}
