package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkerContext

interface WebWorkerFactory {
    fun create(workerClass: String, context: WorkerContext): CoroutineWorker
}
