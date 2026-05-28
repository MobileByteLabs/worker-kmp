package io.github.mobilebytelabs.worker.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Launch a Compose Multiplatform Desktop window backed by worker-kmp.
 *
 * Consumer usage — a complete `fun main` becomes a single call:
 *
 * ```
 * fun main() = launchDesktopWorkerApp(
 *     title = "My App",
 *     koinModules = { listOf(appModule(), workModule(desktopWorkManagerFactory())) },
 * ) {
 *     App()
 * }
 * ```
 *
 * The library handles the boilerplate every desktop consumer would otherwise repeat:
 * - Idempotent Koin start via [startWorkerKoinIfAbsent] (safe to call multiple times).
 * - `application { Window(onCloseRequest = ::exitApplication, title) { content() } }`.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md` AC5.
 *
 * @param title Title bar text for the desktop window.
 * @param koinModules Lambda producing the Koin modules to register on first launch.
 *                    Invoked lazily so module construction (which may capture platform
 *                    factories like `desktopWorkManagerFactory()`) is deferred until Koin
 *                    actually needs them.
 * @param content Compose UI composable to render inside the window.
 */
public fun launchDesktopWorkerApp(title: String, koinModules: () -> List<Module>, content: @Composable () -> Unit) {
    startWorkerKoinIfAbsent(koinModules)
    application {
        Window(onCloseRequest = ::exitApplication, title = title) {
            content()
        }
    }
}

/**
 * Idempotent Koin start helper.
 *
 * Public for test access — production callers should prefer [launchDesktopWorkerApp].
 * Returns silently if Koin's [GlobalContext] is already populated, so the same launcher
 * function can run in test fixtures that pre-initialize Koin without throwing.
 */
public fun startWorkerKoinIfAbsent(koinModules: () -> List<Module>) {
    if (GlobalContext.getOrNull() == null) {
        startKoin { modules(koinModules()) }
    }
}
