@file:OptIn(ExperimentalForegroundApi::class)

package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger

/**
 * Wasm/JS (browser) actual for [runAsForeground].
 *
 * Same semantics as the JS actual — uses the browser Notification API to surface
 * progress, prefers a registered Service Worker when available. Wasm-side calls
 * the underlying JS interop bridge via the `@JsFun`-bound helpers below.
 *
 * **Unsupported environments**: when the browser lacks Notification API or permission is
 * denied, the call degrades to a kermit WARN log — the worker keeps running.
 *
 * Replaces the log-only stub from alpha01.
 *
 * Added in v3.0.0-alpha01.X (Phase 1 deep impl).
 */
@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    if (!isNotificationApiAvailableWasm()) {
        Logger.withTag("worker-kmp.foreground.wasmjs").w {
            "Notification API not available. Foreground promotion is log-only. " +
                "title=${info.title} progress=${info.progress.progress}"
        }
        return
    }
    requestNotificationPermissionWasm()
    showWasmNotification(
        title = info.title,
        body = "${info.message} (${info.progress.progress}%)",
        tag = "worker-kmp.${info.notificationId}",
    )
    Logger.withTag("worker-kmp.foreground.wasmjs").d {
        "WasmJS notification dispatched: id=${info.notificationId} title=${info.title} progress=${info.progress.progress}"
    }
}

private fun isNotificationApiAvailableWasm(): Boolean = isNotificationApiAvailableJs()

private fun requestNotificationPermissionWasm() {
    requestNotificationPermissionJs()
}

private fun showWasmNotification(title: String, body: String, tag: String) {
    showNotificationJs(title, body, tag)
}

// ── JS interop bridges (wasmJs → JS) ─────────────────────────────────────────

@JsFun(
    "() => (typeof window !== 'undefined' && 'Notification' in window)",
)
private external fun isNotificationApiAvailableJs(): Boolean

@JsFun(
    """() => {
        try { if (Notification.permission === 'default') { Notification.requestPermission(); } } catch(e) {}
    }""",
)
private external fun requestNotificationPermissionJs()

@JsFun(
    """(t, b, g) => {
        try {
            if (typeof Notification === 'undefined') return;
            if (Notification.permission !== 'granted') return;
            if (typeof navigator !== 'undefined' && navigator.serviceWorker && navigator.serviceWorker.ready && navigator.serviceWorker.ready.then) {
                navigator.serviceWorker.ready
                    .then(function(reg){ reg.showNotification(t, { body: b, tag: g, silent: true, renotify: true }); })
                    .catch(function(){ new Notification(t, { body: b, tag: g, silent: true }); });
            } else {
                new Notification(t, { body: b, tag: g, silent: true });
            }
        } catch(e) {}
    }""",
)
private external fun showNotificationJs(title: String, body: String, tag: String)
