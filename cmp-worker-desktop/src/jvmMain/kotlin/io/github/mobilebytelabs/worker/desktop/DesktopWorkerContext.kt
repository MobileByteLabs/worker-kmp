package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkerContext
import kotlin.uuid.Uuid

internal class DesktopWorkerContext(
    override val id: Uuid,
    override val inputData: WorkData,
    override val tags: Set<String>,
    private val stateStore: DesktopWorkStateStore,
) : WorkerContext {
    override suspend fun setProgress(progress: WorkProgress) {
        stateStore.updateProgress(id, progress)
    }
}
