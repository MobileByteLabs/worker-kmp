package io.github.mobilebytelabs.worker.sample.workers

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.delay

class ImageResizeWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val src = inputData.getString("src") ?: return WorkResult.failure("missing src")
        val width = inputData.getInt("width")
        val height = inputData.getInt("height")

        setProgress(WorkProgress(0))
        delay(80)
        setProgress(WorkProgress(33))
        delay(80)
        setProgress(WorkProgress(67))
        delay(80)
        setProgress(WorkProgress(100))

        val dst = src.replace(".", "_${width}x$height.")
        return WorkResult.success(workDataOf("dst" to dst, "width" to width, "height" to height))
    }
}

class DataSyncWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        delay(100)
        val attempt = inputData.getInt("attempt_hint", 0)
        return if (callCount.incrementAndGet() < 3) {
            WorkResult.retry("simulated network timeout (attempt ${callCount.get()})")
        } else {
            WorkResult.success(workDataOf("recordCount" to 42, "endpoint" to "https://api.example.com/sync"))
        }
    }

    companion object {
        val callCount = java.util.concurrent.atomic.AtomicInteger(0)
    }
}

class CacheCleanupWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        delay(50)
        val freedBytes = (1024..8192).random() * 1024L
        return WorkResult.success(workDataOf("freedBytes" to freedBytes, "run" to runCount.incrementAndGet()))
    }

    companion object {
        val runCount = java.util.concurrent.atomic.AtomicInteger(0)
    }
}

class LongRunningWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        delay(60_000)
        return WorkResult.success()
    }
}
