package io.github.mobilebytelabs.worker.sample.android

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.android.KmpAndroidWorkerFactory
import org.koin.core.component.KoinComponent
import org.koin.dsl.module

val appModule = module {
    // Register your workers here for DI-aware instantiation
    factory { (workerClass: String, context: WorkerContext) ->
        createWorker(workerClass, context)
    }
}

private fun createWorker(workerClass: String, context: WorkerContext): CoroutineWorker = when (workerClass) {
    "SyncWorker" -> SyncWorker(context)
    else -> error("Unknown worker: $workerClass — register it in appModule")
}

/** Koin-backed factory passed to [initializeWorkerAndroid]. */
object KoinAndroidWorkerFactory : KmpAndroidWorkerFactory {
    override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
        createWorker(workerClass, context)
}
