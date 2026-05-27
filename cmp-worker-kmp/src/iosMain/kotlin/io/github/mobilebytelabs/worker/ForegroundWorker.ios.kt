package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger

@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    // alpha01: log-only stub. alpha01.X delivers BGContinuedProcessingTaskRequest (iOS 17+)
    // + UNNotifications shim for iOS 13-16.
    Logger.withTag("worker-kmp").i {
        "ForegroundWorker promotion requested on iOS (stub — alpha01.X delivers BGContinuedProcessingTask). " +
            "title=${info.title} progress=${info.progress.progress}"
    }
}
