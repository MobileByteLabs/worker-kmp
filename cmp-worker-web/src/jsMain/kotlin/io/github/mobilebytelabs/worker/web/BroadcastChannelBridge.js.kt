package io.github.mobilebytelabs.worker.web

import co.touchlab.kermit.Logger

/**
 * JS BroadcastChannel actual — listens on `'worker-kmp'` channel for SW broadcasts.
 *
 * Added in v3.0.0-alpha06.X (Phase 9 alpha06.X).
 */
public actual fun openWorkerKmpBroadcastChannel(
    onMessage: (eventType: String, payload: String) -> Unit,
): WorkerKmpBroadcastSubscription {
    val log = Logger.withTag("worker-kmp-web")
    return try {
        val channel = js("new BroadcastChannel('worker-kmp')")
        val handler: (dynamic) -> Unit = handler@{ event ->
            try {
                val data = event.data ?: return@handler
                val type = data.type as? String ?: return@handler
                // Serialize the payload as JSON so the commonMain consumer gets a stable type.
                val payload = js("JSON.stringify(data)") as String
                onMessage(type, payload)
            } catch (e: Throwable) {
                log.w { "BroadcastChannel handler threw: ${e.message}" }
            }
        }
        channel.addEventListener("message", handler)
        JsBroadcastSubscription(channel)
    } catch (e: Throwable) {
        log.w { "openWorkerKmpBroadcastChannel failed: ${e.message}" }
        NoOpSubscription
    }
}

private class JsBroadcastSubscription(private val channel: dynamic) : WorkerKmpBroadcastSubscription {
    override fun close() {
        try {
            channel.close()
        } catch (_: Throwable) {
            // best-effort
        }
    }
}

private object NoOpSubscription : WorkerKmpBroadcastSubscription {
    override fun close() = Unit
}
