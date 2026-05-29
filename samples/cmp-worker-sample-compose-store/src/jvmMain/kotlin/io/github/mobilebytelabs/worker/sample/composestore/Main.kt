package io.github.mobilebytelabs.worker.sample.composestore

import io.github.mobilebytelabs.worker.desktop.desktopWorkManagerFactory
import io.github.mobilebytelabs.worker.desktop.launchDesktopWorkerApp
import io.github.mobilebytelabs.worker.sample.composestore.di.sampleKoinModules
import io.github.mobilebytelabs.worker.sample.composestore.ui.SampleApp

/**
 * JVM (desktop) entry point.
 *
 * NOTE: This file mirrors exactly what `worker-kmp-app-plugin`'s
 * `desktop-main.kt.template` would generate from the `@WorkerKmpApp` annotation
 * on `sampleKoinModules` + `@WorkerKmpAppContent` on `SampleApp`. Kept
 * hand-authored here because applying the plugin from a sibling-module build
 * requires `includeBuild` infrastructure not yet in place — sample migration
 * to the plugin tracks as a follow-up to the worker-kmp-app-plugin epic.
 */
fun main() = launchDesktopWorkerApp(
    title = "worker-kmp Store Demo",
    koinModules = { sampleKoinModules(desktopWorkManagerFactory()) },
) { SampleApp() }
