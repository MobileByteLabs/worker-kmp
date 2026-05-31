package io.github.mobilebytelabs.worker.scheduler.sync

import io.github.mobilebytelabs.worker.WorkData

/**
 * Wraps any `suspend () -> Unit` fetcher as a [Syncable]. Use when you have a function
 * (not a class) that performs a sync — typically calling an API and writing to a local
 * cache — and don't want to define a whole class implementing [Syncable] just to wrap it.
 *
 * The wrapper delegates to [Synchronizer.snapshotSync], so the success path bumps
 * `ChangeListVersions[name]` to `now.epochSeconds` — same staleness semantics as
 * a hand-rolled repository-style adopter.
 *
 * Example:
 *
 * ```kotlin
 * val currencySyncable = FetcherSyncable("currency-rates") {
 *     dao.upsertAll(api.fetchLatestRates(base = "USD"))
 * }
 *
 * class AppSyncWorker(
 *     ctx: WorkerContext,
 *     persister: SyncStatePersister,
 * ) : AbstractDataSyncWorker(ctx, syncables = listOf(currencySyncable), persister = persister)
 * ```
 *
 * For more control (per-payload routing, custom version semantics), implement [Syncable]
 * directly on your repository class instead.
 *
 * @property name unique name used as the [ChangeListVersions] key; pick something stable
 *   (e.g. the resource name — "currency-rates", "user-prefs") so re-runs update the same row.
 * @property fetcher the suspending block that performs the actual sync (network call +
 *   local write); should throw on transient failure to trigger a retry.
 */
class FetcherSyncable(private val name: String, private val fetcher: suspend (payload: WorkData) -> Unit) : Syncable {

    /** Convenience constructor for fetchers that don't need the payload. */
    constructor(name: String, fetcher: suspend () -> Unit) : this(name, { _ -> fetcher() })

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean =
        syncWith(synchronizer, io.github.mobilebytelabs.worker.workDataOf())

    override suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean =
        synchronizer.snapshotSync(name) { fetcher(payload) }
}

/**
 * Composes multiple [Syncable] instances into one. The composite's [syncWith] iterates
 * children **in declaration order, sequentially** — useful when there's an ordering
 * dependency between syncs (e.g. user-profile must sync before user-content).
 *
 * For parallel fan-out across independent syncables, list them directly in
 * [AbstractDataSyncWorker]'s `syncables` constructor parameter — that path uses
 * `coroutineScope { … async { … } …awaitAll() }`.
 *
 * Returns `true` only if every child returns `true`. Stops at the first `false` or throw.
 *
 * Example:
 *
 * ```kotlin
 * val onboardingSyncable = CompositeSyncable(
 *     SettingsRepo,           // 1st
 *     UserProfileRepo,        // 2nd — depends on settings being current
 *     UserContentRepo,        // 3rd — depends on profile being current
 * )
 * ```
 */
class CompositeSyncable(private val children: List<Syncable>) : Syncable {
    constructor(vararg children: Syncable) : this(children.toList())

    override suspend fun syncWith(synchronizer: Synchronizer): Boolean = children.all { it.syncWith(synchronizer) }

    override suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean =
        children.all { it.syncWith(synchronizer, payload) }
}
