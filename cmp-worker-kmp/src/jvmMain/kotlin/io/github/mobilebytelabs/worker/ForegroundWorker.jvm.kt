package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger

@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    // alpha01: log-only stub. alpha01.X delivers java.awt.SystemTray integration
    // + DesktopForegroundBridge keep-alive coroutine pattern.
    Logger.withTag("worker-kmp").i {
        "ForegroundWorker promotion requested on Desktop (stub — alpha01.X delivers SystemTray integration). " +
            "title=${info.title} progress=${info.progress.progress}"
    }
}
