@file:OptIn(org.mobilenativefoundation.store.core5.ExperimentalStoreApi::class)

package io.github.mobilebytelabs.worker.store5.scheduler

import io.github.mobilebytelabs.worker.scheduler.sync.ChangeListVersions
import io.github.mobilebytelabs.worker.scheduler.sync.Synchronizer
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the syncWith() bodies on [StoreSyncable] + [MutableStoreSyncable] and the
 * `storeSync` / `mutableStoreSync` extension functions on [Synchronizer]. Pre-existing
 * [StoreSyncableTest] is a smoke test that doesn't actually invoke `syncWith` — these
 * tests close that gap so Kover sees the syncWith / storeSync / mutableStoreSync paths
 * executed.
 */
class StoreSyncableSyncTest {

    @Test
    fun storeSyncable_syncWith_success_returnsTrue_andBumpsVersion() = runTest {
        val sync = FakeSyncer()
        val store = OkStore(value = 42)
        val syncable = StoreSyncable(name = "rates", key = "USD", store = store)

        assertTrue(syncable.syncWith(sync))
        assertTrue(sync.versions.versions.containsKey("rates"))
    }

    @Test
    fun storeSyncable_syncWith_payload_overload_delegates_to_1arg() = runTest {
        val sync = FakeSyncer()
        val store = OkStore(value = 1)
        val syncable = StoreSyncable(name = "x", key = "k", store = store)
        // The payload-overload default impl on Syncable forwards to the 1-arg variant
        // via `syncWith(synchronizer)`. Hits MutableStoreSyncableKt$mutableStoreSync$2$terminal$1 — no, that's storeSync.
        assertTrue(syncable.syncWith(sync, workDataOf("ignored" to "yes")))
    }

    @Test
    fun storeSyncable_syncWith_error_returnsFalse() = runTest {
        val sync = FakeSyncer()
        val store = ErrorStore(error = RuntimeException("network down"))
        val syncable = StoreSyncable(name = "rates", key = "USD", store = store)

        assertFalse(syncable.syncWith(sync))
    }

    @Test
    fun mutableStoreSyncable_syncWith_success_returnsTrue_andBumpsVersion() = runTest {
        val sync = FakeSyncer()
        val store = OkMutableStore<String, Int>()
        val syncable = MutableStoreSyncable(name = "profile", key = "u1", value = 7, mutableStore = store)

        assertTrue(syncable.syncWith(sync))
        assertTrue(sync.versions.versions.containsKey("profile"))
    }

    @Test
    fun mutableStoreSyncable_syncWith_payload_overload_delegates_to_1arg() = runTest {
        val sync = FakeSyncer()
        val store = OkMutableStore<String, Int>()
        val syncable = MutableStoreSyncable(name = "p2", key = "u2", value = 9, mutableStore = store)
        assertTrue(syncable.syncWith(sync, workDataOf("payload" to "ignored")))
    }

    @Test
    fun mutableStoreSyncable_syncWith_writeThrows_returnsFalse() = runTest {
        val sync = FakeSyncer()
        val store = ThrowingMutableStoreLocal<String, Int>(error = RuntimeException("offline"))
        val syncable = MutableStoreSyncable(name = "p3", key = "u3", value = 1, mutableStore = store)
        assertFalse(syncable.syncWith(sync))
    }

    @Test
    fun storeSync_extension_directly_returnsTrue() = runTest {
        val sync = FakeSyncer()
        assertTrue(sync.storeSync(name = "n", key = "k", store = OkStore(value = 99)))
        assertEquals(1, sync.updateCount)
    }

    @Test
    fun mutableStoreSync_extension_directly_returnsTrue() = runTest {
        val sync = FakeSyncer()
        assertTrue(sync.mutableStoreSync(name = "m", key = "k", value = 1, mutableStore = OkMutableStore()))
        assertEquals(1, sync.updateCount)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Fakes
// ──────────────────────────────────────────────────────────────────────────────

private class FakeSyncer : Synchronizer {
    var versions: ChangeListVersions = ChangeListVersions(versions = emptyMap())
    var updateCount: Int = 0
    override suspend fun getChangeListVersions(): ChangeListVersions = versions
    override suspend fun updateChangeListVersions(update: ChangeListVersions.() -> ChangeListVersions) {
        versions = update(versions)
        updateCount++
    }
}

private fun fetcherOriginLocal(): StoreReadResponseOrigin = StoreReadResponseOrigin.Fetcher(name = null)

/** Returns Data(value) on stream. */
private class OkStore(private val value: Int) : Store<String, Int> {
    override fun stream(request: StoreReadRequest<String>): Flow<StoreReadResponse<Int>> =
        flow { emit(StoreReadResponse.Data(value = value, origin = fetcherOriginLocal())) }
    override suspend fun clear(key: String) = Unit
    override suspend fun clear() = Unit
}

/** Returns Error.Exception(error) on stream — requireData() throws → snapshotSync returns false. */
private class ErrorStore(private val error: Throwable) : Store<String, Int> {
    override fun stream(request: StoreReadRequest<String>): Flow<StoreReadResponse<Int>> =
        flow { emit(StoreReadResponse.Error.Exception(error, fetcherOriginLocal())) }
    override suspend fun clear(key: String) = Unit
    override suspend fun clear() = Unit
}

/** Returns Success.Untyped on write. */
private class OkMutableStore<K : Any, V : Any> : MutableStore<K, V> {
    override suspend fun <Response : Any> write(request: StoreWriteRequest<K, V, Response>): StoreWriteResponse =
        StoreWriteResponse.Success.Untyped(value = Unit)

    override fun <Response : Any> stream(
        requestStream: Flow<StoreWriteRequest<K, V, Response>>,
    ): Flow<StoreWriteResponse> = flow { emit(StoreWriteResponse.Success.Untyped(value = Unit)) }

    override fun <Response : Any> stream(request: StoreReadRequest<K>): Flow<StoreReadResponse<V>> = flow {
        @Suppress("UNCHECKED_CAST")
        emit(StoreReadResponse.NoNewData(fetcherOriginLocal()) as StoreReadResponse<V>)
    }

    override suspend fun clear(key: K) = Unit
}

/** Throws on write — snapshotSync flips to false. */
private class ThrowingMutableStoreLocal<K : Any, V : Any>(private val error: Throwable) : MutableStore<K, V> {
    override suspend fun <Response : Any> write(request: StoreWriteRequest<K, V, Response>): StoreWriteResponse =
        throw error

    override fun <Response : Any> stream(
        requestStream: Flow<StoreWriteRequest<K, V, Response>>,
    ): Flow<StoreWriteResponse> = flow { throw error }

    override fun <Response : Any> stream(request: StoreReadRequest<K>): Flow<StoreReadResponse<V>> = flow {
        @Suppress("UNCHECKED_CAST")
        emit(StoreReadResponse.NoNewData(fetcherOriginLocal()) as StoreReadResponse<V>)
    }

    override suspend fun clear(key: K) = Unit
}
