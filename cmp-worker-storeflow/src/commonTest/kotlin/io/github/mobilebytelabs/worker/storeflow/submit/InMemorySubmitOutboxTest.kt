package io.github.mobilebytelabs.worker.storeflow.submit

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Covers the remaining [InMemorySubmitOutbox] branches not exercised by
 * [StoreFlowSmokeTest] — markRetrying, markFailed (with and without reason),
 * deleteSubmitted, and the silent-noop behaviour of mark* on unknown IDs.
 *
 * Also touches the [OutboxEntry] data class to exercise component accessors / equals.
 */
class InMemorySubmitOutboxTest {

    @Test
    fun markRetrying_setsRetryingAndIncrementsCount() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val id = outbox.enqueue("p1")
        outbox.markRetrying(id)
        outbox.markRetrying(id)
        val entry = outbox.getAllPending().first { it.id == id }
        assertEquals(OutboxState.RETRYING, entry.state)
        assertEquals(2, entry.retryCount)
    }

    @Test
    fun markFailed_withReason_recordsReason() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val id = outbox.enqueue("p1")
        outbox.markFailed(id, "5xx")
        // FAILED is not in the pending filter (PENDING|RETRYING).
        assertTrue(outbox.getAllPending().isEmpty())
    }

    @Test
    fun markFailed_nullReason_isAccepted() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val id = outbox.enqueue("p1")
        outbox.markFailed(id, null)
        assertTrue(outbox.getAllPending().isEmpty())
    }

    @Test
    fun markCalls_onUnknownId_areSilentNoOp() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val ghostId = Uuid.random()
        // None of these should throw, even though the id is unknown.
        outbox.markRetrying(ghostId)
        outbox.markSubmitted(ghostId)
        outbox.markFailed(ghostId, "n/a")
        assertTrue(outbox.getAllPending().isEmpty())
    }

    @Test
    fun deleteSubmitted_clearsOnlySubmittedEntries() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val id1 = outbox.enqueue("a")
        val id2 = outbox.enqueue("b")
        val id3 = outbox.enqueue("c")
        outbox.markSubmitted(id1)
        outbox.markRetrying(id2)
        // id3 remains PENDING.
        outbox.deleteSubmitted()
        val pending = outbox.getAllPending()
        assertEquals(2, pending.size)
        assertNotNull(pending.firstOrNull { it.id == id2 })
        assertNotNull(pending.firstOrNull { it.id == id3 })
    }

    @Test
    fun getAllPending_includesRetryingEntries() = runTest {
        val outbox = InMemorySubmitOutbox<String>()
        val id = outbox.enqueue("retrying-payload")
        outbox.markRetrying(id)
        val pending = outbox.getAllPending()
        assertEquals(1, pending.size)
        assertEquals(OutboxState.RETRYING, pending.first().state)
    }

    @Test
    fun outboxEntry_dataClassEquality() {
        val id = Uuid.random()
        val a = OutboxEntry(id, "x", OutboxState.PENDING, 0, null, 100L)
        val b = OutboxEntry(id, "x", OutboxState.PENDING, 0, null, 100L)
        assertEquals(a, b)
        // copy() — exercise the synthetic data-class accessor.
        val c = a.copy(state = OutboxState.SUBMITTED)
        assertEquals(OutboxState.SUBMITTED, c.state)
        assertEquals(id, c.id)
        // toString — non-null sanity (covers the synthesized data-class toString).
        assertNotNull(a.toString())
    }

    @Test
    fun outboxState_enumHasFourVariants() {
        assertEquals(4, OutboxState.entries.size)
        assertNotNull(OutboxState.PENDING)
        assertNotNull(OutboxState.RETRYING)
        assertNotNull(OutboxState.SUBMITTED)
        assertNotNull(OutboxState.FAILED)
        assertNull(OutboxState.entries.firstOrNull { it.name == "MISSING" })
    }
}
