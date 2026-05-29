package io.github.mobilebytelabs.worker.sample.composestore

import io.github.mobilebytelabs.worker.sample.composestore.di.sampleKoinModules
import io.github.mobilebytelabs.worker.sample.composestore.ui.SampleApp
import io.github.mobilebytelabs.worker.web.launchWebWorkerApp
import io.github.mobilebytelabs.worker.web.webWorkManagerFactory

/**
 * Web (wasmJs) entry point.
 *
 * Hand-authored mirror of what `worker-kmp-app-plugin`'s `web-main.kt.template`
 * would generate. See `samples/.../jvmMain/Main.kt` for the rationale.
 */
fun main() = launchWebWorkerApp(
    canvasElementId = "composeCanvas",
    koinModules = { sampleKoinModules(webWorkManagerFactory()) },
) { SampleApp() }
