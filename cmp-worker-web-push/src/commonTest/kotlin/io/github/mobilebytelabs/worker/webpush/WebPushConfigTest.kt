package io.github.mobilebytelabs.worker.webpush

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebPushConfigTest {

    @Test
    fun defaults_areSafeForConsumer() {
        val cfg = WebPushConfig()
        assertFalse(cfg.enabled)
        assertFalse(cfg.notificationPermissionAutoRequest)
        assertTrue(cfg.foregroundFallback)
        assertEquals(90, cfg.subscriptionExpiryDays)
        assertEquals("/worker-kmp-sw.js", cfg.serviceWorkerScript)
    }

    @Test
    fun vapidPublicKey_defaultIsBlank() {
        assertEquals("", WebPushConfig().vapidPublicKey)
    }

    @Test
    fun vapidPublicKey_canBeSet() {
        val cfg = WebPushConfig(vapidPublicKey = "BASE64_VAPID_KEY")
        assertEquals("BASE64_VAPID_KEY", cfg.vapidPublicKey)
    }

    @Test
    fun serverEndpoint_canBeSet() {
        val cfg = WebPushConfig(serverEndpoint = "https://example.com/push")
        assertEquals("https://example.com/push", cfg.serverEndpoint)
    }

    @Test
    fun enabled_canBeSetToTrue() {
        val cfg = WebPushConfig(enabled = true)
        assertTrue(cfg.enabled)
    }

    @Test
    fun dataClass_copyPreservesOtherFields() {
        val original = WebPushConfig(vapidPublicKey = "KEY", subscriptionExpiryDays = 30)
        val copy = original.copy(enabled = true)
        assertTrue(copy.enabled)
        assertEquals("KEY", copy.vapidPublicKey)
        assertEquals(30, copy.subscriptionExpiryDays)
    }
}
