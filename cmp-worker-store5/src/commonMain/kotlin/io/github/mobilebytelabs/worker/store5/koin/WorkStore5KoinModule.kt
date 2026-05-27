package io.github.mobilebytelabs.worker.store5.koin

import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.store5.StoreRefreshScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module — exposes [StoreRefreshScheduler] resolved from the [WorkManager]
 * provided by `workKoinModule` in `cmp-worker-koin`. Consumer adds alongside:
 *
 * ```kotlin
 * startKoin {
 *     modules(
 *         workKoinModule(config = WorkerConfig(), workers = workerRegistry { /* ... */ }),
 *         workStore5KoinModule,
 *         appModule,
 *     )
 * }
 * ```
 *
 * Order matters: `workKoinModule(...)` MUST be registered before `workStore5KoinModule` so
 * the `single<WorkManager>` it provides can be resolved by the scheduler factory below.
 *
 * Added in v3.0.0-alpha02.
 */
public val workStore5KoinModule: Module = module {
    single { StoreRefreshScheduler(get<WorkManager>()) }
}
