package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkerContext
import kotlin.uuid.Uuid

internal class WebWorkerContext(
    override val id: Uuid,
    override val inputData: WorkData,
    override val tags: Set<String>,
    private val stateStore: WebWorkStateStore
) : WorkerContext {
    override suspend fun setProgress(progress: WorkProgress) {
        stateStore.updateProgress(id, progress)
    }
}
