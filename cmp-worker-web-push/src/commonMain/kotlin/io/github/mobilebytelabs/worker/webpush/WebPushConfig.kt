package io.github.mobilebytelabs.worker.webpush

/**
 * Configuration for Web Push subscription. Consumer's server is the cron — sends
 * a push message that wakes the Service Worker.
 *
 * Added in v3.0.0-alpha06 (Phase 9 of the v3.0.0 epic).
 *
 * @property enabled If false, library does nothing (no subscription, no SW registration).
 * @property vapidPublicKey BASE64URL-encoded P-256 public key. Consumer generates via
 *   the `generateVapidKeys` Gradle task (alpha06.X) + keeps the private key in the
 *   framework vault per RULE-SECRETS-VAULT-001.
 * @property serverEndpoint Consumer's server endpoint for POSTing subscription metadata
 *   (`{endpoint, p256dh, auth}` JSON). Consumer uses these to construct subsequent pushes
 *   per RFC 8030.
 * @property serverEndpointAuthHeader Optional async provider for the `Authorization` header
 *   value used when POSTing subscriptions to [serverEndpoint]. The library invokes the
 *   suspend lambda each time it submits a subscription — letting consumers fetch a fresh
 *   bearer token / OIDC JWT / signed-request header out-of-band. Returning a blank string
 *   omits the header. Added in v3.0.0-alpha06.X (Phase 9 alpha06.X).
 * @property foregroundFallback If push subscription fails or browser doesn't support it,
 *   fall back to existing 2.1.0 polling-while-tab-open behavior.
 * @property notificationPermissionAutoRequest If false, library NEVER auto-prompts for
 *   notification permission. Default false — consumer-side UX is responsible.
 * @property serviceWorkerScript Path to the Service Worker file on the consumer's
 *   HTTP origin. Defaults to `/worker-kmp-sw.js`.
 * @property subscriptionExpiryDays Auto-refresh subscriptions every N days to defend
 *   against T8 (replay over long horizons).
 */
public data class WebPushConfig(
    public val enabled: Boolean = false,
    public val vapidPublicKey: String = "",
    public val serverEndpoint: String = "",
    public val serverEndpointAuthHeader: (suspend () -> String)? = null,
    public val foregroundFallback: Boolean = true,
    public val notificationPermissionAutoRequest: Boolean = false,
    public val serviceWorkerScript: String = "/worker-kmp-sw.js",
    public val subscriptionExpiryDays: Int = 90,
)
