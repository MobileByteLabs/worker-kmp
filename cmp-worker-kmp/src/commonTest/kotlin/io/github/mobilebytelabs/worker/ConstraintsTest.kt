package io.github.mobilebytelabs.worker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Coverage tests for [Constraints], [Constraints.Builder], [NetworkType], [ContentUriTrigger]
 * and [ExistingPeriodicWorkPolicy]. Targets every Builder method + every enum entry per
 * Phase 4 (kover-100-coverage) GOAL.md AC #10.
 */
class ConstraintsTest {

    // --- Constraints.NONE -------------------------------------------------

    @Test
    fun none_hasAllDefaults() {
        val c = Constraints.NONE
        assertEquals(NetworkType.NOT_REQUIRED, c.requiredNetworkType)
        assertFalse(c.requiresCharging)
        assertFalse(c.requiresDeviceIdle)
        assertFalse(c.requiresBatteryNotLow)
        assertFalse(c.requiresStorageNotLow)
        assertTrue(c.contentUriTriggers.isEmpty())
    }

    // --- Constraints.invoke DSL + every NetworkType variant --------------

    @Test
    fun builder_setRequiredNetworkType_NOT_REQUIRED() {
        val c = Constraints { setRequiredNetworkType(NetworkType.NOT_REQUIRED) }
        assertEquals(NetworkType.NOT_REQUIRED, c.requiredNetworkType)
    }

    @Test
    fun builder_setRequiredNetworkType_CONNECTED() {
        val c = Constraints { setRequiredNetworkType(NetworkType.CONNECTED) }
        assertEquals(NetworkType.CONNECTED, c.requiredNetworkType)
    }

    @Test
    fun builder_setRequiredNetworkType_UNMETERED() {
        val c = Constraints { setRequiredNetworkType(NetworkType.UNMETERED) }
        assertEquals(NetworkType.UNMETERED, c.requiredNetworkType)
    }

    @Test
    fun builder_setRequiredNetworkType_NOT_ROAMING() {
        val c = Constraints { setRequiredNetworkType(NetworkType.NOT_ROAMING) }
        assertEquals(NetworkType.NOT_ROAMING, c.requiredNetworkType)
    }

    @Test
    fun builder_setRequiredNetworkType_METERED() {
        val c = Constraints { setRequiredNetworkType(NetworkType.METERED) }
        assertEquals(NetworkType.METERED, c.requiredNetworkType)
    }

    @Test
    fun networkType_entries_hasFiveValues() {
        assertEquals(5, NetworkType.entries.size)
    }

    @Test
    fun networkType_valueOf_roundTrip() {
        for (entry in NetworkType.entries) {
            assertEquals(entry, NetworkType.valueOf(entry.name))
        }
    }

    // --- Builder boolean setters -----------------------------------------

    @Test
    fun builder_setRequiresCharging_true() {
        assertTrue(Constraints { setRequiresCharging(true) }.requiresCharging)
    }

    @Test
    fun builder_setRequiresCharging_false() {
        assertFalse(Constraints { setRequiresCharging(false) }.requiresCharging)
    }

    @Test
    fun builder_setRequiresDeviceIdle_true() {
        assertTrue(Constraints { setRequiresDeviceIdle(true) }.requiresDeviceIdle)
    }

    @Test
    fun builder_setRequiresBatteryNotLow_true() {
        assertTrue(Constraints { setRequiresBatteryNotLow(true) }.requiresBatteryNotLow)
    }

    @Test
    fun builder_setRequiresStorageNotLow_true() {
        assertTrue(Constraints { setRequiresStorageNotLow(true) }.requiresStorageNotLow)
    }

    // --- Builder addContentUriTrigger ------------------------------------

    @Test
    fun builder_addContentUriTrigger_oneEntry() {
        val c = Constraints {
            addContentUriTrigger("content://com.example/items", triggerForDescendants = true)
        }
        assertEquals(1, c.contentUriTriggers.size)
        val trig = c.contentUriTriggers.first()
        assertEquals("content://com.example/items", trig.uriString)
        assertTrue(trig.triggerForDescendants)
    }

    @Test
    fun builder_addContentUriTrigger_multipleEntries() {
        val c = Constraints {
            addContentUriTrigger("content://a", triggerForDescendants = false)
            addContentUriTrigger("content://b", triggerForDescendants = true)
        }
        assertEquals(2, c.contentUriTriggers.size)
        assertFalse(c.contentUriTriggers[0].triggerForDescendants)
        assertTrue(c.contentUriTriggers[1].triggerForDescendants)
    }

    // --- Builder.build() via direct instantiation ------------------------

    @Test
    fun builder_directBuild_isEquivalentToInvokeDsl() {
        val viaBuilder = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()
        val viaDsl = Constraints {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }
        assertEquals(viaDsl, viaBuilder)
    }

    // --- equals + hashCode -----------------------------------------------

    @Test
    fun equals_sameContent_returnsTrue() {
        val a = Constraints { setRequiresCharging(true) }
        val b = Constraints { setRequiresCharging(true) }
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_sameInstance_returnsTrue() {
        val c = Constraints.NONE
        @Suppress("KotlinConstantConditions")
        assertTrue(c.equals(c))
    }

    @Test
    fun equals_differentContent_returnsFalse() {
        val a = Constraints.NONE
        val b = Constraints { setRequiresCharging(true) }
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentType_returnsFalse() {
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertFalse(Constraints.NONE.equals("not constraints"))
    }

    @Test
    fun equals_differingOnEveryField() {
        assertNotEquals(
            Constraints.NONE,
            Constraints { setRequiredNetworkType(NetworkType.CONNECTED) },
        )
        assertNotEquals(
            Constraints.NONE,
            Constraints { setRequiresDeviceIdle(true) },
        )
        assertNotEquals(
            Constraints.NONE,
            Constraints { setRequiresBatteryNotLow(true) },
        )
        assertNotEquals(
            Constraints.NONE,
            Constraints { setRequiresStorageNotLow(true) },
        )
        assertNotEquals(
            Constraints.NONE,
            Constraints { addContentUriTrigger("uri", false) },
        )
    }

    // --- ContentUriTrigger (data class) ----------------------------------

    @Test
    fun contentUriTrigger_dataClass_componentsAndCopy() {
        val t = ContentUriTrigger("content://x", true)
        assertEquals("content://x", t.component1())
        assertTrue(t.component2())
        val t2 = t.copy(triggerForDescendants = false)
        assertEquals("content://x", t2.uriString)
        assertFalse(t2.triggerForDescendants)
        assertNotEquals(t, t2)
        assertTrue("ContentUriTrigger" in t.toString())
    }

    // --- ExistingPeriodicWorkPolicy --------------------------------------

    @Test
    fun existingPeriodicWorkPolicy_entries_hasThreeValues() {
        assertEquals(3, ExistingPeriodicWorkPolicy.entries.size)
        assertTrue(ExistingPeriodicWorkPolicy.KEEP in ExistingPeriodicWorkPolicy.entries)
        assertTrue(ExistingPeriodicWorkPolicy.REPLACE in ExistingPeriodicWorkPolicy.entries)
        assertTrue(ExistingPeriodicWorkPolicy.UPDATE in ExistingPeriodicWorkPolicy.entries)
    }

    @Test
    fun existingPeriodicWorkPolicy_valueOf_roundTrip() {
        for (entry in ExistingPeriodicWorkPolicy.entries) {
            assertEquals(entry, ExistingPeriodicWorkPolicy.valueOf(entry.name))
        }
    }
}
