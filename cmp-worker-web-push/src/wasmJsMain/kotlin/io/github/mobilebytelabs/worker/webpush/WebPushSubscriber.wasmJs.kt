@file:Suppress("NOTHING_TO_INLINE")

package io.github.mobilebytelabs.worker.webpush

import co.touchlab.kermit.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * WasmJs WebPushSubscriber actual — real impl using `@JsFun` interop to the browser's
 * Service Worker + Push Manager APIs. Replaces the v3.0.0-alpha06 stub.
 *
 * Closed by cross-platform-worker-parity-audit sub-plan 06 (G5).
 *
 * Observable behaviour matches the JS target ([JsWebPushSubscriber]):
 * - [ensureSubscribed]: registers `config.serviceWorkerScript`, calls
 *   `pushManager.subscribe({userVisibleOnly: true, applicationServerKey: vapidKey})`,
 *   POSTs `{endpoint, p256dh, auth}` to `config.serverEndpoint`. Returns the resulting
 *   [WebPushSubscription], or null on any failure (logged at WARN).
 * - [unsubscribe]: calls `subscription.unsubscribe()` if a subscription exists.
 * - [currentSubscription]: reads the existing subscription from `pushManager.getSubscription()`.
 * - [pushSupported]: probes `'serviceWorker' in navigator && 'PushManager' in window`.
 * - [requiresPwaInstall]: detects non-standalone Safari (PWA-install required).
 *
 * The async Service Worker / Push Manager calls are bridged through
 * [suspendCancellableCoroutine] + JS `then/catch` callbacks via @JsFun externs.
 */
public actual fun createWebPushSubscriber(): WebPushSubscriber = WasmJsWebPushSubscriber()

private class WasmJsWebPushSubscriber : WebPushSubscriber {
    private val log = Logger.withTag("worker-kmp-web-push.wasmjs")

    override val pushSupported: Boolean get() = jsPushSupported()
    override val requiresPwaInstall: Boolean get() = jsRequiresPwaInstall()

    override suspend fun ensureSubscribed(config: WebPushConfig): WebPushSubscription? {
        if (!config.enabled) {
            log.d { "WebPushConfig.enabled=false — skipping subscribe" }
            return null
        }
        if (!pushSupported) {
            log.w { "Browser lacks ServiceWorker or PushManager — Web Push unavailable on this host" }
            return null
        }
        return runCatching {
            val sub = subscribePushAsync(config.serviceWorkerScript, config.vapidPublicKey) ?: return null
            val authHeader = config.serverEndpointAuthHeader?.invoke().orEmpty()
            postSubscriptionToServerAsync(config.serverEndpoint, sub, authHeader)
            sub
        }.getOrElse { t ->
            log.w(t) { "ensureSubscribed failed on WasmJs" }
            null
        }
    }

    override suspend fun unsubscribe(): Boolean = runCatching { unsubscribeAsync() }
        .getOrElse { t ->
            log.w(t) { "unsubscribe failed on WasmJs" }
            false
        }

    override suspend fun currentSubscription(): WebPushSubscription? = runCatching { currentSubscriptionAsync() }
        .getOrElse { t ->
            log.w(t) { "currentSubscription failed on WasmJs" }
            null
        }

    private suspend fun subscribePushAsync(swScript: String, vapidKey: String): WebPushSubscription? =
        suspendCancellableCoroutine { cont ->
            jsSubscribePush(swScript, vapidKey) { endpoint, p256dh, auth ->
                if (endpoint == null) {
                    cont.resume(null)
                } else {
                    cont.resume(WebPushSubscription(endpoint = endpoint, p256dh = p256dh ?: "", auth = auth ?: ""))
                }
            }
        }

    private suspend fun postSubscriptionToServerAsync(
        url: String,
        sub: WebPushSubscription,
        authHeader: String,
    ): Boolean = suspendCancellableCoroutine { cont ->
        jsPostSubscription(url, sub.endpoint, sub.p256dh, sub.auth, authHeader) { ok ->
            cont.resume(ok)
        }
    }

