package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkManagerFactory
import kotlin.test.Test
import kotlin.test.assertNotNull

class DesktopWorkManagerInitTest {

    @Test
    fun desktopWorkManagerFactory_returnsNonNullFactory() {
        val factory: WorkManagerFactory = desktopWorkManagerFactory()
        assertNotNull(factory)
    }
}
