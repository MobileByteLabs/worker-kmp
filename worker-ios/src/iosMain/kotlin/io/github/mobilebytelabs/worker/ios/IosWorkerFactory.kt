package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkerContext

interface IosWorkerFactory {
    fun create(workerClass: String, context: WorkerContext): CoroutineWorker
}
