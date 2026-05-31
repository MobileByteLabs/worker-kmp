@file:OptIn(org.mobilenativefoundation.store.core5.ExperimentalStoreApi::class)

package io.github.mobilebytelabs.worker.store5.scheduler

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.scheduler.sync.Syncable
import io.github.mobilebytelabs.worker.scheduler.sync.Synchronizer
import io.github.mobilebytelabs.worker.scheduler.sync.snapshotSync
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * [Syncable] adapter for a Mobile Native Foundation [Store]. On sync, calls `store.fresh(key)`
 * which forces a network fetch and writes the result to the Store's SourceOfTruth (DB),
 * bypassing the memory cache. Versions are bumped to `now.epochSeconds` on success.
 *
 * Use when your sync target is already a Store5 [Store] (read-side cache) — no need to
 * implement [Syncable] on a separate repository class.
 *
 * Example with `cmp-worker-store5`'s registry:
 *
 * ```kotlin
 * val currencyStore: Store<String, FrankfurterResponse> = appStoreRegistry.currencyStore
 *
 * val currencySyncable = StoreSyncable(
 *     name  = "currency-rates",
 *     key   = "USD",
 *     store = currencyStore,
 * )
 *
 * class AppSyncWorker(
 *     ctx: WorkerContext,
 *     persister: SyncStatePersister,
 * ) : AbstractDataSyncWorker(ctx, syncables = listOf(currencySyncable), persister = persister)
 * ```
 *
 * For consumers who need per-payload routing (e.g. `currency.base` from the WorkData),
 * use [Synchronizer.storeSync] directly inside a custom [Syncable] implementation.
 *
 * @param K Store key type.
 * @param V Store output type.
 * @property name unique name used as the [io.github.mobilebytelabs.worker.scheduler.sync.ChangeListVersions]
 *   key; pick something stable so re-runs update the same row.
 * @property key the [Store] key to refresh.
 * @property store the [Store] whose cache should be refreshed from the network.
 */
class StoreSyncable<K : Any, V : Any>(private val name: String, private val key: K, private val store: Store<K, V>) :
    Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean = synchronizer.storeSync(name, key, store)

    /** Ignores [payload] — the key + store are fixed at construction time. */
    override suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean = syncWith(synchronizer)
}

/**
 * Force a Store5 [Store] to refresh from the network and bump the corresponding
 * [io.github.mobilebytelabs.worker.scheduler.sync.ChangeListVersions] entry.
 *
 * Equivalent to [Synchronizer.snapshotSync] but wraps the Store5-specific `.fresh(key)`
 * call. Use this from inside a custom [Syncable] when you need to route the key/payload
 * from [WorkData].
 *
 * Example with payload routing:
 *
 * ```kotlin
 * class CurrencyStoreSyncable(
 *     private val store: Store<String, FrankfurterResponse>,
 * ) : Syncable {
 *     override suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean {
 *         val base = payload.getString("currency.base") ?: "USD"
 *         return synchronizer.storeSync(name = "currency-rates-$base", key = base, store = store)
 *     }
 *     override suspend fun syncWith(synchronizer: Synchronizer) =
 *         syncWith(synchronizer, workDataOf("currency.base" to "USD"))
 * }
 * ```
 */
suspend fun <K : Any, V : Any> Synchronizer.storeSync(name: String, key: K, store: Store<K, V>): Boolean =
    snapshotSync(name) {
        // Force a fresh fetch — Store5 bypasses cache, invokes the fetcher, writes back.
        // Collect the first terminal response (Data or any Error variant); requireData() throws
        // on errors so the runCatching{} in snapshotSync flips to a `false` result.
        val terminal: StoreReadResponse<V> = store
            .stream(StoreReadRequest.fresh(key))
            .first { it is StoreReadResponse.Data || it is StoreReadResponse.Error }
        terminal.requireData()
    }
