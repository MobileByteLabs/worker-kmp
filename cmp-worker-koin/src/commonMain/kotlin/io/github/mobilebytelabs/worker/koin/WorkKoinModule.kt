package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.WorkerRegistry
import io.github.mobilebytelabs.worker.registry.workerRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module factory that wires worker-kmp into the consumer's DI graph.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor). The new signature accepts an
 * explicit [WorkManagerFactory] supplied by the platform module the consumer added
 * (`cmp-worker-android` / `cmp-worker-ios` / `cmp-worker-desktop` / `cmp-worker-web`).
 *
 * **Clean-break refactor**: the legacy `PlatformWorkManager.configure(...)` global slot
 * and `initializeWorkerXxx(...)` side-effecting init functions have been REMOVED outright.
 * Consumers wire the platform implementation via the factory parameter — there is no
 * pre-`startKoin` setup step anymore.
 *
 * Consumer usage (100% commonMain except the factory selection):
 *
 * ```kotlin
 * // androidMain
 * startKoin {
 *     androidContext(this@App)
 *     modules(
 *         workKoinModule(
 *             config = WorkerConfig(),
 *             workers = workerRegistry {
 *                 register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
 *             },
 *             factory = androidWorkManagerFactory(this@App),
 *         ),
 *         appModule,
 *     )
 * }
 *
 * // iosMain
 * startKoin {
 *     modules(
 *         workKoinModule(
 *             config = WorkerConfig(),
 *             workers = workerRegistry {
 *                 register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
 *             },
 *             factory = iosWorkManagerFactory(),
 *         ),
 *     )
 * }
 * ```
 *
 * The factory parameter is mandatory — consumers cannot accidentally start Koin without a
 * platform backend (the previous footgun where `workKoinModule()` returned a
 * `WorkManager` that threw on first use is gone).
 *
 * **Source-compat break from v3.0.0-alpha00**: the previous default-everything signature
 * `workKoinModule(WorkerConfig, WorkerRegistry)` now requires a third positional
 * [WorkManagerFactory] arg. Migration is a per-platform 1-line edit at the consumer's
 * `startKoin` site — see MIGRATION_FROM_2_x.md §2.
 */
public fun workKoinModule(
    config: WorkerConfig = WorkerConfig(),
    workers: WorkerRegistry = workerRegistry { },
    factory: WorkManagerFactory,
): Module = module {
    workers.lock() // Immutable after this point — defends against T23 (per SECURITY.md)
    single { config }
    single { workers }
    single<WorkManager> { factory.create(get(), get()) }
}
