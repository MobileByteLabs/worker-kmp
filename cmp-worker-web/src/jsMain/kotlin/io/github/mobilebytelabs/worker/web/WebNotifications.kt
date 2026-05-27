package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.await
import kotlin.js.Promise

@Suppress("UnsafeCastFromDynamic")
private fun isAvailable(): Boolean = js(
    "(typeof window !== 'undefined' && 'Notification' in window)",
) as Boolean

@Suppress("UnsafeCastFromDynamic")
private fun currentPermission(): String = js(
    "(typeof Notification === 'undefined' ? 'default' : Notification.permission)",
) as String

@Suppress("UnsafeCastFromDynamic")
private fun requestPermissionPromise(): Promise<String> = js(
    "Notification.requestPermission()",
) as Promise<String>

public actual suspend fun requestNotificationPermission(): NotificationPermission {
    if (!isAvailable()) return NotificationPermission.DEFAULT
    val current = currentPermission()
    if (current == "granted") return NotificationPermission.GRANTED
    if (current == "denied") return NotificationPermission.DENIED
    return try {
        when (requestPermissionPromise().await()) {
            "granted" -> NotificationPermission.GRANTED
            "denied" -> NotificationPermission.DENIED
            else -> NotificationPermission.DEFAULT
        }
    } catch (_: Exception) {
        NotificationPermission.DEFAULT
    }
}

@Suppress("UnsafeCastFromDynamic", "UNUSED_PARAMETER")
private fun showOrCloseInJs(title: String, body: String, tag: String, progress: Int) {
    js(
        "(function(t,b,g,p){" +
            "try{" +
            "if(typeof Notification==='undefined')return;" +
            "if(Notification.permission!=='granted')return;" +
            "if(p>=100){" +
            "if(typeof navigator!=='undefined' && navigator.serviceWorker && navigator.serviceWorker.ready && navigator.serviceWorker.ready.then){" +
            "navigator.serviceWorker.ready.then(function(reg){reg.getNotifications({tag:g}).then(function(ns){ns.forEach(function(n){n.close();});});}).catch(function(){});" +
            "}" +
            "return;" +
            "}" +
            "var opts={body:b,tag:g,silent:true,renotify:true};" +
            "if(p>=0 && p<100){opts.body=b+' ('+p+'%)';}" +
            "if(typeof navigator!=='undefined' && navigator.serviceWorker && navigator.serviceWorker.ready && navigator.serviceWorker.ready.then){" +
            "navigator.serviceWorker.ready.then(function(reg){reg.showNotification(t,opts);}).catch(function(){new Notification(t,opts);});" +
            "}else{" +
            "new Notification(t,opts);" +
            "}" +
            "}catch(e){}" +
            "})",
    )(title, body, tag, progress)
}

public actual fun showWorkerNotification(id: String, title: String, body: String, progress: Int?) {
    if (!isAvailable()) return
    val pct = progress ?: -1
    showOrCloseInJs(title, body, "worker-kmp.$id", pct)
}
