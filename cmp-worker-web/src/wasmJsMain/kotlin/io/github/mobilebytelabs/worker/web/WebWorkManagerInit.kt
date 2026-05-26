package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.PlatformWorkManager

@ExperimentalWorkerApi
fun initWebWorkManager(workerFactory: WebWorkerFactory) {
    val manager = WebWorkManager(workerFactory)
    PlatformWorkManager.configure(manager)
}
