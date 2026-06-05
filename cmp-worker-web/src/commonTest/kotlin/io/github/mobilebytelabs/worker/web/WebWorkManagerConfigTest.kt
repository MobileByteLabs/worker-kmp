package io.github.mobilebytelabs.worker.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebWorkManagerConfigTest {

    @Test
    fun defaultConfig_constraintCheckIntervalMs_is5000() {
        assertEquals(5_000L, WebWorkManagerConfig().constraintCheckIntervalMs)
    }

    @Test
    fun defaultConfig_persistenceEnabled_isTrue() {
        assertTrue(WebWorkManagerConfig().enablePersistence)
    }

    @Test
    fun defaultConfig_persistenceDbName_isWorkerKmp() {
        assertEquals("worker-kmp", WebWorkManagerConfig().persistenceDbName)
    }

    @Test
    fun defaultConfig_backgroundSync_isDisabled() {
        assertFalse(WebWorkManagerConfig().enableBackgroundSync)
    }

    @Test
    fun defaultConfig_periodicBackgroundSync_isDisabled() {
        assertFalse(WebWorkManagerConfig().enablePeriodicBackgroundSync)
    }

    @Test
    fun defaultConfig_serviceWorkerScript_isWorkerKmpSwJs() {
        assertEquals("/worker-kmp-sw.js", WebWorkManagerConfig().serviceWorkerScript)
    }

    @Test
    fun defaultConstant_matchesDefaultConstructor() {
        assertEquals(WebWorkManagerConfig(), WebWorkManagerConfig.DEFAULT)
    }

    @Test
    fun customConfig_overridesDefaults() {
        val config = WebWorkManagerConfig(
            enablePersistence = false,
            persistenceDbName = "my-app",
            enableBackgroundSync = true,
        )
        assertFalse(config.enablePersistence)
        assertEquals("my-app", config.persistenceDbName)
        assertTrue(config.enableBackgroundSync)
    }
}
