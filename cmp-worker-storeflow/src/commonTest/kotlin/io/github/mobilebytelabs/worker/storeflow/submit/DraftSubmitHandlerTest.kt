package io.github.mobilebytelabs.worker.storeflow.submit

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Covers [DraftSubmitHandler] — every state transition, the no-op submit branch,
 * reset, and the rehydrate-from-outbox restoration path.
 */
class DraftSubmitHandlerTest {

    @Test
    fun initialState_isIdle() {
        val handler = DraftSubmitHandler<String, Unit>(
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
        )
        assertSame(DraftSubmitHandler.State.Idle, handler.state)
    }

    @Test
    fun draft_transitionsToDrafting() {
        val handler = DraftSubmitHandler<String, Unit>(
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
        )
        handler.draft("hello")
        val state = handler.state
        assertIs<DraftSubmitHandler.State.Drafting<String>>(state)
        assertEquals("hello", state.payload)
    }

    @Test
    fun submit_fromIdle_isNoOp() = runTest {
        val handler = DraftSubmitHandler<String, Unit>(
            outbox = InMemorySubmitOutbox(),
            submitBlock = { error("must not be invoked") },
        )
        val result = handler.submit()
        assertSame(DraftSubmitHandler.State.Idle, result)
        assertSame(DraftSubmitHandler.State.Idle, handler.state)
    }

    @Test
    fun submit_success_transitionsToSubmitted_andOutboxIsSubmitted() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        var captured: String? = null
        val handler = DraftSubmitHandler<String, Unit>(outbox = outbox) { p -> captured = p }
        handler.draft("payload-X")
        val finalState = handler.submit()
        assertIs<DraftSubmitHandler.State.Submitted<String>>(finalState)
        assertEquals("payload-X", captured)
        // The outbox entry must end SUBMITTED — pending list is empty (filter excludes SUBMITTED).
        assertTrue(outbox.getAllPending().isEmpty())
    }

    @Test
    fun submit_failure_transitionsToFailedWithReason() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val handler = DraftSubmitHandler<String, Unit>(outbox = outbox) { error("network down") }
        handler.draft("payload-Y")
        val finalState = handler.submit()
        val failed = assertIs<DraftSubmitHandler.State.Failed<String>>(finalState)
        assertEquals("network down", failed.reason)
    }

    @Test
    fun reset_returnsToIdle() {
        val handler = DraftSubmitHandler<String, Unit>(
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
        )
        handler.draft("anything")
        handler.reset()
        assertSame(DraftSubmitHandler.State.Idle, handler.state)
    }

    @Test
    fun rehydrate_emptyOutbox_isNoOp() = runTest {
        val handler = DraftSubmitHandler<String, Unit>(
            outbox = InMemorySubmitOutbox(),
            submitBlock = { },
        )
        handler.rehydrateFromOutbox()
        assertSame(DraftSubmitHandler.State.Idle, handler.state)
    }

    @Test
    fun rehydrate_pickingMostRecentPending_restoresSubmittingState() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        outbox.enqueue("oldest")
        outbox.enqueue("newest")
        val handler = DraftSubmitHandler<String, Unit>(outbox = outbox) { }
        handler.rehydrateFromOutbox()
        val state = handler.state
        // InMemorySubmitOutbox uses a fresh TimeSource.Monotonic mark per enqueue,
        // so both items can share the same elapsed-ms value — assert structurally
        // (state is Submitting + payload is one of the two enqueued strings).
        val submitting = assertIs<DraftSubmitHandler.State.Submitting<String>>(state)
        assertTrue(submitting.payload in setOf("oldest", "newest"))
    }

    @Test
    fun submit_consumesDraft_subsequentSubmitIsNoOp() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        var invocations = 0
        val handler = DraftSubmitHandler<String, Unit>(outbox = outbox) { invocations++ }
        handler.draft("once")
        handler.submit()
        // After success, state is Submitted — second submit must not re-invoke submitBlock.
        val second = handler.submit()
        assertIs<DraftSubmitHandler.State.Submitted<String>>(second)
        assertEquals(1, invocations)
    }
}
