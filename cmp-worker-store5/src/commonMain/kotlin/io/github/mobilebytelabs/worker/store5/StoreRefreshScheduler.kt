package io.github.mobilebytelabs.worker.store5

import io.github.mobilebytelabs.worker.Constraints
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.periodicWorkRequest
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Convenience scheduler — wraps [WorkManager.enqueueUniquePeriodicWork] for
 * [org.mobilenativefoundation.store.store5.Store]-backed workers.
 *
 * Added in v3.0.0-alpha02 (Phase 2 of worker-kmp v3.0.0 epic).
 *
 * Example:
 * ```kotlin
 * val scheduler: StoreRefreshScheduler = get()  // resolved from Koin via workStore5KoinModule
 * scheduler.schedulePeriodicRefresh<UserProfileSyncWorker>(
 *     interval = 6.hours,
 *     uniqueWorkName = "user-profile-sync",
 *     constraints = Constraints.Builder()
 *         .setRequiredNetworkType(NetworkType.CONNECTED)
 *         .build(),
 * )
 * ```
 *
 * Use the [reified] generic to identify the worker type — worker-kmp's `periodicWorkRequest`
 * DSL uses `KClass::simpleName` (the only portable reflection surface across iOS / JS / Wasm /
 * JVM) to wire up the [io.github.mobilebytelabs.worker.registry.WorkerRegistry] lookup.
 */
public class StoreRefreshScheduler(private val workManager: WorkManager) {

    /**
     * Schedules a periodic refresh of the [org.mobilenativefoundation.store.store5.Store]
     * backing worker [T].
     *
     * Tags the request with `store5-refresh:<uniqueWorkName>` so observers and the
     * [cancelRefresh] / [observeRefreshes] paths can find it without knowing the per-platform
     * internal request ID.
     *
     * @param T the [CoroutineWorker] subclass (typically a [StoreBackedWorker]) to schedule.
     * @param interval how often the worker should run.
     * @param uniqueWorkName globally unique name (per [WorkManager.enqueueUniquePeriodicWork]).
     * @param constraints platform conditions that must be met; defaults to [Constraints.NONE].
     * @param existingPolicy what to do if a periodic task with this name already exists;
     *   defaults to [ExistingPeriodicWorkPolicy.KEEP].
     * @return the [Uuid] of the (possibly pre-existing) active work unit.
     */
    @OptIn(ExperimentalUuidApi::class)
    public suspend inline fun <reified T : CoroutineWorker> schedulePeriodicRefresh(
        interval: Duration,
        uniqueWorkName: String,
        constraints: Constraints = Constraints.NONE,
        existingPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
    ): Uuid {
        val request = periodicWorkRequest<T>(repeatInterval = interval) {
            setConstraints(constraints)
            addTag(refreshTag(uniqueWorkName))
        }
        return enqueueUnique(uniqueWorkName, existingPolicy, request)
    }

    /**
     * Cancels every refresh scheduled with the given [uniqueWorkName].
     *
     * Implemented via tag-based cancellation since [WorkManager] does not expose name-prefix
     * queries in the v2.x contract.
     */
    public suspend fun cancelRefresh(uniqueWorkName: String) {
        workManager.cancelAllWorkByTag(refreshTag(uniqueWorkName))
    }

    /**
     * Observes the live state of refreshes scheduled with [uniqueWorkName].
     *
     * @return a hot flow of [WorkInfo] lists matching the scheduler's tag. Cancel the
     *   collecting coroutine to stop observing.
     */
    public fun observeRefreshes(uniqueWorkName: String): Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTag(refreshTag(uniqueWorkName))

    /**
     * Internal forwarder — [schedulePeriodicRefresh] is `inline` to support the reified
     * generic, so it cannot touch [workManager] directly (it's `private`). This
     * [PublishedApi]-tagged member is the seam.
     */
    @OptIn(ExperimentalUuidApi::class)
    @PublishedApi
    internal suspend fun enqueueUnique(
        uniqueWorkName: String,
        existingPolicy: ExistingPeriodicWorkPolicy,
        request: io.github.mobilebytelabs.worker.PeriodicWorkRequest,
    ): Uuid = workManager.enqueueUniquePeriodicWork(uniqueWorkName, existingPolicy, request)

    public companion object {
        /** Tag prefix attached to every Store5-driven refresh. */
        public const val TAG_PREFIX: String = "store5-refresh:"

        /**
         * Builds the deterministic tag for a refresh's [uniqueWorkName]. Exposed via
         * [PublishedApi] so the inline reified [schedulePeriodicRefresh] can call it from
         * consumer compilation units.
         */
        @PublishedApi
        internal fun refreshTag(uniqueWorkName: String): String = "$TAG_PREFIX$uniqueWorkName"
    }
}
