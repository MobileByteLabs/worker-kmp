package io.github.mobilebytelabs.worker.sample.composestore

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.desktop.desktopWorkManagerFactory
import io.github.mobilebytelabs.worker.sample.composestore.di.appModule
import io.github.mobilebytelabs.worker.sample.composestore.di.articleWorkerKoinModule
import io.github.mobilebytelabs.worker.sample.composestore.domain.Article
import io.github.mobilebytelabs.worker.sample.composestore.store.buildArticlesStore
import io.github.mobilebytelabs.worker.sample.composestore.ui.App
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import org.mobilenativefoundation.store.store5.Store

/**
 * JVM (desktop) entry point.
 *
 * 1. Builds the shared Store5 [Store<String, Article>] once.
 * 2. Starts Koin with two modules:
 *    - `appModule(store)` exposes the Store to the UI graph.
 *    - `articleWorkerKoinModule(store, factory)` wires the WorkManager backend +
 *      registers `ArticleSyncWorker` against the same Store instance.
 * 3. Resolves the [WorkManager] from Koin and hands both Store + WorkManager into the
 *    common `App()` composable.
 *
 * The Store and WorkManager are both shared between Koin's UI graph and the worker
 * registry — when a worker fires, it writes to the same cache the UI is observing.
 */
fun main() = application {
    val store: Store<String, Article> = buildArticlesStore()

    startKoin {
        modules(
            appModule(store),
            articleWorkerKoinModule(
                store = store,
                factory = desktopWorkManagerFactory(),
            ),
        )
    }

    val workManager: WorkManager = getKoin().get()

    Window(
        onCloseRequest = ::exitApplication,
        title = "worker-kmp — Store5 + Koin + Compose demo",
    ) {
        App(workManager = workManager, store = store)
    }
}
