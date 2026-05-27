package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// WasmJs does not yet have direct SyncManager / ServiceWorker JS interop bindings.
// Falls back to polling + online watcher.
internal actual fun isBackgroundSyncSupported(): Boolean = false

internal actual fun backgroundSyncFlow(tag: String): Flow<Unit> = emptyFlow()

internal actual suspend fun registerBackgroundSyncTag(tag: String, swScript: String) = Unit
