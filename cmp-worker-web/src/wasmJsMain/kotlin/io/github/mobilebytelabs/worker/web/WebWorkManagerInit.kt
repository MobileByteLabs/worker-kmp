package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.PlatformWorkManager

@ExperimentalWorkerApi
fun initWebWorkManager(
    workerFactory: WebWorkerFactory,
    config: WebWorkManagerConfig = WebWorkManagerConfig.DEFAULT,
) {
    val manager = WebWorkManager(workerFactory = workerFactory, config = config)
    PlatformWorkManager.configure(manager)
}
