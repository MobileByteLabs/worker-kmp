package io.github.mobilebytelabs.worker.sample.composestore

import io.github.mobilebytelabs.worker.ios.iosWorkManagerFactory
import io.github.mobilebytelabs.worker.ios.workerKmpMainViewController
import io.github.mobilebytelabs.worker.sample.composestore.di.sampleKoinModules
import io.github.mobilebytelabs.worker.sample.composestore.ui.SampleApp
import platform.UIKit.UIViewController

/**
 * iOS entry point — exposed to Swift as `MainViewControllerKt.MainViewController()`.
 *
 * See [Main] for the same rationale: hand-authored mirror of what
 * `worker-kmp-app-plugin`'s `ios-mainviewcontroller.kt.template` would generate.
 */
@Suppress("FunctionName")
fun MainViewController(): UIViewController = workerKmpMainViewController(
    koinModules = { sampleKoinModules(iosWorkManagerFactory()) },
) { SampleApp() }
