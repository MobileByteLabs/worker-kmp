package io.github.mobilebytelabs.worker.storeflow.submit

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Outbox for offline-resilient mutations. Consumer enqueues pending submissions;
 * [WorkScheduledOfflineSubmitSyncer] flushes the outbox via a periodic worker on
 * connectivity restoration.
 *
 * Lifted from `kmp-project-template/core-base/store/submit/SubmitOutbox.kt` —
 * worker-anchored adaptation: the persistence backend is platform-pluggable
 * (InMemorySubmitOutbox ships in core; per-platform persistent impls land in
 * v3.0.0-alpha03.X follow-ups).
 *
 * Added in v3.0.0-alpha03.
 *
 * @param P Serializable payload type.
 */
@OptIn(ExperimentalUuidApi::class)
public interface SubmitOutbox<P : Any> {

    public suspend fun enqueue(payload: P): Uuid
    public suspend fun getAllPending(): List<OutboxEntry<P>>
    public suspend fun markRetrying(id: Uuid)
    public suspend fun markSubmitted(id: Uuid)
    public suspend fun markFailed(id: Uuid, reason: String?)
    public suspend fun deleteSubmitted()
}

public data class OutboxEntry<P : Any>(
    public val id: Uuid,
    public val payload: P,
    public val state: OutboxState,
    public val retryCount: Int,
    public val lastFailureReason: String?,
    public val enqueuedAtMillis: Long,
)

public enum class OutboxState { PENDING, RETRYING, SUBMITTED, FAILED }
