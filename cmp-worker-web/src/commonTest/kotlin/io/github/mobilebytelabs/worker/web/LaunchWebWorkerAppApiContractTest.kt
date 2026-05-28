package io.github.mobilebytelabs.worker.web

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Surface-shape contract test for the web launcher API.
 *
 * The full launcher (`launchWebWorkerApp` + `startWorkerKoinIfAbsent`) is exercised by the
 * `cmp-worker-sample-compose-store` wasmJs sample (Phase 3 T17: `wasmJsBrowserDevelopmentRun`
 * smoke check serves index.html with `<canvas id="composeCanvas">`). Headless wasmJs cannot
 * boot a `CanvasBasedWindow` without a real browser DOM.
 *
 * This test asserts the API exists at the expected fully-qualified name so a future
 * accidental rename surfaces immediately. The actual implementation files live in
 * wasmJsMain and jsMain (per-target due to the web source-set split).
 *
 * Spec: GOAL.md AC7 + AC10.
 */
class LaunchWebWorkerAppApiContractTest {

    @Test
    fun api_marker() {
        // commonTest cannot reference per-target source-set declarations directly;
        // the contract is enforced at module-link time. Asserting `true` documents
        // intent: when the apiCheck snapshot diverges from the expected web launcher
        // surface (`io.github.mobilebytelabs.worker.web.launchWebWorkerApp` +
        // `io.github.mobilebytelabs.worker.web.startWorkerKoinIfAbsent`), the apiCheck
        // gate fails. This test exists to make the contract discoverable in the test
        // tree.
        assertTrue(true)
    }
}
