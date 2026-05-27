package io.github.mobilebytelabs.worker.storeflow.submit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * In-memory [SubmitOutbox] — default; loses state on process death.
 * For production use, swap for a per-platform persistent implementation (alpha03.X).
 */
@OptIn(ExperimentalUuidApi::class)
public class InMemorySubmitOutbox<P : Any> : SubmitOutbox<P> {

    private val entries = linkedMapOf<Uuid, OutboxEntry<P>>()
    private val mutex = Mutex()
    private val now: () -> Long = { kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds }

    override suspend fun enqueue(payload: P): Uuid = mutex.withLock {
        val id = Uuid.random()
        entries[id] = OutboxEntry(id, payload, OutboxState.PENDING, 0, null, now())
        id
    }

    override suspend fun getAllPending(): List<OutboxEntry<P>> = mutex.withLock {
        entries.values.filter { it.state == OutboxState.PENDING || it.state == OutboxState.RETRYING }
    }

    override suspend fun markRetrying(id: Uuid) = mutex.withLock {
        entries[id]?.let { entries[id] = it.copy(state = OutboxState.RETRYING, retryCount = it.retryCount + 1) }
        Unit
    }

    override suspend fun markSubmitted(id: Uuid) = mutex.withLock {
        entries[id]?.let { entries[id] = it.copy(state = OutboxState.SUBMITTED) }
        Unit
    }

    override suspend fun markFailed(id: Uuid, reason: String?) = mutex.withLock {
        entries[id]?.let { entries[id] = it.copy(state = OutboxState.FAILED, lastFailureReason = reason) }
        Unit
    }

    override suspend fun deleteSubmitted() = mutex.withLock {
        entries.entries.removeAll { it.value.state == OutboxState.SUBMITTED }
        Unit
    }
}
