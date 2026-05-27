package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.PlatformWorkManager
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.WorkerRegistry
import io.github.mobilebytelabs.worker.registry.workerRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module factory that wires worker-kmp into the consumer's DI graph.
 *
 * Added in v3.0.0-alpha00. Replaces the legacy 1-arg `workKoinModule` singleton
 * which required per-platform initializeWorkerXxx(...) calls BEFORE startKoin.
 *
 * Consumer usage (100% commonMain):
 * ```kotlin
 * startKoin {
 *     modules(
 *         workKoinModule(
 *             config = WorkerConfig(),
 *             workers = workerRegistry {
 *                 register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
 *             },
 *         ),
 *         appModule,
 *     )
 * }
 * ```
 *
 * **Backward compatibility**: in v3.0.0-alpha00, the per-platform actuals (Android/
 * iOS/Desktop/Web) STILL require `initializeWorkerXxx()` to be called first. The new
 * module shape signals consumer intent + carries the [WorkerConfig] + [WorkerRegistry]
 * into the Koin graph for FUTURE per-actual refactors. Full zero-init wiring lands
 * per-actual in v3.0.0-alpha00.X follow-ups.
 *
 * Until the per-actual refactors complete, calling `workKoinModule(...)` WITHOUT a
 * preceding `initializeWorkerXxx()` yields a [WorkManager] that throws on first use.
 * This is documented + emits a kermit WARN log at module-load time (follow-up).
 *
 * **Source-compat break from v2.x**: the previous `val workKoinModule: Module`
 * is replaced with this function. Migration is a 1-line edit — append `()` parens
 * at the call site. See MIGRATION_FROM_2_x.md.
 */
public fun workKoinModule(
    config: WorkerConfig = WorkerConfig(),
    workers: WorkerRegistry = workerRegistry { },
): Module = module {
    workers.lock() // Immutable after this point — defends against T23 (per SECURITY.md)
    single { config }
    single { workers }
    single<WorkManager> { PlatformWorkManager() }
}
