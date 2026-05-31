@file:OptIn(org.mobilenativefoundation.store.core5.ExperimentalStoreApi::class)

package io.github.mobilebytelabs.worker.store5.scheduler

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.scheduler.sync.Syncable
import io.github.mobilebytelabs.worker.scheduler.sync.Synchronizer
import io.github.mobilebytelabs.worker.scheduler.sync.snapshotSync
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreWriteRequest

/**
 * [Syncable] adapter for a Mobile Native Foundation [MutableStore]. On sync, calls
 * `mutableStore.write(StoreWriteRequest.of(key, value))` which triggers the configured
 * `Updater` (push to network) + `Bookkeeper` (mark synced). Versions are bumped to
 * `now.epochSeconds` on success.
 *
 * Use when your sync target is a Store5 [MutableStore] (write-side / mutation queue) —
 * pairs with [StoreSyncable] which handles the read-side. Each instance carries a single
 * (key, value) tuple; for push-many-pending-edits semantics, list multiple instances in
 * [io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker]'s `syncables`
 * constructor parameter (they fan out in parallel via `awaitAll`).
 *
 * Example:
 *
 * ```kotlin
 * val profileEditSyncable = MutableStoreSyncable(
 *     name  = "profile-edit-${edits.id}",
 *     key   = edits.id,
 *     value = edits,
 *     mutableStore = appStoreRegistry.profileMutableStore,
 * )
 *
 * class ProfileSyncWorker(
 *     ctx: WorkerContext,
 *     persister: SyncStatePersister,
 *     pendingEdits: List<MutableStoreSyncable<UserId, UserProfile>>,
 * ) : AbstractDataSyncWorker(ctx, syncables = pendingEdits, persister = persister)
 * ```
 *
 * @param K Store key type.
 * @param V Mutation payload type (the Store's `Output`).
 * @property name unique name used as the [io.github.mobilebytelabs.worker.scheduler.sync.ChangeListVersions]
 *   key.
 * @property key the [MutableStore] key being mutated.
 * @property value the mutation payload to push.
 * @property mutableStore the [MutableStore] handling the write path.
 */
class MutableStoreSyncable<K : Any, V : Any>(
    private val name: String,
    private val key: K,
    private val value: V,
    private val mutableStore: MutableStore<K, V>,
) : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean =
        synchronizer.mutableStoreSync(name, key, value, mutableStore)

    /** Ignores [payload] — the key + value + store are fixed at construction time. */
    override suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean = syncWith(synchronizer)
}

/**
 * Push a (key, value) mutation to a Store5 [MutableStore] and bump the corresponding
 * [io.github.mobilebytelabs.worker.scheduler.sync.ChangeListVersions] entry on success.
 *
 * Equivalent to [Synchronizer.snapshotSync] but wraps the Store5-specific `.write(...)`
 * call. Use this from inside a custom [Syncable] when you need to route the key/value
 * from [WorkData] or pull a pending-edits list from a queue.
 */
suspend fun <K : Any, V : Any> Synchronizer.mutableStoreSync(
    name: String,
    key: K,
    value: V,
    mutableStore: MutableStore<K, V>,
): Boolean = snapshotSync(name) {
    val request: StoreWriteRequest<K, V, Any> = StoreWriteRequest.of(key, value)
    mutableStore.write(request)
}
