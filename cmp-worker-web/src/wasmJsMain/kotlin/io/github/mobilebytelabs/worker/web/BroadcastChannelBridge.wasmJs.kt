@file:Suppress("NOTHING_TO_INLINE")

package io.github.mobilebytelabs.worker.web

import co.touchlab.kermit.Logger

/**
 * WasmJs BroadcastChannel actual — real impl using `@JsFun` interop to the browser's
 * `BroadcastChannel` API. Replaces the v3.0.0-alpha06 no-op stub.
 *
 * Closed by cross-platform-worker-parity-audit sub-plan 06 (G5).
 *
 * Observable behaviour matches the JS target:
 * - Opens a `BroadcastChannel('worker-kmp')` (same channel name across targets so
 *   JS and WasmJs tabs of the same origin can cross-talk if mixed).
 * - Wires `onmessage` to invoke [onMessage] with `(eventType, payloadJson)`.
 *   The browser-side wire format is `{ type: string, payload: string }` — matching
 *   the JS target's contract.
 * - Returns a [WorkerKmpBroadcastSubscription] whose `close()` calls `channel.close()`.
 *
 * Falls back to a no-op (with a one-time warning) when `BroadcastChannel` is undefined
 * in the host (e.g. an extension worker context). The constraint-check polling path
 * still surfaces work updates within `constraintCheckIntervalMs` — no functional break.
 */
public actual fun openWorkerKmpBroadcastChannel(
    onMessage: (eventType: String, payload: String) -> Unit,
): WorkerKmpBroadcastSubscription {
    if (!hasBroadcastChannel()) {
        Logger.withTag("worker-kmp.web.wasmjs").w {
            "BroadcastChannel is undefined in this WasmJs host — cross-tab dedup disabled. " +
                "Work updates still surface via constraint-check polling."
        }
        return NoOpSubscription
    }
    val handle = runCatching { openBroadcastChannel(CHANNEL_NAME, onMessage) }
        .getOrElse { t ->
            Logger.withTag("worker-kmp.web.wasmjs").w(t) { "BroadcastChannel open failed — falling back to no-op" }
            return NoOpSubscription
        }
    return RealSubscription(handle)
}

private const val CHANNEL_NAME = "worker-kmp"

private class RealSubscription(private val handle: JsAny) : WorkerKmpBroadcastSubscription {
    override fun close() {
        runCatching { closeBroadcastChannel(handle) }
            .onFailure { t -> Logger.withTag("worker-kmp.web.wasmjs").d(t) { "BroadcastChannel close threw" } }
    }
}

private object NoOpSubscription : WorkerKmpBroadcastSubscription {
    override fun close() = Unit
}

// ────────────────────────────────────────────────────────────────────
// @JsFun externals — minimum surface to talk to the browser's BroadcastChannel
// without depending on kotlinx-browser (its WasmJs bindings are still alpha).
// Replace with kotlinx-browser imports when those stabilise.
// ────────────────────────────────────────────────────────────────────

private fun hasBroadcastChannel(): Boolean = jsBroadcastChannelDefined()

@Suppress("UnusedReceiverParameter")
@JsFun("() => (typeof BroadcastChannel !== 'undefined')")
private external fun jsBroadcastChannelDefined(): Boolean

@JsFun(
    """
    (name, listener) => {
        const ch = new BroadcastChannel(name);
        ch.onmessage = (e) => {
            try {
                const d = e.data || {};
                listener(String(d.type || ''), String(d.payload || ''));
            } catch (_) { /* swallow */ }
        };
        return ch;
    }
    """,
)
private external fun openBroadcastChannel(name: String, listener: (String, String) -> Unit): JsAny

@JsFun("(channel) => { try { channel.close(); } catch (_) {} }")
private external fun closeBroadcastChannel(channel: JsAny)
