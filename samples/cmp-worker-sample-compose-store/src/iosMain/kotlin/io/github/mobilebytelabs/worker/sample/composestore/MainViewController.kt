package io.github.mobilebytelabs.worker.sample.composestore

import io.github.mobilebytelabs.worker.ios.iosWorkManagerFactory
import io.github.mobilebytelabs.worker.ios.workerKmpMainViewController
import io.github.mobilebytelabs.worker.sample.composestore.di.sampleKoinModules
import io.github.mobilebytelabs.worker.sample.composestore.ui.SampleApp
import platform.UIKit.UIViewController

/**
 * iOS entry point — exposed to Swift as `MainViewControllerKt.MainViewController()`
 * and consumed via SwiftUI's [UIViewControllerRepresentable] in `iosApp/`.
 */
@Suppress("FunctionName")
fun MainViewController(): UIViewController = workerKmpMainViewController(
    koinModules = { sampleKoinModules(iosWorkManagerFactory()) },
) { SampleApp() }
