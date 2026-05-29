package io.github.mobilebytelabs.worker.sample.composestore.di

import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.app.WorkerKmpApp
import io.github.mobilebytelabs.worker.sample.composestore.store.buildArticlesStore
import org.koin.core.module.Module

/**
 * Single declaration the worker-kmp-app Gradle plugin uses to codegen every
 * per-platform launcher — Android Application + Activity + manifest, JVM
 * `fun main()`, iOS `MainViewController`, wasmJs `fun main()` + `index.html`,
 * iOS Xcode project (via xcodegen). Consumer per-platform Kotlin files: ZERO.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-app-plugin/`
 */
@WorkerKmpApp(
    title = "worker-kmp Store Demo",
    iosBundleId = "io.github.mobilebytelabs.worker.sample.composestore",
    webCanvasId = "composeCanvas",
    androidPermissions = ["android.permission.POST_NOTIFICATIONS"],
)
fun sampleKoinModules(factory: WorkManagerFactory): List<Module> {
    val store = buildArticlesStore()
    return listOf(
        appModule(store),
        articleWorkerKoinModule(store = store, factory = factory),
    )
}
