package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.WorkerContext

@ExperimentalWorkerApi
interface IosWorkerFactory {
    fun create(workerClass: String, context: WorkerContext): CoroutineWorker
}
