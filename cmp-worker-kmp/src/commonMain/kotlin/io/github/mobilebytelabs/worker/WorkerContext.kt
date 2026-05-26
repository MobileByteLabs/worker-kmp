package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

interface WorkerContext {
    val id: Uuid
    val inputData: WorkData
    val tags: Set<String>
    suspend fun setProgress(progress: WorkProgress)
}
