package io.github.mobilebytelabs.worker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Coverage tests for [WorkProgress]. NONE / COMPLETE / midpoint are exercised by
 * [WorkManagerTest]; this file completes [WorkProgress.Companion.of], boundary values,
 * the [WorkProgress.data] payload, and data-class operations.
 */
class WorkProgressTest {

    // --- Companion constants ---------------------------------------------

    @Test
    fun none_progressIsZero() {
        assertEquals(0, WorkProgress.NONE.progress)
        assertTrue(WorkProgress.NONE.isIndeterminate)
        assertFalse(WorkProgress.NONE.isComplete)
        assertEquals(WorkData.EMPTY, WorkProgress.NONE.data)
    }

    @Test
    fun complete_progressIs100() {
        assertEquals(100, WorkProgress.COMPLETE.progress)
        assertTrue(WorkProgress.COMPLETE.isComplete)
        assertFalse(WorkProgress.COMPLETE.isIndeterminate)
    }

    // --- of() factory -----------------------------------------------------

    @Test
    fun of_zero_isIndeterminate() {
        val p = WorkProgress.of(0)
        assertEquals(0, p.progress)
        assertTrue(p.isIndeterminate)
    }

    @Test
    fun of_midpoint_returnsExactPercent() {
        val p = WorkProgress.of(50)
        assertEquals(50, p.progress)
        assertFalse(p.isIndeterminate)
        assertFalse(p.isComplete)
    }

    @Test
    fun of_oneHundred_isComplete() {
        val p = WorkProgress.of(100)
        assertTrue(p.isComplete)
    }

    @Test
    fun of_withData_carriesPayload() {
        val payload = workDataOf("processed" to 12)
        val p = WorkProgress.of(25, payload)
        assertEquals(25, p.progress)
        assertEquals(12, p.data.getInt("processed"))
    }

    // --- init throws on out-of-range -------------------------------------

    @Test
    fun init_negativePercent_throws() {
        assertFailsWith<IllegalArgumentException> { WorkProgress(-1) }
    }

    @Test
    fun init_above100_throws() {
        assertFailsWith<IllegalArgumentException> { WorkProgress(101) }
    }

    @Test
    fun of_negative_throwsViaInit() {
        assertFailsWith<IllegalArgumentException> { WorkProgress.of(-5) }
    }

    @Test
    fun of_above100_throwsViaInit() {
        assertFailsWith<IllegalArgumentException> { WorkProgress.of(150) }
    }

    // --- Data class operations -------------------------------------------

    @Test
    fun dataClass_componentN() {
        val data = workDataOf("k" to "v")
        val p = WorkProgress(42, data)
        assertEquals(42, p.component1())
        assertEquals(data, p.component2())
    }

    @Test
    fun copy_overridesProgress() {
        val p = WorkProgress(30)
        val p2 = p.copy(progress = 60)
        assertEquals(60, p2.progress)
        assertNotEquals(p, p2)
    }

    @Test
    fun copy_overridesData() {
        val p = WorkProgress(50)
        val p2 = p.copy(data = workDataOf("step" to "two"))
        assertEquals(50, p2.progress)
        assertEquals("two", p2.data.getString("step"))
    }

    @Test
    fun equals_sameContent_isEqual() {
        val a = WorkProgress(75)
        val b = WorkProgress(75)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun toString_containsClassNameAndProgress() {
        val s = WorkProgress(33).toString()
        assertTrue("WorkProgress" in s)
        assertTrue("33" in s)
    }
}
