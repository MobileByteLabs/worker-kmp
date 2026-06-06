package io.github.mobilebytelabs.worker.webpush

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * wasmJsTest (runs as wasmJsBrowserTest via Karma + headless Chrome) — exercises
 * the WasmJs actual of WebPushSubscriber. In a headless test origin (no HTTPS,
 * no push permission), subscribe paths return null gracefully. Tests confirm the
 * API surface is callable without throwing.
 */
class WebPushSubscriberWasmJsTest {

    @Test
    fun createWebPushSubscriber_returnsNonNull() {
        val sub = createWebPushSubscriber()
        assertNotNull(sub)
    }

    @Test
    fun pushSupported_doesNotThrow() {
        val sub = createWebPushSubscriber()
        val supported = sub.pushSupported
        assertNotNull(supported)
    }

    @Test
    fun ensureSubscribed_whenDisabled_returnsNull() = runTest {
        val sub = createWebPushSubscriber()
        assertNull(sub.ensureSubscribed(WebPushConfig(enabled = false)))
    }

    @Test
    fun ensureSubscribed_withBlankVapidKey_returnsNull() = runTest {
        val sub = createWebPushSubscriber()
        // enabled=true but pushSupported will be false in headless test origin
        val result = sub.ensureSubscribed(WebPushConfig(enabled = true, vapidPublicKey = ""))
        assertNull(result)
    }
}
