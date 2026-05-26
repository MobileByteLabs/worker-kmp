package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Suppress("UnsafeCastFromDynamic")
private fun addWindowListener(type: String, handler: () -> Unit): dynamic =
    js("(function(t, h) { if (typeof window !== 'undefined') { window.addEventListener(t, h); } })")(type, handler)

@Suppress("UnsafeCastFromDynamic")
private fun removeWindowListener(type: String, handler: dynamic): Unit =
    js("(function(t, h) { if (typeof window !== 'undefined') { window.removeEventListener(t, h); } })")(type, handler)

internal actual fun onlineWatcher(): Flow<Unit> = callbackFlow {
    if (js("typeof window === 'undefined'") as Boolean) {
        close()
        return@callbackFlow
    }
    val handler: () -> Unit = { trySend(Unit) }
    addWindowListener("online", handler)
    addWindowListener("offline", handler)
    awaitClose {
        removeWindowListener("online", handler)
        removeWindowListener("offline", handler)
    }
}
