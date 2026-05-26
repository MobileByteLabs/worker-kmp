package io.github.mobilebytelabs.worker.compose

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext

class DummyWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.Success()
}
