package io.github.mobilebytelabs.worker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi

/**
 * Exercises the `block: ... -> Unit = {}` default-parameter lambda paths on
 * [oneTimeWorkRequest] / [periodicWorkRequest] — every other test in
 * `WorkManagerTest` supplies a non-empty block, leaving the default-empty lambda
 * (`WorkRequestKt$oneTimeWorkRequest$1`) uncovered. Phase 8 closes that gap.
 *
 * Also pins the unparameterized `copy()`-via-data-class paths that surface as
 * "1 missed line" on [OneTimeWorkRequest] / [PeriodicWorkRequest] in the
 * aggregate Kover report.
 */
@OptIn(ExperimentalUuidApi::class)
class WorkRequestDefaultBlockTest {

    private class NoOpWorker(ctx: WorkerContext) : CoroutineWorker(ctx) {
        override suspend fun doWork(): WorkResult = WorkResult.success()
    }

    @Test
    fun oneTimeWorkRequest_default_block_returnsRequestWithDefaults() {
        val r = oneTimeWorkRequest<NoOpWorker>()
        assertEquals("NoOpWorker", r.workerClass)
        assertEquals(WorkData.EMPTY, r.inputData)
        assertEquals(Constraints.NONE, r.constraints)
        assertTrue(r.tags.isEmpty())
    }

    @Test
    fun periodicWorkRequest_default_block_returnsRequestWithDefaults() {
        val r = periodicWorkRequest<NoOpWorker>(repeatInterval = 15.minutes)
        assertEquals("NoOpWorker", r.workerClass)
        assertEquals(15.minutes, r.repeatInterval)
        assertEquals(WorkData.EMPTY, r.inputData)
    }

    @Test
    fun oneTimeWorkRequest_dataClass_copy_propagatesField() {
        val original = oneTimeWorkRequest<NoOpWorker> { addTag("a") }
        val copy = original.copy(tags = original.tags + "b")
        assertEquals(setOf("a", "b"), copy.tags)
        assertEquals(original.workerClass, copy.workerClass)
    }

    @Test
    fun periodicWorkRequest_dataClass_copy_propagatesField() {
        val original = periodicWorkRequest<NoOpWorker>(15.minutes) { addTag("p") }
        val copy = original.copy(quickRefresh = !original.quickRefresh)
        assertEquals(setOf("p"), copy.tags)
        assertEquals(15.minutes, copy.repeatInterval)
    }

    // The next two tests construct the data classes via their internal primary
    // constructor WITHOUT supplying the default-valued tail parameters
    // (initialDelay / expeditedPolicy on OneTime; flexTimeInterval /
    // initialDelay / quickRefresh on Periodic). This exercises the synthetic
    // `<init>$default(...)` branch the compiler emits for default-parameter
    // dispatch — the missing 1 line per data class in the aggregate Kover
    // report. Builders always set every field explicitly, so they never reach
    // this path.

    @Test
    fun oneTimeWorkRequest_directConstructor_defaultParams_yieldDefaults() {
        val r = OneTimeWorkRequest(
            id = kotlin.uuid.Uuid.random(),
            workerClass = "X",
            inputData = WorkData.EMPTY,
            constraints = Constraints.NONE,
            retryConfig = RetryConfig.DEFAULT,
            tags = emptySet(),
        )
        assertEquals(kotlin.time.Duration.ZERO, r.initialDelay)
        assertEquals(null, r.expeditedPolicy)
    }

    @Test
    fun periodicWorkRequest_directConstructor_defaultParams_yieldDefaults() {
        val r = PeriodicWorkRequest(
            id = kotlin.uuid.Uuid.random(),
            workerClass = "Y",
            inputData = WorkData.EMPTY,
            constraints = Constraints.NONE,
            retryConfig = RetryConfig.DEFAULT,
            tags = emptySet(),
            repeatInterval = 15.minutes,
        )
        assertEquals(kotlin.time.Duration.ZERO, r.flexTimeInterval)
        assertEquals(kotlin.time.Duration.ZERO, r.initialDelay)
        assertEquals(false, r.quickRefresh)
    }
}
