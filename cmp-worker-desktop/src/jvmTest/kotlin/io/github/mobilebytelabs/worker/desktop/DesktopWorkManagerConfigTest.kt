package io.github.mobilebytelabs.worker.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWorkManagerConfigTest {

    @Test
    fun defaultConfig_hasFourMaxWorkers() {
        assertEquals(4, DesktopWorkManagerConfig().maxConcurrentWorkers)
    }

    @Test
    fun defaultConfig_persistenceEnabled() {
        assertTrue(DesktopWorkManagerConfig().persistenceEnabled)
    }

    @Test
    fun inMemoryConstant_hasPersistenceDisabled() {
        assertFalse(DesktopWorkManagerConfig.IN_MEMORY.persistenceEnabled)
    }

    @Test
    fun defaultConstant_matchesDefaultConstructor() {
        assertEquals(DesktopWorkManagerConfig(), DesktopWorkManagerConfig.DEFAULT)
    }
}
