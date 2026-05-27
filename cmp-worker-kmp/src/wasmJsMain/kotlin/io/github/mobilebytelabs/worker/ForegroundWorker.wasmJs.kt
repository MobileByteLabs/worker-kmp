package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger

@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    Logger.withTag("worker-kmp").i {
        "ForegroundWorker promotion requested on Web/WasmJs (stub — alpha01.X delivers persistent SW). " +
            "title=${info.title} progress=${info.progress.progress}"
    }
}
