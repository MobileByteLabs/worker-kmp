package io.github.mobilebytelabs.worker.sample.android

import android.app.Application
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.android.androidWorkManagerFactory
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.koin.workKoinModule
import io.github.mobilebytelabs.worker.registry.workerRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WorkerSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // v3.0.0-alpha00.X deep-refactored API: a single startKoin call wires both the
        // WorkManager backend (via androidWorkManagerFactory) and the consumer registry.
        // No more pre-startKoin initializeWorkerAndroid(...) step — that legacy entry
        // point + the PlatformWorkManager global slot have been removed outright.
        startKoin {
            androidContext(this@WorkerSampleApp)
            modules(
                workKoinModule(
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
