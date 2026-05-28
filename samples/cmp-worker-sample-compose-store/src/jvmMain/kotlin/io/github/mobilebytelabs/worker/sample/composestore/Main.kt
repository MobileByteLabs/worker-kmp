package io.github.mobilebytelabs.worker.sample.composestore

import io.github.mobilebytelabs.worker.desktop.desktopWorkManagerFactory
import io.github.mobilebytelabs.worker.desktop.launchDesktopWorkerApp
import io.github.mobilebytelabs.worker.sample.composestore.di.sampleKoinModules
import io.github.mobilebytelabs.worker.sample.composestore.ui.SampleApp

/**
 * JVM (desktop) entry point.
 *
 * Pre-launcher version (commit `8478fea`) was 52 lines (manual Koin start + manual
 * `application { Window { App(workManager, store) } }` + Koin-via-Java helper +
 * factory lookup). Library encapsulation via [launchDesktopWorkerApp] reduces this
 * to a 5-line `fun main`.
 */
fun main() = launchDesktopWorkerApp(
    title = "worker-kmp — Store5 + Koin + Compose demo",
    koinModules = { sampleKoinModules(desktopWorkManagerFactory()) },
) { SampleApp() }
