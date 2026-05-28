package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.registry.WorkerRegistry

/**
 * Constructs a [WorkManagerFactory] that builds a [WebWorkManager] from the
 * [io.github.mobilebytelabs.worker.config.WorkerConfig.webConfig] sub-object. Pass the
 * result as the `factory` parameter to `workKoinModule(...)`.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor). REPLACES the legacy
 * `initWebWorkManager(...)` side-effecting init function and the
 * `PlatformWorkManager.configure(...)` global slot — clean break, no v2-style auto-init.
 * Now lives in commonMain because both JS and Wasm targets share the same factory shape;
 * the per-target diverging support (constraint evaluator, persistence, online watcher)
 * resolves through the existing `expect`/`actual` declarations.
 *
 * ```kotlin
 * fun main() {
 *     if (!isWebWorkManagerSupported()) return
 *     startKoin {
 *         modules(
 *             workKoinModule(
 *                 config = WorkerConfig(
 *                     webConfig = WebWorkerConfig(enablePersistence = false),
 *                 ),
 *                 workers = workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } },
 *                 factory = webWorkManagerFactory(),
 *             ),
 *         )
 *     }
 * }
 * ```
 *
 * Threads the consumer's [WorkerRegistry] through a [WebWorkerFactory] adapter — workers
 * register once in commonMain. The platform-specific [WebConstraintEvaluator] +
 * [WebWorkPersistence] resolve through the existing per-target `expect`/`actual` shims.
 */
@ExperimentalWorkerApi
public fun webWorkManagerFactory(): WorkManagerFactory = WorkManagerFactory { config, workers ->
    WebWorkManager(
        workerFactory = WorkerRegistryWebAdapter(workers),
        config = WebWorkManagerConfig(
            constraintCheckIntervalMs = config.webConfig.constraintCheckIntervalMs,
            enablePersistence = config.webConfig.enablePersistence,
            persistenceDbName = config.webConfig.persistenceDbName,
            enableBackgroundSync = config.webConfig.enableBackgroundSync,
            serviceWorkerScript = config.webConfig.serviceWorkerScript,
            enablePeriodicBackgroundSync = config.webConfig.enablePeriodicBackgroundSync,
        ),
    )
}

/**
 * Bridges a commonMain [WorkerRegistry] to the Web-specific [WebWorkerFactory] contract.
 * Throws on unregistered worker class — Web has no reflection fallback available.
 */
@ExperimentalWorkerApi
internal class WorkerRegistryWebAdapter(private val registry: WorkerRegistry) : WebWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
        registry.create(workerClass, context)
            ?: error(
                "Worker '$workerClass' not registered in WorkerRegistry. " +
                    "Add it via workerRegistry { register<$workerClass> { ctx -> $workerClass(ctx) } } " +
                    "before passing the registry to workKoinModule(...).",
            )
}
