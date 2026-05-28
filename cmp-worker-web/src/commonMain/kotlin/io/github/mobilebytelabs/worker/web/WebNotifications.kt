package io.github.mobilebytelabs.worker.web

/**
 * Public Web Notifications API surface for `cmp-worker-web` consumers.
 *
 * Used internally by the JS / WasmJs `runAsForeground` actuals to surface progress
 * for foreground tasks. Exposed publicly so app code can re-use the same primitives
 * (e.g. show notifications outside the worker context).
 *
 * Per-platform actual implementations live under `jsMain` / `wasmJsMain` / `jvmMain`.
 * Non-browser environments degrade to no-ops (the JVM tests-side actual returns
 * [NotificationPermission.DEFAULT] and silently drops [showWorkerNotification]).
 *
 * Added in v3.0.0-alpha04.X (Phase 7 alpha04.X).
 */
public enum class NotificationPermission { GRANTED, DENIED, DEFAULT }

/**
 * Requests Notification permission from the user. Idempotent — on browsers
 * already in `GRANTED` or `DENIED` state the call returns immediately without
 * prompting again.
 *
 * Returns the current permission AFTER the request resolves.
 *
 * Added in v3.0.0-alpha04.X.
 */
public expect suspend fun requestNotificationPermission(): NotificationPermission

/**
 * Shows a worker-kmp notification with optional progress.
 *
 * - When [progress] is `null` → no progress UI (one-shot status notification).
 * - When [progress] is in `0..100` → progress bar rendered (where supported).
 * - When [progress] >= 100 → notification is closed if already visible.
 *
 * Uses a `tag` derived from a stable id so subsequent calls REPLACE the prior
 * notification rather than stacking. Routes through `ServiceWorker.showNotification`
 * when an SW is registered (better eviction resistance), else page-scope `Notification`.
 *
 * No-op when [requestNotificationPermission] has not returned [NotificationPermission.GRANTED]
 * — call [requestNotificationPermission] first.
 *
 * Added in v3.0.0-alpha04.X.
 */
public expect fun showWorkerNotification(id: String, title: String, body: String, progress: Int? = null)
