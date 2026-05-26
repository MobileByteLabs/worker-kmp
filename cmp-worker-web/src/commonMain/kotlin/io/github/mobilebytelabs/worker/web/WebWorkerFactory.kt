package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.WorkerContext

@ExperimentalWorkerApi
interface WebWorkerFactory {
    fun create(workerClass: String, context: WorkerContext): CoroutineWorker
}
