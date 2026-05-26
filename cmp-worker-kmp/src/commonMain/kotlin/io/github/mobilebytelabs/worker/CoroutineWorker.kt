package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

abstract class CoroutineWorker(protected val context: WorkerContext) {
    val id: Uuid get() = context.id
    val inputData: WorkData get() = context.inputData
    val tags: Set<String> get() = context.tags

    suspend fun setProgress(progress: WorkProgress) = context.setProgress(progress)

    abstract suspend fun doWork(): WorkResult
}
