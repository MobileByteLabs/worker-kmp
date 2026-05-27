package io.github.mobilebytelabs.worker

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SPI contract test. v2.2.0 ships the WorkObserver interface + WorkEvent sealed class.
 * Per-platform actual wiring (Android/iOS/Desktop/Web emit events from their work
 * lifecycles) lands as part of Phases 1/7/8/9.
 */
@OptIn(ExperimentalUuidApi::class)
class WorkObserverTest {

    @Test
    fun workObserver_isFunctionalSam_acceptsLambda() {
        val recorded = mutableListOf<WorkEvent>()
        val observer = WorkObserver { event -> recorded += event }

        val id = Uuid.random()
        runTest {
            observer.onEvent(WorkEvent.Enqueued(id = id, tag = "test", inputData = workDataOf()))
            observer.onEvent(WorkEvent.Started(id = id, attemptCount = 1))
            observer.onEvent(WorkEvent.Progress(id = id, progress = WorkProgress(50)))
            observer.onEvent(WorkEvent.Resulted(id = id, result = WorkResult.success(), durationMs = 42L))
        }

        assertEquals(4, recorded.size)
        assertTrue(recorded[0] is WorkEvent.Enqueued)
        assertTrue(recorded[1] is WorkEvent.Started)
        assertTrue(recorded[2] is WorkEvent.Progress)
        assertTrue(recorded[3] is WorkEvent.Resulted)
    }

    @Test
    fun workEvent_enqueued_carriesIdTagAndInputData() {
        val id = Uuid.random()
        val data = workDataOf("key" to "value")
        val event = WorkEvent.Enqueued(id = id, tag = "sync", inputData = data)
        assertEquals(id, event.id)
        assertEquals("sync", event.tag)
        assertEquals("value", event.inputData.getString("key"))
    }

    @Test
    fun workEvent_resulted_carriesIdResultAndDuration() {
        val id = Uuid.random()
        val result = WorkResult.success(workDataOf("out" to 7))
        val event = WorkEvent.Resulted(id = id, result = result, durationMs = 123L)
        assertEquals(id, event.id)
        assertSame(result, event.result)
        assertEquals(123L, event.durationMs)
    }
}
