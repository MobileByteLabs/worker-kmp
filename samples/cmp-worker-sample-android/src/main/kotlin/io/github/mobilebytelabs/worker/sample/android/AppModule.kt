package io.github.mobilebytelabs.worker.sample.android

import org.koin.dsl.module

/**
 * Sample app Koin module. Worker registration now happens inside `workKoinModule(workers = ...)`
 * in [WorkerSampleApp.onCreate]; app-specific bindings live here.
 *
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor) — the previous KoinAndroidWorkerFactory
 * was retired alongside the `initializeWorkerAndroid(...)` entry point. Workers register
 * once via the commonMain [io.github.mobilebytelabs.worker.registry.WorkerRegistry] DSL.
 */
val appModule = module {
    // Add app-specific singletons / factories here.
}
