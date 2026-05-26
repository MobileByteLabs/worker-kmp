package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.PlatformWorkManager

fun initIosWorkManager(workerFactory: IosWorkerFactory) {
    val manager = IosWorkManager(workerFactory)
    PlatformWorkManager.configure(manager)
}
