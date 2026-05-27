package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.js.Promise

@Suppress("UnsafeCastFromDynamic")
internal actual fun isBackgroundSyncSupported(): Boolean = js(
    "(typeof navigator !== 'undefined' && 'serviceWorker' in navigator " +
        "&& typeof window !== 'undefined' && 'SyncManager' in window)",
) as Boolean

internal actual fun backgroundSyncFlow(tag: String): Flow<Unit> {
    if (js("typeof navigator === 'undefined' || !('serviceWorker' in navigator)") as Boolean) {
        return emptyFlow()
    }
    return callbackFlow {
        val messageHandler: (dynamic) -> Unit = { event: dynamic ->
            val data = event.data
            if (data != null &&
                (data["type"] as? String) == "WORKER_KMP_SYNC" &&
                (data["tag"] as? String) == tag
            ) {
                trySend(Unit)
            }
        }
        @Suppress("UnsafeCastFromDynamic")
        js("(function(h) { navigator.serviceWorker.addEventListener('message', h); })")(messageHandler)
        awaitClose {
            @Suppress("UnsafeCastFromDynamic")
            js("(function(h) { navigator.serviceWorker.removeEventListener('message', h); })")(messageHandler)
        }
    }
}

@Suppress("UnsafeCastFromDynamic")
private fun registerSwAndGetReady(script: String): Promise<dynamic> = js(
    "(function(s) { return navigator.serviceWorker.register(s)" +
        ".then(function() { return navigator.serviceWorker.ready; }); })",
)(script) as Promise<dynamic>

@Suppress("UnsafeCastFromDynamic")
private fun syncRegister(registration: dynamic, tag: String): Promise<dynamic> =
    js("(function(r, t) { return r.sync.register(t); })")(registration, tag) as Promise<dynamic>

internal actual suspend fun registerBackgroundSyncTag(tag: String, swScript: String) {
    if (!isBackgroundSyncSupported()) return
    try {
        val registration = registerSwAndGetReady(swScript).await()
        syncRegister(registration, tag).await<Unit>()
    } catch (_: Exception) {
        // SW registration or sync.register() failed — polling fallback takes over.
    }
}
