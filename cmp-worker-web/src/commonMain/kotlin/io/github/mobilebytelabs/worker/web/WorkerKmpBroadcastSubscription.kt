package io.github.mobilebytelabs.worker.web

/**
 * Cross-tab broadcast bridge — surfaces messages emitted by the worker-kmp Service Worker
 * on the `'worker-kmp'` BroadcastChannel to the in-tab [WebWorkManager].
 *
 * Added in v3.0.0-alpha06.X (Phase 9 alpha06.X) — the SW broadcasts a
 * `{type: 'PENDING_PROCESSED', scope, count}` envelope after it marks ENQUEUED entries
 * as RUNNING; the in-tab manager listens + re-loads state from IndexedDB so the UI
 * re-renders without a manual refresh.
 *
 * Per-platform actuals:
 * - **JS** — real `BroadcastChannel('worker-kmp')` with `addEventListener('message', ...)`.
 * - **WasmJs** — no-op (will land in alpha06.X.Y when the BroadcastChannel @JsFun bindings
 *   surface in the kotlinx-browser shim — for now WasmJs tabs miss the SW notification but
 *   still poll IndexedDB on the existing constraint-check interval).
 * - **JVM** — no-op (no browser runtime).
 */
public expect fun openWorkerKmpBroadcastChannel(
    onMessage: (eventType: String, payload: String) -> Unit,
): WorkerKmpBroadcastSubscription

/**
 * Closeable handle returned by [openWorkerKmpBroadcastChannel]. Call [close] when the
 * in-tab manager is torn down (e.g., before page unload) — the JS actual closes the
 * underlying BroadcastChannel; other actuals are no-ops.
 */
public interface WorkerKmpBroadcastSubscription {
    public fun close()
}
