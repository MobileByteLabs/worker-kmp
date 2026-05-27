package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.flow.Flow

/** Returns `true` when the current platform supports the Browser Background Sync API. */
internal expect fun isBackgroundSyncSupported(): Boolean

/**
 * A [Flow] that emits [Unit] each time the Service Worker signals a sync event for [tag].
 * Returns an empty flow on platforms that do not support Background Sync.
 */
internal expect fun backgroundSyncFlow(tag: String): Flow<Unit>

/**
 * Registers a Background Sync tag with the platform Service Worker.
 * No-op on platforms that do not support Background Sync or when [isBackgroundSyncSupported] is false.
 */
internal expect suspend fun registerBackgroundSyncTag(tag: String, swScript: String)

/**
 * Registers a Periodic Background Sync tag with the platform Service Worker.
 * Periodic Sync runs at minimum [minIntervalMs] intervals while the browser deems
 * the origin "active" (PWA installed, user-engaged). No-op on platforms that do
 * not support the Periodic Background Sync API.
 *
 * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
 */
internal expect suspend fun registerPeriodicSyncTag(tag: String, minIntervalMs: Long, swScript: String)
