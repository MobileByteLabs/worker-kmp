@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.WorkManagerFactory
import kotlin.test.Test
import kotlin.test.assertNotNull

class IosWorkManagerInitTest {

    @Test
    fun iosWorkManagerFactory_returnsNonNullFactory() {
        val factory: WorkManagerFactory = iosWorkManagerFactory()
        assertNotNull(factory)
    }
}
