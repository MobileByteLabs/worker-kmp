package io.github.mobilebytelabs.worker.compose

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.compose.storeflow.SubmitStateUi
import io.github.mobilebytelabs.worker.compose.storeflow.SubmitStateUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/**
 * Covers every non-`@Composable` executable surface in `cmp-worker-compose`.
 *
 * The Kover root filter excludes `@Composable` functions via `annotatedBy(...)` so
 * UI bodies are off-coverage by design. The artifacts tested here are state-mapping
 * helpers, request builders, enum/data-class declarations — all plain Kotlin.
 */
class NonComposableSurfacesTest {

    // ── SubmitStateUi enum + SubmitStateUiModel data class ────────────────────

    @Test
    fun submitStateUi_hasFiveVariants() {
        assertEquals(5, SubmitStateUi.entries.size)
        assertNotNull(SubmitStateUi.IDLE)
        assertNotNull(SubmitStateUi.DRAFTING)
        assertNotNull(SubmitStateUi.SUBMITTING)
        assertNotNull(SubmitStateUi.SUBMITTED)
        assertNotNull(SubmitStateUi.FAILED)
    }

    @Test
    fun submitStateUiModel_defaultsAndCopy() {
        val model = SubmitStateUiModel(state = SubmitStateUi.IDLE)
        assertEquals(SubmitStateUi.IDLE, model.state)
        assertNull(model.message)
        assertNull(model.onRetry)
        assertNull(model.onCancel)

        var retried = false
        val cancelCount = intArrayOf(0)
        val full = SubmitStateUiModel(
            state = SubmitStateUi.FAILED,
            message = "timeout",
            onRetry = { retried = true },
            onCancel = { cancelCount[0]++ },
        )
        full.onRetry?.invoke()
        full.onCancel?.invoke()
        assertEquals("timeout", full.message)
        assertEquals(true, retried)
        assertEquals(1, cancelCount[0])

        // Exercise data-class generated members (equals/hashCode/copy/toString).
        val same = full.copy()
        assertEquals(full, same)
        assertEquals(full.hashCode(), same.hashCode())
        assertNotNull(full.toString())
    }

    // ── WorkStatusChip non-@Composable extensions (label / icon) ──────────────

    @Test
    fun workStatusChip_label_coversAllStates() {
        assertEquals("Enqueued", WorkInfo.State.ENQUEUED.label())
        assertEquals("Running", WorkInfo.State.RUNNING.label())
        assertEquals("Succeeded", WorkInfo.State.SUCCEEDED.label())
        assertEquals("Failed", WorkInfo.State.FAILED.label())
        assertEquals("Cancelled", WorkInfo.State.CANCELLED.label())
        assertEquals("Blocked", WorkInfo.State.BLOCKED.label())
    }

    @Test
    fun workStatusChip_icon_returnsNullForAllStates() {
        WorkInfo.State.entries.forEach { state ->
            assertNull(state.icon(), "icon() must be null for $state")
        }
    }

    // ── WorkSchedulerScreen internal request builders ─────────────────────────

    @Test
    fun buildOneTimeRequest_withTag_setsTagAndConstraints() {
        val constraints = Constraints { setRequiresCharging(true) }
        val req = buildOneTimeRequest(
            workerClass = "com.example.SyncWorker",
            tag = "sync",
            constraints = constraints,
        )
        assertEquals("com.example.SyncWorker", req.workerClass)
        assertEquals(setOf("sync"), req.tags)
        assertEquals(constraints, req.constraints)
    }

    @Test
    fun buildOneTimeRequest_emptyTag_omitsTagSet() {
        val req = buildOneTimeRequest(
            workerClass = "WorkerA",
            tag = "",
            constraints = Constraints.NONE,
        )
        assertEquals(emptySet(), req.tags)
    }

    @Test
    fun buildPeriodicRequest_withTag_setsTagAndInterval() {
        val constraints = Constraints { setRequiresBatteryNotLow(true) }
        val req = buildPeriodicRequest(
            workerClass = "Heartbeat",
            tag = "hb",
            constraints = constraints,
            interval = 15.minutes,
        )
        assertEquals("Heartbeat", req.workerClass)
        assertEquals(setOf("hb"), req.tags)
        assertEquals(15.minutes, req.repeatInterval)
        assertEquals(constraints, req.constraints)
    }

    @Test
    fun buildPeriodicRequest_emptyTag_omitsTagSet() {
        val req = buildPeriodicRequest(
            workerClass = "Heartbeat",
            tag = "",
            constraints = Constraints.NONE,
            interval = 30.minutes,
        )
        assertEquals(emptySet(), req.tags)
        assertEquals(30.minutes, req.repeatInterval)
    }
}
