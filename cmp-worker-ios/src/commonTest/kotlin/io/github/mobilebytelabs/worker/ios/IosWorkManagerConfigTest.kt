package io.github.mobilebytelabs.worker.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosWorkManagerConfigTest {

    @Test
    fun defaultConfig_hasBackgroundTasksDisabled() {
        val config = IosWorkManagerConfig()
        assertFalse(config.enableBackgroundTasks)
    }

    @Test
    fun defaultConfig_persistenceEnabled() {
        val config = IosWorkManagerConfig()
        assertTrue(config.enablePersistence)
    }

    @Test
    fun customConfig_overridesDefaults() {
        val config = IosWorkManagerConfig(
            enableBackgroundTasks = true,
            bgProcessingTaskIdentifier = "com.example.sync",
            enablePersistence = false,
        )
        assertTrue(config.enableBackgroundTasks)
        assertEquals("com.example.sync", config.bgProcessingTaskIdentifier)
        assertFalse(config.enablePersistence)
    }

    @Test
    fun defaultConstant_matchesDefaultConstructor() {
        val config = IosWorkManagerConfig.DEFAULT
        assertEquals(IosWorkManagerConfig(), config)
    }
}
