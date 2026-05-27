package io.github.mobilebytelabs.worker.webpush

import co.touchlab.kermit.Logger
import kotlinx.coroutines.await
import kotlin.js.Promise

public actual fun createWebPushSubscriber(): WebPushSubscriber = JsWebPushSubscriber()

/**
 * Real JS Web Push subscriber. Wraps `navigator.serviceWorker.register(...)` +
 * `pushManager.subscribe({userVisibleOnly, applicationServerKey})` per the W3C Push API spec.
 *
 * Added in v3.0.0-alpha06.X (Phase 9 alpha06.X). Replaces the alpha06 log-only stub.
 *
 * Lifecycle:
 * 1. [ensureSubscribed] registers the Service Worker at [WebPushConfig.serviceWorkerScript],
 *    queries an existing subscription, or creates a fresh one with the consumer's VAPID key.
 *    The resulting subscription metadata (endpoint + p256dh + auth, BASE64URL-encoded) is
 *    POSTed to [WebPushConfig.serverEndpoint] with an optional Authorization header from
 *    [WebPushConfig.serverEndpointAuthHeader].
 * 2. [currentSubscription] returns the existing subscription via the `ready` registration.
 * 3. [unsubscribe] calls `subscription.unsubscribe()` and returns the result.
 *
 * All browser/network errors are caught + logged at WARN; consumers fall back to the
 * polling-while-tab-open path when [WebPushConfig.foregroundFallback] is true.
 */
internal class JsWebPushSubscriber : WebPushSubscriber {

    private val log = Logger.withTag("worker-kmp-web-push")

    override val pushSupported: Boolean
        get() = try {
            js("(typeof navigator !== 'undefined') && ('serviceWorker' in navigator) && (typeof window !== 'undefined') && ('PushManager' in window)")
                .unsafeCast<Boolean>()
        } catch (_: Throwable) {
            false
        }

    override val requiresPwaInstall: Boolean
        get() = try {
            js("(typeof navigator !== 'undefined') && (/iPad|iPhone|iPod/.test(navigator.userAgent)) && !navigator.standalone")
                .unsafeCast<Boolean>()
        } catch (_: Throwable) {
            false
        }

    override suspend fun ensureSubscribed(config: WebPushConfig): WebPushSubscription? {
        if (!pushSupported || !config.enabled) {
            log.i { "ensureSubscribed: pushSupported=$pushSupported enabled=${config.enabled} — skipping" }
            return null
        }
        if (config.vapidPublicKey.isBlank()) {
            log.w { "ensureSubscribed: vapidPublicKey is empty; consumer must set WebPushConfig.vapidPublicKey" }
            return null
        }
        return try {
            val registration = registerServiceWorker(config.serviceWorkerScript).await()
            val existing = getSubscription(registration).await()
            val rawSubscription = existing ?: subscribePush(registration, config.vapidPublicKey).await()
            val sub = parseSubscription(rawSubscription)
            postSubscriptionToServer(config, sub)
            sub
        } catch (e: Throwable) {
            log.w { "ensureSubscribed failed: ${e.message}" }
            null
        }
    }

    override suspend fun unsubscribe(): Boolean = try {
        val registration = ready().await()
        val sub = getSubscription(registration).await()
        if (sub == null) {
            true
        } else {
            (sub.asDynamic().unsubscribe() as Promise<Boolean>).await()
        }
    } catch (e: Throwable) {
        log.w { "unsubscribe failed: ${e.message}" }
        false
    }

    override suspend fun currentSubscription(): WebPushSubscription? = try {
        val registration = ready().await()
        val sub = getSubscription(registration).await()
        if (sub == null) null else parseSubscription(sub)
    } catch (e: Throwable) {
        log.w { "currentSubscription failed: ${e.message}" }
        null
    }

    private fun registerServiceWorker(scriptUrl: String): Promise<dynamic> =
        js("navigator.serviceWorker.register(scriptUrl)").unsafeCast<Promise<dynamic>>()

    private fun ready(): Promise<dynamic> =
        js("navigator.serviceWorker.ready").unsafeCast<Promise<dynamic>>()

    private fun getSubscription(reg: dynamic): Promise<dynamic> =
        js("reg.pushManager.getSubscription()").unsafeCast<Promise<dynamic>>()

    private fun subscribePush(reg: dynamic, vapidKey: String): Promise<dynamic> {
        val key = vapidKey
        return js("reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: key })")
            .unsafeCast<Promise<dynamic>>()
    }

    private fun parseSubscription(sub: dynamic): WebPushSubscription {
        val endpoint = sub.endpoint as String
        val p256dhKey = sub.getKey("p256dh")
        val authKey = sub.getKey("auth")
        return WebPushSubscription(
            endpoint = endpoint,
            p256dh = encodeArrayBufferAsBase64Url(p256dhKey),
            auth = encodeArrayBufferAsBase64Url(authKey),
        )
    }

    private fun encodeArrayBufferAsBase64Url(buffer: dynamic): String =
        js("btoa(String.fromCharCode.apply(null, new Uint8Array(buffer))).replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+${'$'}/, '')")
            .unsafeCast<String>()

    private suspend fun postSubscriptionToServer(config: WebPushConfig, sub: WebPushSubscription) {
        if (config.serverEndpoint.isBlank()) {
            log.i { "postSubscriptionToServer: serverEndpoint is blank — skipping POST" }
            return
        }
        try {
            val authHeader = config.serverEndpointAuthHeader?.invoke().orEmpty()
            val endpointUrl = config.serverEndpoint
            val body = """{"endpoint":"${sub.endpoint}","p256dh":"${sub.p256dh}","auth":"${sub.auth}"}"""
            val headerToken = authHeader
            // fetch() returns a Promise<Response>; we fire-and-forget (consumer may retry on its own).
            js("fetch(endpointUrl, { method: 'POST', headers: headerToken ? { 'Content-Type': 'application/json', 'Authorization': headerToken } : { 'Content-Type': 'application/json' }, body: body })")
        } catch (e: Throwable) {
            log.w { "POST subscription to ${config.serverEndpoint} failed: ${e.message}" }
        }
    }
}
