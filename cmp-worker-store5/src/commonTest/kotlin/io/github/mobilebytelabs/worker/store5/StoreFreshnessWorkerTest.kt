@file:OptIn(org.mobilenativefoundation.store.core5.ExperimentalStoreApi::class)

package io.github.mobilebytelabs.worker.store5

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin
import org.mobilenativefoundation.store.store5.Validator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Drives [StoreFreshnessWorker.doWork] against a recording fake [Store] + [Validator].
 *
 * Covers:
 *  1. Cached value present + validator says valid → skip fetch, return Success("skipped"→"fresh").
 *  2. Cached value present but validator rejects → force fresh path → Success with empty WorkData.
 *  3. No cached value (NoNewData) → force fresh path → Success.
 *  4. Stale + fresh fetch fails → Failure (default isRetryable=false).
 *  5. Override `isRetryable`=true → Retry.
 *  6. KEY_SKIPPED / VALUE_FRESH companion constants are stable.
 */
class StoreFreshnessWorkerTest {

    @Test
    fun doWork_cacheFresh_skipsFetch_returnsSuccessWithSkippedFresh() = runTest {
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.Data(value = 42, origin = StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Data(value = 99, origin = fetcherOrigin()),
        )
        val worker = TestStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysValid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals(
            StoreFreshnessWorker.VALUE_FRESH,
            (result as WorkResult.Success).outputData.getString(StoreFreshnessWorker.KEY_SKIPPED),
        )
        // Skip path means the cached stream was queried but the fresh stream was NOT.
        assertEquals(1, store.cachedStreamCalls)
        assertEquals(0, store.freshStreamCalls)
    }

    @Test
    fun doWork_cacheStale_triggersFreshFetch_returnsSuccessWithEmptyData() = runTest {
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.Data(value = 42, origin = StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Data(value = 99, origin = fetcherOrigin()),
        )
        val worker = TestStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysInvalid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        // Fresh path returns empty data (consumers rely on the Store cache write side-effect)
        assertEquals(WorkData.EMPTY, (result as WorkResult.Success).outputData)
        assertEquals(1, store.cachedStreamCalls)
        assertEquals(1, store.freshStreamCalls)
    }

    @Test
    fun doWork_noCachedValue_forcesFreshFetch_returnsSuccess() = runTest {
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.NoNewData(StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Data(value = 99, origin = fetcherOrigin()),
        )
        val worker = TestStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysValid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals(WorkData.EMPTY, (result as WorkResult.Success).outputData)
        // No cached data → validator never even ran on a value → force fresh fetch
        assertEquals(1, store.cachedStreamCalls)
        assertEquals(1, store.freshStreamCalls)
    }

    @Test
    fun doWork_cachedStreamErrors_treatsAsStale_forcesFreshFetch() = runTest {
        // readCached() catches errors → returns null → treated as stale.
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.Error.Message("cache-miss", StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Data(value = 99, origin = fetcherOrigin()),
        )
        val worker = TestStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysValid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals(WorkData.EMPTY, (result as WorkResult.Success).outputData)
        assertEquals(1, store.freshStreamCalls)
    }

    @Test
    fun doWork_freshFetchFails_returnsFailure_byDefault() = runTest {
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.NoNewData(StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Error.Exception(RuntimeException("offline"), fetcherOrigin()),
        )
        val worker = TestStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysValid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("offline", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_freshFetchFailsWithRetryableOverride_returnsRetry() = runTest {
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.NoNewData(StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Error.Exception(RuntimeException("transient"), fetcherOrigin()),
        )
        val worker = RetryableStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysValid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("transient", (result as WorkResult.Retry).reason)
    }

    @Test
    fun doWork_freshFetchFailsWithNullMessage_fallsBackToDefaultCopy() = runTest {
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.NoNewData(StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Error.Exception(RuntimeException(), fetcherOrigin()),
        )
        val worker = TestStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysValid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("Store5 fatal error", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_freshFetchFailsRetryableWithNullMessage_fallsBackToDefaultRetryCopy() = runTest {
        val store = RecordingStore<String, Int>(
            cached = StoreReadResponse.NoNewData(StoreReadResponseOrigin.Cache),
            fresh = StoreReadResponse.Error.Exception(RuntimeException(), fetcherOrigin()),
        )
        val worker = RetryableStoreFreshnessWorker(fakeCtx(), store, key = "k", validator = AlwaysValid())

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("Store5 retryable error", (result as WorkResult.Retry).reason)
    }

    @Test
    fun companionConstants_areStable() {
        assertEquals("skipped", StoreFreshnessWorker.KEY_SKIPPED)
        assertEquals("fresh", StoreFreshnessWorker.VALUE_FRESH)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Test fakes
// ──────────────────────────────────────────────────────────────────────────────

private fun fetcherOrigin(): StoreReadResponseOrigin = StoreReadResponseOrigin.Fetcher(name = null)

/**
 * Recording fake [Store] that returns a different response on cached-stream vs fresh-stream
 * requests (distinguished by [StoreReadRequest.refresh]). Increments per-call counters
 * for assertion.
 */
private class RecordingStore<K : Any, V : Any>(
    private val cached: StoreReadResponse<V>,
    private val fresh: StoreReadResponse<V>,
) : Store<K, V> {
    var cachedStreamCalls: Int = 0
        private set
    var freshStreamCalls: Int = 0
        private set

    override fun stream(request: StoreReadRequest<K>): Flow<StoreReadResponse<V>> = flow {
        if (request.refresh) {
            freshStreamCalls += 1
            emit(fresh)
        } else {
            cachedStreamCalls += 1
            emit(cached)
        }
    }

    override suspend fun clear(key: K) = Unit
    override suspend fun clear() = Unit
}

private class AlwaysValid<T : Any> : Validator<T> {
    override suspend fun isValid(item: T): Boolean = true
}

private class AlwaysInvalid<T : Any> : Validator<T> {
    override suspend fun isValid(item: T): Boolean = false
}

@OptIn(ExperimentalUuidApi::class)
private fun fakeCtx(): WorkerContext = object : WorkerContext {
    override val id: Uuid = Uuid.random()
    override val inputData: WorkData = workDataOf()
    override val tags: Set<String> = emptySet()
    override suspend fun setProgress(progress: WorkProgress) = Unit
}

private class TestStoreFreshnessWorker(
    context: WorkerContext,
    store: Store<String, Int>,
    key: String,
    validator: Validator<Int>,
) : StoreFreshnessWorker<String, Int>(context, store, key, validator)

private class RetryableStoreFreshnessWorker(
    context: WorkerContext,
    store: Store<String, Int>,
    key: String,
    validator: Validator<Int>,
) : StoreFreshnessWorker<String, Int>(context, store, key, validator) {
    override fun Throwable.isRetryable(): Boolean = true
}
