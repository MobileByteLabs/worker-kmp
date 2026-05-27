package io.github.mobilebytelabs.worker.webpush

/**
 * Web Push subscription metadata. Per RFC 8030 — consumer's server uses these to
 * construct subsequent pushes via VAPID-signed JWT.
 *
 * Added in v3.0.0-alpha06.
 *
 * @property endpoint The push service URL (Chrome → FCM, Firefox → Mozilla Push,
 *   Safari → APNs). Treat as a bearer secret.
 * @property p256dh BASE64URL P-256 public key for end-to-end push payload encryption.
 * @property auth BASE64URL random 16-byte auth secret for payload authentication.
 */
public data class WebPushSubscription(
    public val endpoint: String,
    public val p256dh: String,
    public val auth: String,
)
