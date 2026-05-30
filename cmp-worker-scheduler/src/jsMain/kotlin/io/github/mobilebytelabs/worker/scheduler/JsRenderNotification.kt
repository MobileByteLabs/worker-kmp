package io.github.mobilebytelabs.worker.scheduler

/**
 * JS (legacy) actual for [renderNotification].
 *
 * Minimal log-only stub — JS-target browser Notification API not wired in v1.
 * wasmJs uses `cmp-worker-web.showWorkerNotification`; the jsMain equivalent
 * would be added when a consumer pulls the `js` target. Until then, this prints
 * to the browser console so calls remain non-throwing.
 */
actual fun renderNotification(content: NotificationContent) {
    println("[JsRenderNotification] ${content.title}: ${content.body}")
}
