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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Drives [StoreBackedWorker.doWork] end-to-end against a hand-rolled fake [Store].
 *
 * Covers:
 *  1. `Data` response → [WorkResult.Success] with the mapped [WorkData].
 *  2. `Error.Exception` + default [StoreBackedWorker.isRetryable]=false → [WorkResult.Failure].
 *  3. `Error.Message` → [WorkResult.Failure] (covers the non-Exception error branch).
 *  4. `Error.Exception` + override `isRetryable`=true → [WorkResult.Retry].
 *  5. Default [StoreBackedWorker.mapOutputToWorkData] returns empty [WorkData].
 *  6. Loading / NoNewData responses are filtered (terminal-only collection).
 */
class StoreBackedWorkerTest {

    @Test
    fun doWork_dataResponse_returnsSuccess_withMappedOutput() = runTest {
        val store = fakeStoreEmitting<String, Int>(StoreReadResponse.Data(value = 42, origin = fetcherOrigin()))
        val worker = TestStoreBackedWorker(fakeCtx(), store, key = "k") { output ->
            workDataOf("output" to output)
        }

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals(42, (result as WorkResult.Success).outputData.getInt("output"))
    }

    @Test
    fun doWork_errorException_defaultsToFailure_withErrorMessage() = runTest {
        val store = fakeStoreEmitting<String, Int>(
            StoreReadResponse.Error.Exception(RuntimeException("boom"), fetcherOrigin()),
        )
        val worker = TestStoreBackedWorker(fakeCtx(), store, key = "k")

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("boom", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_errorMessage_returnsFailure_withWrappedMessage() = runTest {
        // The Error.Message branch goes through requireData() → doThrow() →
        // RuntimeException(message). Default isRetryable returns false, so it lands
        // in the Failure branch with the wrapped message threaded through.
        val store = fakeStoreEmitting<String, Int>(
            StoreReadResponse.Error.Message("server-rejected", fetcherOrigin()),
        )
        val worker = TestStoreBackedWorker(fakeCtx(), store, key = "k")

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("server-rejected", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_retryableException_overridesToRetry() = runTest {
        val store = fakeStoreEmitting<String, Int>(
            StoreReadResponse.Error.Exception(RuntimeException("transient"), fetcherOrigin()),
        )
        val worker = RetryableStoreBackedWorker(fakeCtx(), store, key = "k")

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("transient", (result as WorkResult.Retry).reason)
    }

    @Test
    fun doWork_defaultMapOutputToWorkData_returnsEmptyWorkData() = runTest {
        val store = fakeStoreEmitting<String, Int>(StoreReadResponse.Data(value = 7, origin = fetcherOrigin()))
        // Subclass that does NOT override mapOutputToWorkData — exercises the open
        // default in StoreBackedWorker that returns workDataOf().
        val worker = DefaultMappingStoreBackedWorker(fakeCtx(), store, key = "k")

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals(WorkData.EMPTY, (result as WorkResult.Success).outputData)
    }

    @Test
    fun doWork_filtersLoadingAndNoNewData_returnsTerminalData() = runTest {
        // Confirms StoreBackedWorker only acts on Data/Error — Loading + NoNewData are
        // transient and ignored. We emit Loading → NoNewData → Data; the worker must
        // surface the Data.
        val store = object : Store<String, Int> {
            override fun stream(request: StoreReadRequest<String>): Flow<StoreReadResponse<Int>> = flow {
                emit(StoreReadResponse.Loading(fetcherOrigin()))
                emit(StoreReadResponse.NoNewData(fetcherOrigin()))
                emit(StoreReadResponse.Data(value = 99, origin = fetcherOrigin()))
            }
            override suspend fun clear(key: String) = Unit
            override suspend fun clear() = Unit
        }
        val worker = TestStoreBackedWorker(fakeCtx(), store, key = "k") { output ->
            workDataOf("output" to output)
        }

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals(99, (result as WorkResult.Success).outputData.getInt("output"))
    }

    @Test
    fun doWork_errorWithNullMessage_fallsBackToDefaultFailureCopy() = runTest {
        // RuntimeException() has message=null → the worker must use the fallback
        // "Store5 fatal error" string in the Failure.
        val store = fakeStoreEmitting<String, Int>(
            StoreReadResponse.Error.Exception(RuntimeException(), fetcherOrigin()),
        )
        val worker = TestStoreBackedWorker(fakeCtx(), store, key = "k")

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("Store5 fatal error", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_retryableExceptionWithNullMessage_fallsBackToDefaultRetryCopy() = runTest {
        val store = fakeStoreEmitting<String, Int>(
            StoreReadResponse.Error.Exception(RuntimeException(), fetcherOrigin()),
        )
        val worker = RetryableStoreBackedWorker(fakeCtx(), store, key = "k")

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("Store5 retryable error", (result as WorkResult.Retry).reason)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Test fakes — minimal Store<K, V> stubs (3 abstract members: stream, clear(key), clear())
// ──────────────────────────────────────────────────────────────────────────────

private fun fetcherOrigin(): StoreReadResponseOrigin = StoreReadResponseOrigin.Fetcher(name = null)

private fun <K : Any, V : Any> fakeStoreEmitting(response: StoreReadResponse<V>): Store<K, V> = object : Store<K, V> {
    override fun stream(request: StoreReadRequest<K>): Flow<StoreReadResponse<V>> = flow { emit(response) }
    override suspend fun clear(key: K) = Unit
    override suspend fun clear() = Unit
}

@OptIn(ExperimentalUuidApi::class)
private fun fakeCtx(): WorkerContext = object : WorkerContext {
    override val id: Uuid = Uuid.random()
    override val inputData: WorkData = workDataOf()
    override val tags: Set<String> = emptySet()
    override suspend fun setProgress(progress: WorkProgress) = Unit
}

private class TestStoreBackedWorker(
    context: WorkerContext,
    store: Store<String, Int>,
    key: String,
    private val mapper: (Int) -> WorkData = { workDataOf() },
) : StoreBackedWorker<String, Int>(context, store, key) {
    override fun mapOutputToWorkData(output: Int): WorkData = mapper(output)
}

/** Subclass with the default [StoreBackedWorker.mapOutputToWorkData] (no override). */
private class DefaultMappingStoreBackedWorker(context: WorkerContext, store: Store<String, Int>, key: String) :
    StoreBackedWorker<String, Int>(context, store, key)

/** Subclass overriding [StoreBackedWorker.isRetryable] to mark every Throwable as retryable. */
private class RetryableStoreBackedWorker(context: WorkerContext, store: Store<String, Int>, key: String) :
    StoreBackedWorker<String, Int>(context, store, key) {
    override fun Throwable.isRetryable(): Boolean = true
    override fun mapOutputToWorkData(output: Int): WorkData = workDataOf()
}
