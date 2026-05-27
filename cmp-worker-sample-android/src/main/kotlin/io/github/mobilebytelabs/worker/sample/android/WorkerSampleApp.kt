package io.github.mobilebytelabs.worker.sample.android

import android.app.Application
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.android.initializeWorkerAndroid
import io.github.mobilebytelabs.worker.koin.workKoinModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WorkerSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialise WorkManager with Koin worker factory bridge
        initializeWorkerAndroid(
            context = this,
            workerFactory = KoinAndroidWorkerFactory,
        )
        // Start Koin — workKoinModule provides WorkManager via PlatformWorkManager
        startKoin {
            androidContext(this@WorkerSampleApp)
            modules(workKoinModule, appModule)
        }
    }
}
