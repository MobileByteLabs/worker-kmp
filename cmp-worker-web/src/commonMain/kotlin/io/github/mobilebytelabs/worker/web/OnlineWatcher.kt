package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.flow.Flow

/**
 * Returns a [Flow] that emits [Unit] whenever the network online/offline state changes.
 * JS target wires to `window.addEventListener("online")` + `window.addEventListener("offline")`.
 * JVM and WasmJs targets return [emptyFlow] — the polling fallback in [WebWorkManager] is used.
 */
internal expect fun onlineWatcher(): Flow<Unit>
