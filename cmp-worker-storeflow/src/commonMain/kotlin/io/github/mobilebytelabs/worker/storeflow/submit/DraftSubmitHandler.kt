package io.github.mobilebytelabs.worker.storeflow.submit

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persistent draft state machine — survives process restarts via the [SubmitOutbox].
 *
 * State transitions:
 * - `IDLE → DRAFTING` (consumer typing/editing — call [draft])
 * - `DRAFTING → SUBMITTING` (handler invoked — call [submit])
 * - `SUBMITTING → SUBMITTED` (success)
 * - `SUBMITTING → FAILED` (exhausted retries / fatal error)
 *
 * Added in v3.0.0-alpha03.X (Phase 3 extension).
 *
 * Adapted from `kmp-project-template/core-base/store/submit/DraftSubmitHandler.kt` —
 * worker-anchored variant. Persistence-restart-resume happens via [rehydrateFromOutbox],
 * called by the consumer on Activity/ViewModel recreation. The actual submit step can
 * be wired through [io.github.mobilebytelabs.worker.WorkManager.enqueue] from
 * [submitBlock] when the consumer wants WorkManager-scheduled retry semantics.
 *
 * **Concurrency**: this class is NOT thread-safe. Callers must invoke [draft], [submit],
 * [rehydrateFromOutbox], [reset] from a single coroutine context (typically a ViewModel
 * scope). State observation is read-only via [state].
 *
 * @param P Serializable payload (the form / mutation being submitted).
 * @param R Server response (currently ignored — the submit-outbox flow is fire-and-forget
 *   for state-machine purposes; payload-typed response handling lands in alpha03.X.Y).
 */
@OptIn(ExperimentalUuidApi::class)
public class DraftSubmitHandler<P : Any, R>(
    private val outbox: SubmitOutbox<P>,
    private val submitBlock: suspend (P) -> R,
) {

    /**
     * The draft lifecycle state machine.
     *
     * The type parameter `P` is covariant so callers can use `State<MyPayload>` as a
     * bound on `State<*>` collections. The [Idle] sentinel is `State<Nothing>` —
     * extends every concrete payload type.
     */
    public sealed class State<out P> {
        /** No draft in progress. */
        public data object Idle : State<Nothing>()

        /** Consumer has staged a payload via [draft] but not yet called [submit]. */
        public data class Drafting<P>(public val payload: P) : State<P>()

        /** [submit] enqueued the draft to the outbox; the submit lambda is running. */
        public data class Submitting<P>(public val id: Uuid, public val payload: P) : State<P>()

        /** Submission succeeded; the outbox entry is marked `SUBMITTED`. */
        public data class Submitted<P>(public val id: Uuid) : State<P>()

        /** Submission failed; the outbox entry is marked `FAILED` with the captured reason. */
        public data class Failed<P>(public val id: Uuid, public val reason: String?) : State<P>()
    }

    private var _state: State<P> = State.Idle

    /** The current state. Read-only; mutations happen via [draft] / [submit] / [reset]. */
    public val state: State<P> get() = _state

    /** Stage a payload for later [submit]. Transitions to [State.Drafting]. */
    public fun draft(payload: P) {
        _state = State.Drafting(payload)
    }

    /**
     * Submit the currently-drafted payload. No-op (returns current state unchanged) if
     * the handler is not in [State.Drafting]. On invocation:
     *
     * 1. Enqueues the payload to the outbox → returns a [Uuid].
     * 2. Marks the outbox entry `RETRYING`.
     * 3. Invokes [submitBlock] with the payload.
     * 4. On success: marks `SUBMITTED` + transitions to [State.Submitted].
     * 5. On throw: marks `FAILED` + transitions to [State.Failed] with the captured message.
     *
     * The returned `R` from [submitBlock] is currently discarded — typed-response handling
     * lands in alpha03.X.Y. The final state value is returned for ergonomic chaining.
     */
    public suspend fun submit(): State<P> {
        val drafting = _state as? State.Drafting<P>
            ?: return _state // Not in drafting; no-op.
        val id = outbox.enqueue(drafting.payload)
        _state = State.Submitting(id, drafting.payload)
        return try {
            outbox.markRetrying(id)
            submitBlock(drafting.payload)
            outbox.markSubmitted(id)
            State.Submitted<P>(id).also { _state = it }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            outbox.markFailed(id, e.message)
            State.Failed<P>(id, e.message).also { _state = it }
        }
    }

    /** Reset to [State.Idle]. Does NOT touch outbox entries — those are owned by the outbox. */
    public fun reset() {
        _state = State.Idle
    }

    /**
     * Restore in-memory state from the outbox — call on process restart / ViewModel
     * recreation. If a pending entry exists, the most-recently-enqueued one is restored
     * to [State.Submitting] so the consumer's UI can offer a "retry / cancel" affordance.
     *
     * Idempotent — safe to call multiple times.
     */
    public suspend fun rehydrateFromOutbox() {
        val pending = outbox.getAllPending()
        val mostRecent = pending.maxByOrNull { it.enqueuedAtMillis }
        if (mostRecent != null) {
            _state = State.Submitting(mostRecent.id, mostRecent.payload)
        }
    }
}
