package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger

@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    // alpha01: log-only stub. alpha01.X delivers persistent Service Worker integration
    // + Notification API wiring.
    Logger.withTag("worker-kmp").i {
        "ForegroundWorker promotion requested on Web/JS (stub — alpha01.X delivers persistent SW). " +
            "title=${info.title} progress=${info.progress.progress}"
    }
}
