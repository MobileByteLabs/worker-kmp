package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

class TestWorkerContext(
    override val id: Uuid = Uuid.random(),
    override val inputData: WorkData = WorkData.EMPTY,
    override val tags: Set<String> = emptySet()
) : WorkerContext {
    val progressUpdates = mutableListOf<WorkProgress>()

    override suspend fun setProgress(progress: WorkProgress) {
        progressUpdates.add(progress)
    }
}
