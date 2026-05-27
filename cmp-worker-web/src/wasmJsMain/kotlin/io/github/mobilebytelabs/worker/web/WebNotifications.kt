package io.github.mobilebytelabs.worker.web

@JsFun(
    "() => (typeof window !== 'undefined' && 'Notification' in window)",
)
private external fun isAvailableJs(): Boolean

@JsFun(
    "() => (typeof Notification === 'undefined' ? 'default' : Notification.permission)",
)
private external fun currentPermissionJs(): String

// Wasm/JS cannot await a Promise<String> directly without kotlinx-coroutines wasm
// helpers we don't currently depend on; we fire-and-forget the request and read the
// updated permission a moment later. The result is best-effort but adequate for
// the foreground-task flow (the JS impl waits for the user; Wasm callers may need
// to re-poll after first interaction).
@JsFun(
    """() => {
        try { Notification.requestPermission(); } catch(e) {}
    }""",
)
private external fun fireAndForgetRequestJs()

public actual suspend fun requestNotificationPermission(): NotificationPermission {
    if (!isAvailableJs()) return NotificationPermission.DEFAULT
    val current = currentPermissionJs()
    if (current == "granted") return NotificationPermission.GRANTED
    if (current == "denied") return NotificationPermission.DENIED
    fireAndForgetRequestJs()
    return when (currentPermissionJs()) {
        "granted" -> NotificationPermission.GRANTED
        "denied" -> NotificationPermission.DENIED
        else -> NotificationPermission.DEFAULT
    }
}

@JsFun(
    """(t, b, g, p) => {
        try {
            if (typeof Notification === 'undefined') return;
            if (Notification.permission !== 'granted') return;
            if (p >= 100) {
                if (typeof navigator !== 'undefined' && navigator.serviceWorker && navigator.serviceWorker.ready && navigator.serviceWorker.ready.then) {
                    navigator.serviceWorker.ready
                        .then(function(reg){ reg.getNotifications({ tag: g }).then(function(ns){ ns.forEach(function(n){ n.close(); }); }); })
                        .catch(function(){});
                }
                return;
            }
            var opts = { body: b, tag: g, silent: true, renotify: true };
            if (p >= 0 && p < 100) { opts.body = b + ' (' + p + '%)'; }
            if (typeof navigator !== 'undefined' && navigator.serviceWorker && navigator.serviceWorker.ready && navigator.serviceWorker.ready.then) {
                navigator.serviceWorker.ready
                    .then(function(reg){ reg.showNotification(t, opts); })
                    .catch(function(){ new Notification(t, opts); });
            } else {
                new Notification(t, opts);
            }
        } catch(e) {}
    }""",
)
private external fun showOrCloseJs(title: String, body: String, tag: String, progress: Int)

public actual fun showWorkerNotification(id: String, title: String, body: String, progress: Int?) {
    if (!isAvailableJs()) return
    val pct = progress ?: -1
    showOrCloseJs(title, body, "worker-kmp.$id", pct)
}
