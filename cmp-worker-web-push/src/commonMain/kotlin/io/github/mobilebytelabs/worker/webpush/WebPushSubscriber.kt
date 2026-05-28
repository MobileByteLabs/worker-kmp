package io.github.mobilebytelabs.worker.webpush

/**
 * Web Push subscription manager. Consumer-side interface to register / unregister
 * / query the SW's push subscription.
 *
 * Added in v3.0.0-alpha06.
 *
 * Per-platform actual implementations:
 * - **JS**: wraps `navigator.serviceWorker.register(...)` + `pushManager.subscribe(...)`;
 *   posts `WebPushSubscription` to [WebPushConfig.serverEndpoint].
 * - **WasmJs**: same via @JsFun bindings.
 * - **JVM/iOS**: no-op stubs (Web Push is browser-only).
 *
 * Real per-platform impls land in alpha06.X follow-ups. alpha06 ships the interface
 * + factory + stub actuals.
 */
public interface WebPushSubscriber {

    /**
     * Ensure the SW is registered + push subscribed. Returns the active subscription
     * if successful; null if push is unsupported (consumer should fall back per
     * [WebPushConfig.foregroundFallback]).
     */
    public suspend fun ensureSubscribed(config: WebPushConfig): WebPushSubscription?

    public suspend fun unsubscribe(): Boolean
    public suspend fun currentSubscription(): WebPushSubscription?

    public val pushSupported: Boolean
    public val requiresPwaInstall: Boolean // iOS Safari quirk
}

/**
 * Factory — returns the platform actual. alpha06 returns log-only stubs on all
 * targets; real JS/WasmJs impls land in alpha06.X.
 */
public expect fun createWebPushSubscriber(): WebPushSubscriber
