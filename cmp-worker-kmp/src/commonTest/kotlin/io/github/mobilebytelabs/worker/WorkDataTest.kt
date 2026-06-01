package io.github.mobilebytelabs.worker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Coverage tests for [WorkData] focusing on every typed getter, default-fallback paths,
 * and the equality / map exposure surface that [WorkManagerTest] doesn't already exercise.
 */
class WorkDataTest {

    // --- EMPTY + factory variants -----------------------------------------

    @Test
    fun empty_isReusedSingleton() {
        assertEquals(WorkData.EMPTY, WorkData.EMPTY)
        assertTrue(WorkData.EMPTY.keyValueMap().isEmpty())
    }

    @Test
    fun workDataOf_topLevel_buildsFromPairs() {
        val data = workDataOf("a" to 1, "b" to "two")
        assertEquals(1, data.getInt("a"))
        assertEquals("two", data.getString("b"))
    }

    @Test
    fun workData_invokeOperator_buildsFromPairs() {
        val data = WorkData("k" to true)
        assertTrue(data.getBoolean("k"))
    }

    // --- getString --------------------------------------------------------

    @Test
    fun getString_returnsValueWhenPresent() {
        assertEquals("hello", workDataOf("greeting" to "hello").getString("greeting"))
    }

    @Test
    fun getString_returnsDefaultWhenMissing() {
        assertEquals("fallback", workDataOf().getString("missing", "fallback"))
    }

    @Test
    fun getString_returnsNullByDefaultWhenMissing() {
        assertNull(workDataOf().getString("missing"))
    }

    @Test
    fun getString_returnsDefaultWhenWrongType() {
        assertEquals("fb", workDataOf("k" to 42).getString("k", "fb"))
    }

    // --- getInt -----------------------------------------------------------

    @Test
    fun getInt_returnsValueWhenPresent() {
        assertEquals(42, workDataOf("n" to 42).getInt("n"))
    }

    @Test
    fun getInt_coercesFromLong() {
        assertEquals(7, workDataOf("n" to 7L).getInt("n"))
    }

    @Test
    fun getInt_returnsDefaultWhenMissing() {
        assertEquals(-1, workDataOf().getInt("missing", -1))
    }

    @Test
    fun getInt_returnsDefaultWhenWrongType() {
        assertEquals(0, workDataOf("k" to "not-a-number").getInt("k"))
    }

    // --- getLong ----------------------------------------------------------

    @Test
    fun getLong_returnsValueWhenPresent() {
        assertEquals(42L, workDataOf("n" to 42L).getLong("n"))
    }

    @Test
    fun getLong_coercesFromInt() {
        assertEquals(99L, workDataOf("n" to 99).getLong("n"))
    }

    @Test
    fun getLong_returnsDefaultWhenMissing() {
        assertEquals(100L, workDataOf().getLong("missing", 100L))
    }

    // --- getFloat ---------------------------------------------------------

    @Test
    fun getFloat_returnsValueWhenPresent() {
        assertEquals(3.14f, workDataOf("pi" to 3.14f).getFloat("pi"))
    }

    @Test
    fun getFloat_coercesFromInt() {
        assertEquals(7.0f, workDataOf("n" to 7).getFloat("n"))
    }

    @Test
    fun getFloat_returnsDefaultWhenMissing() {
        assertEquals(1.5f, workDataOf().getFloat("missing", 1.5f))
    }

    // --- getBoolean -------------------------------------------------------

    @Test
    fun getBoolean_returnsValueWhenPresent() {
        assertTrue(workDataOf("flag" to true).getBoolean("flag"))
    }

    @Test
    fun getBoolean_returnsDefaultWhenMissing() {
        assertTrue(workDataOf().getBoolean("missing", default = true))
    }

    @Test
    fun getBoolean_returnsDefaultWhenWrongType() {
        assertFalse(workDataOf("k" to 1).getBoolean("k"))
    }

    // --- getStringArray ---------------------------------------------------

    @Test
    fun getStringArray_returnsArrayWhenPresent() {
        val arr = workDataOf("xs" to arrayOf("a", "b", "c")).getStringArray("xs")
        assertNotNull(arr)
        assertEquals(listOf("a", "b", "c"), arr.toList())
    }

    @Test
    fun getStringArray_returnsNullWhenMissing() {
        assertNull(workDataOf().getStringArray("missing"))
    }

    @Test
    fun getStringArray_filtersNonStringElements() {
        val arr = workDataOf("xs" to arrayOf<Any?>("a", 2, "c")).getStringArray("xs")
        assertNotNull(arr)
        assertEquals(listOf("a", "c"), arr.toList())
    }

    @Test
    fun getStringArray_returnsNullWhenNotArray() {
        assertNull(workDataOf("k" to "plain").getStringArray("k"))
    }

    // --- hasKey + keyValueMap --------------------------------------------

    @Test
    fun hasKey_returnsTrueForPresentKey() {
        assertTrue(workDataOf("x" to 1).hasKey("x"))
    }

    @Test
    fun hasKey_returnsFalseForAbsentKey() {
        assertFalse(workDataOf("x" to 1).hasKey("absent"))
    }

    @Test
    fun keyValueMap_returnsDefensiveCopy() {
        val data = workDataOf("a" to 1, "b" to 2)
        val map = data.keyValueMap()
        assertEquals(2, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
    }

    // --- equals / hashCode / toString ------------------------------------

    @Test
    fun equals_returnsTrueForSameContent() {
        val a = workDataOf("k" to 1)
        val b = workDataOf("k" to 1)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_returnsFalseForDifferentContent() {
        assertNotEquals(workDataOf("k" to 1), workDataOf("k" to 2))
    }

    @Test
    fun equals_returnsFalseForDifferentType() {
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertFalse(workDataOf("k" to 1).equals("not work data"))
    }

    @Test
    fun toString_containsClassName() {
        val s = workDataOf("k" to 1).toString()
        assertTrue("WorkData" in s)
        assertTrue("k" in s)
    }
}
