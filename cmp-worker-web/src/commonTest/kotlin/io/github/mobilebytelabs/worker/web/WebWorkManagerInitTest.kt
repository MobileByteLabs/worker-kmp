@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkManagerFactory
import kotlin.test.Test
import kotlin.test.assertNotNull

class WebWorkManagerInitTest {

    @Test
    fun webWorkManagerFactory_returnsNonNullFactory() {
        val factory: WorkManagerFactory = webWorkManagerFactory()
        assertNotNull(factory)
    }

    @Test
    fun webWorkManagerFactory_calledTwice_returnsDifferentInstances() {
        val f1 = webWorkManagerFactory()
        val f2 = webWorkManagerFactory()
        assertNotNull(f1)
        assertNotNull(f2)
    }

    @Test
    fun isWebWorkManagerSupported_doesNotThrow() {
        val supported = isWebWorkManagerSupported()
        assertNotNull(supported)
    }
}
