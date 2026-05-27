package io.github.mobilebytelabs.worker.web

/**
 * WasmJs BroadcastChannel actual — no-op pending kotlinx-browser BroadcastChannel bindings.
 *
 * Added in v3.0.0-alpha06.X (Phase 9 alpha06.X). Will be replaced with a real @JsFun
 * binding in alpha06.X.Y when WasmJs interop matures. For now, WasmJs tabs miss the SW
 * cross-tab notification but still discover IndexedDB updates via the regular constraint-check
 * polling interval (no functional break — only a UX latency tradeoff).
 */
public actual fun openWorkerKmpBroadcastChannel(
    onMessage: (eventType: String, payload: String) -> Unit,
): WorkerKmpBroadcastSubscription = NoOpSubscription

private object NoOpSubscription : WorkerKmpBroadcastSubscription {
    override fun close() = Unit
}
