package io.github.mobilebytelabs.worker.web

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackgroundSyncTest {

    @Test
    fun backgroundSyncServiceWorkerScript_isNonEmpty() {
        val script = backgroundSyncServiceWorkerScript()
        assertTrue(script.isNotBlank(), "service-worker script must not be blank")
    }

    @Test
    fun backgroundSyncServiceWorkerScript_containsSyncHandler() {
        val script = backgroundSyncServiceWorkerScript()
        assertTrue(script.contains("sync"), "script must handle sync event")
    }

    @Test
    fun backgroundSyncServiceWorkerScript_containsWorkerKmpPrefix() {
        val script = backgroundSyncServiceWorkerScript()
        assertTrue(script.contains("worker-kmp-"), "sync tag prefix must be worker-kmp-")
    }

    @Test
    fun backgroundSyncServiceWorkerScript_containsMessageHandler() {
        val script = backgroundSyncServiceWorkerScript()
        assertTrue(script.contains("message"), "script must handle message event")
    }

    @Test
    fun openWorkerKmpBroadcastChannel_returnsCloseable() {
        val subscription = openWorkerKmpBroadcastChannel { _, _ -> }
        assertNotNull(subscription)
        subscription.close()
    }

    @Test
    fun workerKmpBroadcastSubscription_close_doesNotThrow() {
        val subscription = openWorkerKmpBroadcastChannel { _, _ -> }
        subscription.close()
        subscription.close()
    }
}
