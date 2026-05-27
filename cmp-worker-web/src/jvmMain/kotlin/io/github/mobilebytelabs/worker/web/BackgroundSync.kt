package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal actual fun isBackgroundSyncSupported(): Boolean = false

internal actual fun backgroundSyncFlow(tag: String): Flow<Unit> = emptyFlow()

internal actual suspend fun registerBackgroundSyncTag(tag: String, swScript: String) = Unit
