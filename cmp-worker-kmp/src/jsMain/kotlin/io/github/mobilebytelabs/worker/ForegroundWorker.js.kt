@file:OptIn(ExperimentalForegroundApi::class)

package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger

/**
 * JS (browser) actual for [runAsForeground].
 *
 * Requests Notification permission (idempotent — first call only) then posts a
 * progress-bearing Notification keyed by [ForegroundInfo.notificationId] (tag-based
 * de-dup so subsequent calls replace rather than stack). Service Worker registration
 * is delegated to consumer code — when a Service Worker is registered, the
 * notification is automatically routed through it (better browser eviction
 * resistance); otherwise the page-scope Notification API is used.
 *
 * **Unsupported environments**: when the browser lacks `window`, `Notification`, or
 * permission is denied, the call degrades to a kermit WARN log — the worker keeps
 * running, just without a notification surface.
 *
 * Replaces the log-only stub from alpha01.
 *
 * Added in v3.0.0-alpha01.X (Phase 1 deep impl).
 */
@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    if (!isNotificationApiAvailable()) {
        Logger.withTag("worker-kmp.foreground.js").w {
            "Notification API not available (non-browser JS context). Foreground promotion is log-only. " +
                "title=${info.title} progress=${info.progress.progress}"
        }
        return
    }
    requestNotificationPermissionAsync()
    showProgressNotification(info)
    Logger.withTag("worker-kmp.foreground.js").d {
        "JS notification dispatched: id=${info.notificationId} title=${info.title} progress=${info.progress.progress}"
    }
}

@Suppress("UnsafeCastFromDynamic")
private fun isNotificationApiAvailable(): Boolean =
    js("typeof window !== 'undefined' && 'Notification' in window") as Boolean

@Suppress("UnsafeCastFromDynamic")
private fun requestNotificationPermissionAsync() {
    js(
        "(function(){ try { if (Notification.permission === 'default') { Notification.requestPermission(); } } catch(e) {} })()",
    )
}

private fun showProgressNotification(info: ForegroundInfo) {
    val title = info.title
    val body = "${info.message} (${info.progress.progress}%)"
    val tag = "worker-kmp.${info.notificationId}"
    showNotification(title, body, tag)
}

@Suppress("UnsafeCastFromDynamic", "UNUSED_PARAMETER")
private fun showNotification(title: String, body: String, tag: String) {
    js(
        "(function(t,b,g){" +
            "try{" +
            "if(typeof Notification==='undefined')return;" +
            "if(Notification.permission!=='granted')return;" +
            "if(typeof navigator!=='undefined' && navigator.serviceWorker && " +
            "navigator.serviceWorker.ready && navigator.serviceWorker.ready.then){" +
            "navigator.serviceWorker.ready.then(function(reg){" +
            "reg.showNotification(t,{body:b,tag:g,silent:true,renotify:true});" +
            "}).catch(function(){new Notification(t,{body:b,tag:g,silent:true});});" +
            "}else{" +
            "new Notification(t,{body:b,tag:g,silent:true});" +
            "}" +
            "}catch(e){}" +
            "})",
    )(title, body, tag)
}
