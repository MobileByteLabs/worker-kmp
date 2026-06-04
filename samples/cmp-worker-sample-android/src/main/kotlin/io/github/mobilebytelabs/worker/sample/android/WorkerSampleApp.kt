@file:OptIn(io.github.mobilebytelabs.worker.koin.WorkerKmpInternalApi::class)

package io.github.mobilebytelabs.worker.sample.android

import android.app.Application
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.android.androidWorkManagerFactory
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.koin.workKoinModulePrivateApi
import io.github.mobilebytelabs.worker.registry.workerRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Internal sample for cmp-worker-android.
 *
 * v4.0.0 migration note: this sample's `SyncWorker` is a simple `WorkerContext`-only
 * constructor — the single-API `@WorkerKmpWorkers` path would work cleanly here, but to
 * keep the sample self-contained (avoiding a `@WorkerKmpApp` annotation file + cmp-shared
 * style multi-module setup) we use the `@OptIn(WorkerKmpInternalApi::class)` escape hatch
 * directly. Production consumers should prefer the `@WorkerKmpWorkers` + `WorkerKmpAuto.install()`
 * path documented in `docs/wiki/single-api-guide.md`.
 */
class WorkerSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WorkerSampleApp)
            modules(
                workKoinModulePrivateApi(
                    config = WorkerConfig(),
                    workers = workerRegistry {
                        register<SyncWorker> { ctx: WorkerContext -> SyncWorker(ctx) }
                    },
                    factory = androidWorkManagerFactory(this@WorkerSampleApp),
                ),
                appModule,
            )
        }
    }
}
