package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

/**
 * Snapshot of a single work unit's current state and output.
 *
 * Obtain instances via [WorkManager.getWorkInfoById] (one-shot) or
 * [WorkManager.getWorkInfosByTag] (reactive flow).
 *
 * @property id the unique identifier assigned when the work was enqueued.
 * @property state the current lifecycle state of the work unit.
 * @property progress intermediate progress reported by the worker via
 *   [CoroutineWorker.setProgress]; [WorkProgress.NONE] until the worker calls it.
 * @property outputData key-value data returned by the worker in [WorkResult.Success] or
 *   [WorkResult.Failure]; [WorkData.EMPTY] while the work has not finished.
 * @property tags the set of labels attached to this work unit at creation time.
 * @property runAttemptCount number of times this worker has started execution, including
 *   the current attempt if [state] is [State.RUNNING].
 */
data class WorkInfo(
    val id: Uuid,
    val state: State,
    val progress: WorkProgress = WorkProgress.NONE,
    val outputData: WorkData = WorkData.EMPTY,
    val tags: Set<String> = emptySet(),
    val runAttemptCount: Int = 0,
) {
    /**
     * Lifecycle state of a work unit.
     *
     * Typical one-time flow: ENQUEUED → RUNNING → SUCCEEDED (or FAILED / CANCELLED).
     * A unit may remain BLOCKED when it is part of a [WorkContinuation] chain and
     * predecessor work has not yet completed.
     */
    enum class State {
        /** Scheduled and waiting for constraints to be met or for a free executor slot. */
        ENQUEUED,

        /** Currently executing inside a [CoroutineWorker.doWork] call. */
        RUNNING,

        /** [CoroutineWorker.doWork] returned [WorkResult.Success]. Terminal. */
        SUCCEEDED,

        /** [CoroutineWorker.doWork] returned [WorkResult.Failure] or threw an exception. Terminal. */
        FAILED,

        /** Cancelled via [WorkManager.cancelWorkById] or [WorkManager.cancelAllWorkByTag]. Terminal. */
        CANCELLED,

        /** Waiting for prerequisite work in a [WorkContinuation] chain to finish. */
        BLOCKED,
        ;

        /** `true` for the three terminal states: [SUCCEEDED], [FAILED], and [CANCELLED]. */
        val isFinished: Boolean
            get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
    }

    /** `true` when this work unit has reached a terminal state and will not execute again. */
    val isFinished: Boolean get() = state.isFinished
}
