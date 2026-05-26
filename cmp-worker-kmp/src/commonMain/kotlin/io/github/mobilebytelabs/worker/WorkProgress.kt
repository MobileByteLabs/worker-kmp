package io.github.mobilebytelabs.worker

data class WorkProgress(val progress: Int, val data: WorkData = WorkData.EMPTY) {
    init {
        require(progress in 0..100) { "Progress must be 0..100, was $progress" }
    }

    val isIndeterminate: Boolean get() = progress == 0
    val isComplete: Boolean get() = progress == 100

    companion object {
        val NONE: WorkProgress = WorkProgress(0)
        val COMPLETE: WorkProgress = WorkProgress(100)

        fun of(percent: Int, data: WorkData = WorkData.EMPTY): WorkProgress = WorkProgress(percent, data)
    }
}
