package io.github.mobilebytelabs.worker.ios

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

/**
 * Build a [UIViewController] hosting the supplied Compose Multiplatform [content], with
 * Koin started exactly once.
 *
 * Consumer usage — invoked from Swift via Kotlin/Native interop:
 *
 * ```kotlin
 * // iosMain — exposes `MainViewController()` to Swift as `MainViewControllerKt.MainViewController()`
 * fun MainViewController(): UIViewController = workerKmpMainViewController(
 *     koinModules = { listOf(appModule(), workModule(iosWorkManagerFactory())) },
 * ) {
 *     SampleApp()
 * }
 * ```
 *
 * ```swift
 * // iosApp — bridge into SwiftUI
 * struct ComposeView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController {
 *         MainViewControllerKt.MainViewController()
 *     }
 *     func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
 * }
 * ```
 *
 * The Koin start is idempotent — if Koin's default context is already populated
 * (e.g. by a test fixture or a prior MainViewController instance during a SwiftUI
 * hot-reload), the call is a no-op and the existing context is reused.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md` AC6.
 *
 * @param koinModules Lambda producing the Koin modules to register on first launch.
 *                    Invoked lazily so platform factories like `iosWorkManagerFactory()`
 *                    are constructed only when actually needed.
 * @param content Compose Multiplatform UI to render inside the returned UIViewController.
 * @return A fresh [UIViewController] that can be embedded in a SwiftUI
 *         `UIViewControllerRepresentable` or attached directly to a UIKit window.
 */
public fun workerKmpMainViewController(
    koinModules: () -> List<Module>,
    content: @Composable () -> Unit,
): UIViewController {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(koinModules()) }
    }
    return ComposeUIViewController { content() }
}
