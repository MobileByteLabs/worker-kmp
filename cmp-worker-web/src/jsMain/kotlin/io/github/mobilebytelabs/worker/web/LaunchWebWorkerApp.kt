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
 * js(IR) launcher — mirrors the wasmJs API for consumers targeting Kotlin/JS browser.
 *
 * See the wasmJs sibling at `wasmJsMain/.../LaunchWebWorkerApp.kt` for full KDoc + usage.
 * The duplication exists because both targets need distinct source sets but expose the
 * same signature; sharing via `webMain` would require an `applyDefaultHierarchyTemplate()`
 * restructure that's deferred to a follow-up plan.
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md` AC7
 * (js half).
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun launchWebWorkerApp(
    canvasElementId: String,
    koinModules: () -> List<Module>,
    onAfterKoinStart: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    startWorkerKoinIfAbsent(koinModules)
    // worker-kmp v4.0.0 — see wasmJs sibling for rationale.
    onAfterKoinStart()
    val container = document.getElementById(canvasElementId) as? HTMLElement
        ?: error("worker-kmp: viewport container '#$canvasElementId' not found in DOM")
    ComposeViewport(viewportContainer = container) { content() }
}

/** Idempotent Koin start helper (js). Public for test access. */
public fun startWorkerKoinIfAbsent(koinModules: () -> List<Module>) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(koinModules()) }
    }
}
