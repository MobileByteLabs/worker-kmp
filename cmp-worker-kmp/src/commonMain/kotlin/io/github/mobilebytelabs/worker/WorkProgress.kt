package io.github.mobilebytelabs.worker

/**
 * Intermediate progress reported by a [CoroutineWorker] during execution.
 *
 * Workers call [CoroutineWorker.setProgress] to push updates; observers receive them
 * via [WorkInfo.progress] in the [WorkManager.getWorkInfosByTag] flow.
 *
 * ```kotlin
 * // Inside doWork():
 * setProgress(WorkProgress.of(25))
 * processChunk1()
 * setProgress(WorkProgress.of(50))
 * processChunk2()
 * setProgress(WorkProgress.COMPLETE)
 * ```
 *
 * @property progress completion percentage in the range 0–100 (inclusive).
 *   A value of `0` signals indeterminate progress.
 * @property data optional key-value payload accompanying this progress update;
 *   useful for passing partial results to the UI.
 */
data class WorkProgress(val progress: Int, val data: WorkData = WorkData.EMPTY) {
    init {
        require(progress in 0..100) { "Progress must be 0..100, was $progress" }
    }

    /** `true` when [progress] is `0`, indicating no deterministic percentage is available. */
    val isIndeterminate: Boolean get() = progress == 0

    /** `true` when [progress] is `100`. */
    val isComplete: Boolean get() = progress == 100

    companion object {
        /** Initial state before any progress has been reported. [isIndeterminate] is `true`. */
        val NONE: WorkProgress = WorkProgress(0)

        /** Convenience sentinel for 100 % completion. */
        val COMPLETE: WorkProgress = WorkProgress(100)

        /**
         * Creates a [WorkProgress] at [percent] with optional associated [data].
         *
         * @param percent completion percentage (0–100).
         * @param data optional payload to surface alongside the percentage.
         * @throws IllegalArgumentException if [percent] is outside 0–100.
         */
        fun of(percent: Int, data: WorkData = WorkData.EMPTY): WorkProgress = WorkProgress(percent, data)
    }
}
