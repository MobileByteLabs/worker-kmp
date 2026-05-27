package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.registry.WorkerRegistry
import java.io.File

/**
 * Constructs a [WorkManagerFactory] that builds a [DesktopWorkManager] from the
 * [io.github.mobilebytelabs.worker.config.WorkerConfig.desktopConfig] sub-object. Pass
 * the result as the `factory` parameter to `workKoinModule(...)`.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor). REPLACES the legacy
 * `initializeWorkerDesktop(...)` side-effecting init function and the
 * `PlatformWorkManager.configure(...)` global slot — clean break, no v2-style auto-init.
 *
 * ```kotlin
 * fun main() {
 *     startKoin {
 *         modules(
 *             workKoinModule(
 *                 config = WorkerConfig(
 *                     desktopConfig = DesktopWorkerConfig(persistenceEnabled = false),
 *                 ),
 *                 workers = workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } },
 *                 factory = desktopWorkManagerFactory(),
 *             ),
 *         )
 *     }
 * }
 * ```
 *
 * The factory threads the consumer's [WorkerRegistry] through a [DesktopWorkerFactory]
 * adapter — workers register once in commonMain. When
 * [io.github.mobilebytelabs.worker.config.WorkerConfig.androidConfig.useReflectionFactory]
 * is not the desktop-specific knob (that's Android-only); on desktop, reflection is the
 * fallback when the registry returns null.
 */
public fun desktopWorkManagerFactory(): WorkManagerFactory = WorkManagerFactory { config, workers ->
    val persistencePath = config.desktopConfig.persistencePath?.let { File(it) }
        ?: File(System.getProperty("user.home")).resolve(".worker-kmp")
    DesktopWorkManager(
        config = DesktopWorkManagerConfig(
            maxConcurrentWorkers = config.desktopConfig.maxConcurrentWorkers,
            persistenceEnabled = config.desktopConfig.persistenceEnabled,
            persistencePath = persistencePath,
            constraintCheckIntervalMs = config.desktopConfig.constraintCheckIntervalMs,
        ),
        workerFactory = ChainedDesktopWorkerFactory(workers, ReflectionWorkerFactory),
    )
}

/**
 * Consults [registry] for the worker class; on absence delegates to [fallback] which
 * uses reflection (`Class.forName`). Lets consumers mix registry-backed workers with
 * reflection-instantiated ones during incremental migration.
 */
internal class ChainedDesktopWorkerFactory(
    private val registry: WorkerRegistry,
    private val fallback: DesktopWorkerFactory,
) : DesktopWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
        registry.create(workerClass, context) ?: fallback.create(workerClass, context)
}
