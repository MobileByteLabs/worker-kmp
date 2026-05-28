package io.github.mobilebytelabs.worker

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SAM interface for observing worker lifecycle events.
 *
 * Register observers when initializing the platform [WorkManager] — see
 * platform-specific init APIs. Observers receive every [WorkEvent] in order
 * across all enqueued work items.
 *
 * Use cases:
 * - Structured logging (`LoggingWorkObserver` ships in core)
 * - OpenTelemetry / Sentry / Firebase Performance bridges (consumer wires)
 * - Production telemetry (latency, retry rates, failure clustering)
 *
 * `onEvent` is called from the platform's work-execution scope and MUST NOT block.
 * Heavy work (network, disk) should be offloaded to a coroutine or dedicated thread.
 *
 * Added in v2.2.0; per-platform actual emission lands in Phases 1/7/8/9.
 *
 * @see WorkEvent
 * @see io.github.mobilebytelabs.worker.observer.LoggingWorkObserver
 */
public fun interface WorkObserver {
    public suspend fun onEvent(event: WorkEvent)
}

/**
 * Lifecycle events emitted by a platform [WorkManager] to registered [WorkObserver]s.
 *
 * The four event types cover every state transition a single work unit can undergo:
 * [Enqueued] → [Started] → ([Progress])* → [Resulted].
 *
 * Cancellation surfaces as a [Resulted] with `result is WorkResult.Failure` and a
 * cancellation-specific reason; [WorkInfo.State.CANCELLED] is the consumer-side query
 * surface.
 *
 * Added in v2.2.0.
 */
@OptIn(ExperimentalUuidApi::class)
public sealed class WorkEvent {

    /** Identifier of the work unit this event refers to. */
    public abstract val id: Uuid

    /**
     * Work has been accepted by the scheduler and is now [WorkInfo.State.ENQUEUED].
     *
     * @property tag the tag passed to [OneTimeWorkRequestBuilder.addTag] /
     *   [PeriodicWorkRequestBuilder.addTag], or `null` if none.
     * @property inputData the [WorkData] supplied via `setInputData(...)`.
     */
    public data class Enqueued(override val id: Uuid, public val tag: String?, public val inputData: WorkData) :
        WorkEvent()

    /**
     * Work has begun executing — i.e. transitioned to [WorkInfo.State.RUNNING].
     *
     * @property attemptCount 1-indexed attempt counter; >1 indicates a retry.
     */
    public data class Started(override val id: Uuid, public val attemptCount: Int) : WorkEvent()

    /**
     * Worker reported progress via [CoroutineWorker.setProgress].
     */
    public data class Progress(override val id: Uuid, public val progress: WorkProgress) : WorkEvent()

    /**
     * Work reached a terminal state ([WorkResult.Success], [WorkResult.Failure],
     * or [WorkResult.Retry] with attempts exhausted).
     *
     * @property durationMs wall-clock duration from [Started] to this event;
     *   sum across retries.
     */
    public data class Resulted(override val id: Uuid, public val result: WorkResult, public val durationMs: Long) :
        WorkEvent()
}
