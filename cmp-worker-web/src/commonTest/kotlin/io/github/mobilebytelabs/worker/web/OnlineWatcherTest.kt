package io.github.mobilebytelabs.worker.web

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class OnlineWatcherTest {

    @Test
    fun onlineWatcher_returnsNonNullFlow() {
        val flow = onlineWatcher()
        assertNotNull(flow)
    }

    @Test
    fun onlineWatcher_calledTwice_doesNotThrow() {
        val f1 = onlineWatcher()
        val f2 = onlineWatcher()
        assertNotNull(f1)
        assertNotNull(f2)
    }

    @Test
    fun isBackgroundSyncSupported_doesNotThrow() {
        val supported = isBackgroundSyncSupported()
        assertNotNull(supported)
    }

    @Test
    fun registerBackgroundSyncTag_doesNotThrow() = runTest {
        registerBackgroundSyncTag("worker-kmp-test", "/worker-kmp-sw.js")
    }

    @Test
    fun registerPeriodicSyncTag_doesNotThrow() = runTest {
        registerPeriodicSyncTag("worker-kmp-periodic-test", 15L * 60 * 1000, "/worker-kmp-sw.js")
    }
}