    private suspend fun unsubscribeAsync(): Boolean = suspendCancellableCoroutine { cont ->
        jsUnsubscribe { ok -> cont.resume(ok) }
    }

    private suspend fun currentSubscriptionAsync(): WebPushSubscription? = suspendCancellableCoroutine { cont ->
        jsGetSubscription { endpoint, p256dh, auth ->
            if (endpoint == null) {
                cont.resume(null)
            } else {
                cont.resume(WebPushSubscription(endpoint, p256dh ?: "", auth ?: ""))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// @JsFun externs — Service Worker + Push Manager wire. Minimum surface needed.
// Replace with kotlinx-browser bindings when those ship for WasmJs.
// ────────────────────────────────────────────────────────────────────

@JsFun("() => (typeof navigator !== 'undefined' && 'serviceWorker' in navigator && typeof PushManager !== 'undefined')")
private external fun jsPushSupported(): Boolean

@JsFun(
    """
    () => {
        if (typeof window === 'undefined' || !window.navigator || !window.navigator.userAgent) return false;
        const ua = window.navigator.userAgent;
        const isSafari = /^((?!chrome|android).)*safari/i.test(ua);
        if (!isSafari) return false;
        const isStandalone = ('standalone' in window.navigator) ? window.navigator.standalone : false;
        return !isStandalone;
    }
    """,
)
private external fun jsRequiresPwaInstall(): Boolean

@JsFun(
    """
    (swScript, vapidKey, callback) => {
        navigator.serviceWorker.register(swScript)
            .then(() => navigator.serviceWorker.ready)
            .then(reg => reg.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: vapidKey,
            }))
            .then(sub => {
                const p256dh = btoa(String.fromCharCode.apply(null, new Uint8Array(sub.getKey('p256dh'))));
                const auth = btoa(String.fromCharCode.apply(null, new Uint8Array(sub.getKey('auth'))));
                callback(sub.endpoint, p256dh, auth);
            })
            .catch(_ => callback(null, null, null));
    }
    """,
)
private external fun jsSubscribePush(swScript: String, vapidKey: String, callback: (String?, String?, String?) -> Unit)

@JsFun(
    """
    (url, endpoint, p256dh, auth, authHeader, callback) => {
        const headers = { 'Content-Type': 'application/json' };
        if (authHeader) headers['Authorization'] = authHeader;
        fetch(url, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ endpoint: endpoint, p256dh: p256dh, auth: auth }),
        })
            .then(r => callback(r.ok))
            .catch(_ => callback(false));
    }
    """,
)
private external fun jsPostSubscription(
    url: String,
    endpoint: String,
    p256dh: String,
    auth: String,
    authHeader: String,
    callback: (Boolean) -> Unit,
)

@JsFun(
    """
    (callback) => {
        navigator.serviceWorker.ready
            .then(reg => reg.pushManager.getSubscription())
            .then(sub => sub ? sub.unsubscribe() : false)
            .then(ok => callback(!!ok))
            .catch(_ => callback(false));
    }
    """,
)
private external fun jsUnsubscribe(callback: (Boolean) -> Unit)

@JsFun(
    """
    (callback) => {
        navigator.serviceWorker.ready
            .then(reg => reg.pushManager.getSubscription())
            .then(sub => {
                if (!sub) { callback(null, null, null); return; }
                const p256dh = btoa(String.fromCharCode.apply(null, new Uint8Array(sub.getKey('p256dh'))));
                const auth = btoa(String.fromCharCode.apply(null, new Uint8Array(sub.getKey('auth'))));
                callback(sub.endpoint, p256dh, auth);
            })
            .catch(_ => callback(null, null, null));
    }
    """,
)
private external fun jsGetSubscription(callback: (String?, String?, String?) -> Unit)
