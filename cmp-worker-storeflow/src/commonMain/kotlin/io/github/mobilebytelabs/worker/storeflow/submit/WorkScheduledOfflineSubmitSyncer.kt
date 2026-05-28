package io.github.mobilebytelabs.worker.storeflow.submit

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.NetworkType
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.periodicWorkRequest
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Worker-anchored adaptation of `kmp-project-template/core-base/store/submit/OfflineSubmitSyncer.kt`.
 * Upstream Mifos version uses raw `viewModelScope.launch` + `isOnline.collect { retryAll }`.
 * This version delegates the periodic + connectivity-gated retry loop to worker-kmp's WorkManager.
 *
 * Added in v3.0.0-alpha03.
 *
 * Example wiring:
 * ```kotlin
 * val syncer = workScheduledOfflineSubmitSyncer(
 *     workManager = get(),
 *     outbox = roomBackedOutbox,
 *     submitBlock = { payload -> api.submitLoanApplication(payload) },
 * )
 * syncer.start(interval = 15.minutes, requireNetwork = true)
 * ```
 *
 * @param P Serializable payload type that the outbox holds.
 * @param R Result type returned by the server on success (ignored — fire-and-forget).
 */
@OptIn(ExperimentalUuidApi::class)
public class WorkScheduledOfflineSubmitSyncer<P : Any, R> internal constructor(
    private val workManager: WorkManager,
    private val outbox: SubmitOutbox<P>,
    private val submitBlock: suspend (P) -> R,
    private val uniqueWorkName: String,
) {

    public suspend fun start(
        interval: Duration = 15.minutes,
        requireNetwork: Boolean = true,
        existingPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
    ): Uuid {
        val constraints = Constraints {
            if (requireNetwork) setRequiredNetworkType(NetworkType.CONNECTED)
        }
        // Per Phase 3 scope: the actual SyncerWorker that calls outbox.getAllPending() +
        // submitBlock per entry is deferred to alpha03.X — requires the WorkerRegistry
        // integration to bind submitBlock at runtime. For alpha03 scaffold, this records
        // the syncer's intent in WorkManager but the per-payload retry loop is documented
        // as "consumer wires SyncerWorker.doWork() with submitBlock injection".
        return workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            existingPolicy,
            periodicWorkRequest<SyncerWorker>(interval) {
                setConstraints(constraints)
                addTag("storeflow:syncer:$uniqueWorkName")
            },
        )
    }

    public suspend fun stop() {
        workManager.cancelAllWorkByTag("storeflow:syncer:$uniqueWorkName")
    }

    public fun observeRetries(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTag("storeflow:syncer:$uniqueWorkName")
}

public fun <P : Any, R> workScheduledOfflineSubmitSyncer(
    workManager: WorkManager,
    outbox: SubmitOutbox<P>,
    submitBlock: suspend (P) -> R,
    uniqueWorkName: String = "submit-outbox-syncer",
): WorkScheduledOfflineSubmitSyncer<P, R> =
    WorkScheduledOfflineSubmitSyncer(workManager, outbox, submitBlock, uniqueWorkName)

/**
 * Internal worker invoked by the syncer. Looks up the outbox + submitBlock from Koin
 * + processes pending entries. (alpha03.X — see scope_note.)
 */
public class SyncerWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        // Scaffold: actual outbox-flushing wiring lands in alpha03.X follow-up when the
        // Koin-resolution-from-CoroutineWorker pattern is finalized. For alpha03 this
        // worker just succeeds — the syncer infrastructure is registered but no-op.
        return WorkResult.success()
    }
}
