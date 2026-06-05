package io.github.mobilebytelabs.worker.webpush

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WebPushSubscriptionTest {

    @Test
    fun construct_exposesEndpointAndKeys() {
        val sub = WebPushSubscription(
            endpoint = "https://example.push/abc",
            p256dh = "BASE64_P256DH",
            auth = "BASE64_AUTH",
        )
        assertEquals("https://example.push/abc", sub.endpoint)
        assertNotNull(sub.p256dh)
        assertNotNull(sub.auth)
    }

    @Test
    fun dataClass_equalOnIdenticalValues() {
        val a = WebPushSubscription("https://push/x", "key", "auth")
        val b = WebPushSubscription("https://push/x", "key", "auth")
        assertEquals(a, b)
    }

    @Test
    fun dataClass_copyPreservesOtherFields() {
        val original = WebPushSubscription("https://push/x", "key", "auth")
        val copy = original.copy(auth = "new-auth")
        assertEquals("https://push/x", copy.endpoint)
        assertEquals("key", copy.p256dh)
        assertEquals("new-auth", copy.auth)
    }

    @Test
    fun toString_containsEndpoint() {
        val sub = WebPushSubscription("https://push/abc", "key", "auth")
        assertNotNull(sub.toString())
    }
}
