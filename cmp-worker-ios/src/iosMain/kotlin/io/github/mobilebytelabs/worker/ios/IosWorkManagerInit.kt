package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.PlatformWorkManager

@ExperimentalWorkerApi
fun initIosWorkManager(workerFactory: IosWorkerFactory) {
    val manager = IosWorkManager(workerFactory)
    PlatformWorkManager.configure(manager)
}
