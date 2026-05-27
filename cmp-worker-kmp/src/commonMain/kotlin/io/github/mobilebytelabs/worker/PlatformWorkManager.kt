package io.github.mobilebytelabs.worker

import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.WorkerRegistry

/**
 * Per-platform factory contract that constructs a [WorkManager] from a [WorkerConfig] +
 * [WorkerRegistry]. Each platform module (`cmp-worker-android`, `cmp-worker-ios`,
 * `cmp-worker-desktop`, `cmp-worker-web`) exposes a concrete factory the consumer wires
 * into Koin via `workKoinModule(config, workers, factory)`.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor). Replaces the legacy
 * `PlatformWorkManager.configure(...)` global slot pattern — no more global state, no
 * more per-platform `initializeWorkerXxx(...)` side-effecting calls. The factory pattern
 * is preferred over an `expect class` because the per-platform implementations live in
 * separate gradle modules that depend on `cmp-worker-kmp`; the KMP `expect`/`actual`
 * compiler contract requires actuals to live in the same module as the expect declaration,
 * which is not the case here.
 *
 * Consumer applications obtain platform factories from their respective platform modules:
 *
 * ```kotlin
 * // androidMain
 * startKoin {
 *     androidContext(this@App)
 *     modules(
 *         workKoinModule(
 *             config = WorkerConfig(...),
 *             workers = workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } },
 *             factory = androidWorkManagerFactory(this@App),
 *         ),
 *         appModule,
 *     )
 * }
 *
 * // iosMain / desktopMain / webMain
 * startKoin {
 *     modules(
 *         workKoinModule(
 *             config = WorkerConfig(...),
 *             workers = workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } },
 *             factory = iosWorkManagerFactory(),     // or desktopWorkManagerFactory() / webWorkManagerFactory()
 *         ),
 *         appModule,
 *     )
 * }
 * ```
 */
public fun interface WorkManagerFactory {

    /**
     * Construct the platform [WorkManager]. Called once at Koin module load — never
     * twice for a given Koin lifecycle. Implementations may capture platform-side state
     * (Android `Context`, BGTaskScheduler handler registration, etc.) at construction
     * time.
     */
    public fun create(config: WorkerConfig, workers: WorkerRegistry): WorkManager
}
