package io.github.mobilebytelabs.worker.web

/**
 * JVM BroadcastChannel actual — no-op (no browser runtime).
 *
 * Added in v3.0.0-alpha06.X (Phase 9 alpha06.X). cmp-worker-web's JVM target exists only
 * for test infrastructure; there is no browser/SW context for cross-tab broadcasts.
 */
public actual fun openWorkerKmpBroadcastChannel(
    onMessage: (eventType: String, payload: String) -> Unit,
): WorkerKmpBroadcastSubscription = NoOpSubscription

private object NoOpSubscription : WorkerKmpBroadcastSubscription {
    override fun close() = Unit
}
