package io.github.mobilebytelabs.worker.sample.android

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.delay

class SyncWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val label = inputData.getString("label") ?: "default"
        setProgress(WorkProgress(10))
        delay(1_000)
        setProgress(WorkProgress(60))
        delay(1_500)
        setProgress(WorkProgress(100))
        return WorkResult.success(workDataOf("label" to label, "synced" to true))
    }
}
