package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.PlatformWorkManager

fun initWebWorkManager(workerFactory: WebWorkerFactory) {
    val manager = WebWorkManager(workerFactory)
    PlatformWorkManager.configure(manager)
}
