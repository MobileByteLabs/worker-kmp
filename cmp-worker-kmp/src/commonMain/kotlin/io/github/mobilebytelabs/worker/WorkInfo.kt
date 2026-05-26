package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

data class WorkInfo(
    val id: Uuid,
    val state: State,
    val progress: WorkProgress = WorkProgress.NONE,
    val outputData: WorkData = WorkData.EMPTY,
    val tags: Set<String> = emptySet(),
    val runAttemptCount: Int = 0,
) {
    enum class State {
        ENQUEUED,
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED,
        BLOCKED,
        ;

        val isFinished: Boolean
            get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
    }

    val isFinished: Boolean get() = state.isFinished
}
