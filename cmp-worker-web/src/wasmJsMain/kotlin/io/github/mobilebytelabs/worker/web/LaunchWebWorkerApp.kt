package io.github.mobilebytelabs.worker.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools
import org.w3c.dom.HTMLElement

/**
 * Launch a Compose Multiplatform Web (wasmJs) app backed by worker-kmp.
 *
 * Consumer usage — a complete `fun main` becomes a single call:
 *
 * ```
 * fun main() = launchWebWorkerApp(
 *     canvasElementId = "composeCanvas",
 *     koinModules = { listOf(appModule(), workModule(webWorkManagerFactory())) },
 * ) {
 *     SampleApp()
 * }
 * ```
 *
 * Requires the consumer ship an `index.html` shell with a viewport container:
 *
 * ```html
 * <div id="composeCanvas" style="width:100vw; height:100vh;"></div>
 * <script src="…wasmJs.js"></script>
 * ```
 *
 * The library handles the boilerplate every web consumer would otherwise repeat:
 * - Idempotent Koin start via [startWorkerKoinIfAbsent] (safe to call multiple times).
 * - `ComposeViewport(viewportContainer) { content() }` — the Compose Multiplatform 1.7+
 *   replacement for the older `CanvasBasedWindow` API.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md` AC7
 * (wasmJs half — paired with the jsMain copy for js(IR) consumers).
 *
 * @param canvasElementId The `id` attribute of the viewport container element in `index.html`.
 * @param koinModules Lambda producing the Koin modules to register on first launch.
 * @param content Compose UI composable to render inside the viewport.
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun launchWebWorkerApp(
    canvasElementId: String,
    koinModules: () -> List<Module>,
    onAfterKoinStart: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    startWorkerKoinIfAbsent(koinModules)
    // worker-kmp v4.0.0 — single-API completion: codegen-emitted main passes
    // `onAfterKoinStart = { WorkerKmpAuto.install() }` so worker registry +
    // WorkManager binding land after Koin's default context is populated.
    onAfterKoinStart()
    val container = document.getElementById(canvasElementId) as? HTMLElement
        ?: error("worker-kmp: viewport container '#$canvasElementId' not found in DOM")
    ComposeViewport(viewportContainer = container) { content() }
}

/**
 * Idempotent Koin start helper (wasmJs).
 *
 * Public for test access. Returns silently if Koin's default context is already populated.
 */
public fun startWorkerKoinIfAbsent(koinModules: () -> List<Module>) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(koinModules()) }
    }
}
