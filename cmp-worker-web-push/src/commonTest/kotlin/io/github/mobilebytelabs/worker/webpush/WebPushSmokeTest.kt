package io.github.mobilebytelabs.worker.webpush

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebPushSmokeTest {

    @Test fun config_defaults_areSafe() {
        val c = WebPushConfig()
        assertFalse(c.enabled)
        assertFalse(c.notificationPermissionAutoRequest)
        assertTrue(c.foregroundFallback)
        assertEquals(90, c.subscriptionExpiryDays)
    }

    @Test fun subscriber_stub_returnsNull() = runTest {
        val s = createWebPushSubscriber()
        assertNull(s.ensureSubscribed(WebPushConfig(enabled = true)))
        assertFalse(s.pushSupported)
    }
}
