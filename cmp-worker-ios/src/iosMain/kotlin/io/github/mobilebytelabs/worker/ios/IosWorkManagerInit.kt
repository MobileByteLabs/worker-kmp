package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.registry.WorkerRegistry

/**
 * Constructs a [WorkManagerFactory] that builds an [IosWorkManager] from the
 * [io.github.mobilebytelabs.worker.config.WorkerConfig.iosConfig] sub-object. Pass the
 * result as the `factory` parameter to `workKoinModule(...)`.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor). REPLACES the legacy
 * `initIosWorkManager(...)` side-effecting init function and the
 * `PlatformWorkManager.configure(...)` global slot — clean break, no v2-style auto-init.
 *
 * ```kotlin
 * startKoin {
 *     modules(
 *         workKoinModule(
 *             config = WorkerConfig(
 *                 iosConfig = IosWorkerConfig(
 *                     enableBackgroundTasks = true,
 *                     bgProcessingTaskIdentifier = "com.example.sync",
 *                 ),
 *             ),
 *             workers = workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } },
 *             factory = iosWorkManagerFactory(),
 *         ),
 *     )
 * }
 * ```
 *
 * The factory threads the consumer's [WorkerRegistry] through an [IosWorkerFactory]
 * adapter, so workers register once in commonMain rather than via a separate iOS-only
 * factory class.
 */
@ExperimentalWorkerApi
public fun iosWorkManagerFactory(): WorkManagerFactory = WorkManagerFactory { config, workers ->
    val iosConfig = IosWorkManagerConfig(
        enableBackgroundTasks = config.iosConfig.enableBackgroundTasks,
        bgProcessingTaskIdentifier = config.iosConfig.bgProcessingTaskIdentifier,
        enablePersistence = config.iosConfig.enablePersistence,
        persistenceKey = config.iosConfig.persistenceKey,
        appRefreshTaskIdentifier = config.iosConfig.appRefreshTaskIdentifier,
    )
    validateInfoPlist(iosConfig)
    IosWorkManager(
        workerFactory = WorkerRegistryIosAdapter(workers),
        config = iosConfig,
    )
}

/**
 * Bridges a commonMain [WorkerRegistry] to the iOS-specific [IosWorkerFactory] contract.
 * Throws on unregistered worker class — iOS has no reflection fallback available.
 */
@ExperimentalWorkerApi
internal class WorkerRegistryIosAdapter(private val registry: WorkerRegistry) : IosWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
        registry.create(workerClass, context)
            ?: error(
                "Worker '$workerClass' not registered in WorkerRegistry. " +
                    "Add it via workerRegistry { register<$workerClass> { ctx -> $workerClass(ctx) } } " +
                    "before passing the registry to workKoinModule(...).",
            )
}
