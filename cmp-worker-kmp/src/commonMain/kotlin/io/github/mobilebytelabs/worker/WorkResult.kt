package io.github.mobilebytelabs.worker

sealed class WorkResult {
    data class Success(val outputData: WorkData = WorkData.EMPTY) : WorkResult()
    data class Failure(
        val message: String = "",
        val outputData: WorkData = WorkData.EMPTY
    ) : WorkResult()
    data class Retry(val reason: String = "") : WorkResult()

    companion object {
        fun success(outputData: WorkData = WorkData.EMPTY): WorkResult = Success(outputData)
        fun failure(message: String = ""): WorkResult = Failure(message)
        fun retry(reason: String = ""): WorkResult = Retry(reason)
    }
}
