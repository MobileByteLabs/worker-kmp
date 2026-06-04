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
 * Renamed from legacy `workKoinModule(config, workers, factory)` in v4.0.0 per the
 * worker-kmp-single-api-completion epic (closes audit gap H1) — the function is preserved
 * with identical body but tagged with the [WorkerKmpInternalApi] opt-in marker so direct
 * consumer use fails at compile time with `@RequiresOptIn(level = ERROR)`.
 *
 * **Codegen is the only intended caller.** Code emitted by `cmp-worker-app-plugin`'s
 * `workerKmpAppCodegenInit{Android,Ios,Desktop,Web}` tasks annotates the file with
 * `@file:OptIn(WorkerKmpInternalApi::class)` and calls this function. Consumer's commonMain
 * code never sees this — they call `WorkerKmpAuto.install()` instead.
 *
 * Migration from v3.1.x:
 * - **Old**: `modules(workKoinModule(WorkerConfig(), workerRegistry { … }, androidWorkManagerFactory(this)))`
 * - **New**: `@WorkerKmpWorkers([…]) fun workerDeclarations() = Unit` + `modules(appKoinModules())` + `WorkerKmpAuto.install()`
 *
 * See https://github.com/MobileByteLabs/worker-kmp/wiki/single-api-guide for full migration.
 *
 * @param config worker-kmp runtime configuration.
 * @param workers registered worker classes (immutable after this call — defends against T23).
 * @param factory per-platform [WorkManagerFactory] selecting the runtime backing
 *   (androidx.work on Android; IosWorkStateStore on iOS; etc.).
 * @return Koin [Module] binding `single<WorkManager>`.
 */
@WorkerKmpInternalApi
public fun workKoinModulePrivateApi(
    config: WorkerConfig = WorkerConfig(),
    workers: WorkerRegistry = workerRegistry { },
    factory: WorkManagerFactory,
): Module = module {
    workers.lock() // Immutable after this point — defends against T23 (per docs/operations/security.md)
    single { config }
    single { workers }
    single<WorkManager> { factory.create(get(), get()) }
}
