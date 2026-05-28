package io.github.mobilebytelabs.worker.sample.composestore

import io.github.mobilebytelabs.worker.sample.composestore.di.sampleKoinModules
import io.github.mobilebytelabs.worker.sample.composestore.ui.SampleApp
import io.github.mobilebytelabs.worker.web.launchWebWorkerApp
import io.github.mobilebytelabs.worker.web.webWorkManagerFactory

/**
 * Web (wasmJs) entry point — pairs with `src/wasmJsMain/resources/index.html`
 * which declares `<canvas id="composeCanvas">`.
 */
fun main() = launchWebWorkerApp(
    canvasElementId = "composeCanvas",
    koinModules = { sampleKoinModules(webWorkManagerFactory()) },
) { SampleApp() }
