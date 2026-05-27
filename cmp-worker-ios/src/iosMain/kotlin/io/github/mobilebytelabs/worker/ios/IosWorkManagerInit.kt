package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.PlatformWorkManager

@ExperimentalWorkerApi
fun initIosWorkManager(workerFactory: IosWorkerFactory, config: IosWorkManagerConfig = IosWorkManagerConfig.DEFAULT) {
    val manager = IosWorkManager(workerFactory, config)
    PlatformWorkManager.configure(manager)
}
