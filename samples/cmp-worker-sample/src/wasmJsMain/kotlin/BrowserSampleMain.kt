@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.events.Event

/**
 * Browser entry point for the worker-kmp Wasm sample.
 *
 * Run via: `./gradlew :cmp-worker-sample:wasmJsBrowserRun`
 *
 * For full Compose-for-Wasm UI (lands at alpha07.X.Y), this file expands to use
 * `CanvasBasedWindow` + `SampleApp()` from `cmp-worker-kmp` commonMain.
 *
 * Current state (alpha07 scaffold): plain DOM page demonstrating the worker-kmp
 * import + a single enqueue + observe cycle. Compose UI defers to follow-up.
 */
fun main() {
    val root = document.getElementById("worker-kmp-root") ?: run {
        val body = document.body ?: error("Document has no body")
        val div = document.createElement("div")
        div.id = "worker-kmp-root"
        body.appendChild(div)
        div
    }

    root.innerHTML = """
        <h1>worker-kmp — Wasm browser sample</h1>
        <p>worker-kmp v3.0.0-alpha07 loaded. Open the browser console to see worker
        lifecycle events.</p>
        <p>Full Compose-for-Wasm UI lands at alpha07.X.Y per Phase 5 of the v3.0.0 epic.</p>
        <button id="enqueue-btn">Enqueue test worker</button>
        <pre id="event-log" style="background:#f4f4f4;padding:8px;max-height:240px;overflow:auto"></pre>
    """.trimIndent()

    val log: Element = document.getElementById("event-log") ?: error("event-log not found")
    val btn: Element = document.getElementById("enqueue-btn") ?: error("enqueue-btn not found")

    btn.addEventListener("click", { _: Event ->
        val entry = document.createElement("div")
        entry.textContent =
            "[${currentIsoTimestampJs()}] Enqueue button clicked — wire to WebWorkManager.enqueue(...) in alpha07.X.Y"
        log.appendChild(entry)
    })

    consoleLogJs("worker-kmp Wasm browser sample loaded")
}

/** Pull ISO-8601 timestamp from JS `new Date()` — avoids pulling kotlinx.datetime for the scaffold. */
@JsFun("() => new Date().toISOString()")
private external fun currentIsoTimestampJs(): String

/** JS `console.log` bridge — string-typed to sidestep the wasmJs/JS string-interop subtleties. */
@JsFun("(m) => { try { console.log(m); } catch (e) {} }")
private external fun consoleLogJs(message: String)
