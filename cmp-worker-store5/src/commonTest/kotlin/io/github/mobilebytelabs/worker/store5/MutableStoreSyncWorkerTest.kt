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
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Drives [MutableStoreSyncWorker.doWork] against hand-rolled [MutableStore] fakes.
 *
 * Covers the full response surface of [StoreWriteResponse]:
 *  1. `Success.Typed`        → [WorkResult.Success] (mapped via mapWriteResponseToWorkData).
 *  2. `Success.Untyped`      → [WorkResult.Success].
 *  3. `Error.Exception`+default isRetryable=false → [WorkResult.Failure].
 *  4. `Error.Exception`+override isRetryable=true → [WorkResult.Retry].
 *  5. `Error.Message`        → [WorkResult.Failure] (no retry path for Message).
 *  6. `write` throws         → [WorkResult.Failure] (runCatching onFailure branch).
 *  7. `write` throws + retryable override → [WorkResult.Retry].
 *  8. Default mapWriteResponseToWorkData returns empty [WorkData].
 *  9. Null-message error → fallback copy "Store5 mutation fatal error" / "retryable error".
 */
class MutableStoreSyncWorkerTest {

    @Test
    fun doWork_writeSuccessTyped_returnsSuccess_withMappedWorkData() = runTest {
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Success.Typed(value = "ack-42"),
        )
        val worker = TestMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42) { resp ->
            // Surface the typed payload as WorkData.
            val typed = (resp as StoreWriteResponse.Success.Typed<*>).value
            workDataOf("ack" to typed.toString())
        }

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals("ack-42", (result as WorkResult.Success).outputData.getString("ack"))
    }

    @Test
    fun doWork_writeSuccessUntyped_returnsSuccess() = runTest {
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Success.Untyped(value = "ok"),
        )
        val worker = TestMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
    }

    @Test
    fun doWork_writeErrorException_defaultsToFailure() = runTest {
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Error.Exception(RuntimeException("network-down")),
        )
        val worker = TestMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("network-down", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_writeErrorException_withRetryableOverride_returnsRetry() = runTest {
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Error.Exception(RuntimeException("transient")),
        )
        val worker = RetryableMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("transient", (result as WorkResult.Retry).reason)
    }

    @Test
    fun doWork_writeErrorMessage_alwaysReturnsFailure() = runTest {
        // The Error.Message branch is not retryable by design — no isRetryable check.
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Error.Message("server-validation-failed"),
        )
        val worker = RetryableMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("server-validation-failed", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_writeThrows_returnsFailure_byDefault() = runTest {
        val store = ThrowingMutableStore<String, Int>(RuntimeException("offline"))
        val worker = TestMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("offline", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_writeThrows_withRetryableOverride_returnsRetry() = runTest {
        val store = ThrowingMutableStore<String, Int>(RuntimeException("transient"))
        val worker = RetryableMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("transient", (result as WorkResult.Retry).reason)
    }

    @Test
    fun doWork_defaultMapWriteResponse_returnsEmptyWorkData() = runTest {
        // Subclass that does NOT override mapWriteResponseToWorkData — exercises
        // the open default that returns workDataOf().
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Success.Untyped(value = "ok"),
        )
        val worker = DefaultMappingMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Success)
        assertEquals(WorkData.EMPTY, (result as WorkResult.Success).outputData)
    }

    @Test
    fun doWork_errorExceptionWithNullMessage_fallsBackToDefaultFailureCopy() = runTest {
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Error.Exception(RuntimeException()),
        )
        val worker = TestMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("Store5 mutation fatal error", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_errorExceptionWithNullMessage_retryable_fallsBackToDefaultRetryCopy() = runTest {
        val store = WriteResponseStore<String, Int>(
            response = StoreWriteResponse.Error.Exception(RuntimeException()),
        )
        val worker = RetryableMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("Store5 mutation retryable error", (result as WorkResult.Retry).reason)
    }

    @Test
    fun doWork_writeThrowsWithNullMessage_fallsBackToDefaultFailureCopy() = runTest {
        val store = ThrowingMutableStore<String, Int>(RuntimeException())
        val worker = TestMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Failure)
        assertEquals("Store5 mutation fatal error", (result as WorkResult.Failure).message)
    }

    @Test
    fun doWork_writeThrowsWithNullMessage_retryable_fallsBackToDefaultRetryCopy() = runTest {
        val store = ThrowingMutableStore<String, Int>(RuntimeException())
        val worker = RetryableMutableStoreSyncWorker(fakeCtx(), store, key = "k", value = 42)

        val result = worker.doWork()

        assertTrue(result is WorkResult.Retry)
        assertEquals("Store5 mutation retryable error", (result as WorkResult.Retry).reason)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Test fakes — MutableStore<K, V> has 5+ abstract members across multiple interfaces.
// We implement the minimal set: write(...) on the Write interface (the one the worker
// calls), plus the inherited stream/clear/write-stream stubs that throw / no-op.
// ──────────────────────────────────────────────────────────────────────────────

private fun fetcherOrigin(): StoreReadResponseOrigin = StoreReadResponseOrigin.Fetcher(name = null)

/** Returns [response] on every [MutableStore.write]. */
private class WriteResponseStore<K : Any, V : Any>(private val response: StoreWriteResponse) : MutableStore<K, V> {

    override suspend fun <Response : Any> write(request: StoreWriteRequest<K, V, Response>): StoreWriteResponse =
        response

    override fun <Response : Any> stream(
        requestStream: Flow<StoreWriteRequest<K, V, Response>>,
    ): Flow<StoreWriteResponse> = flow { emit(response) }

    override fun <Response : Any> stream(request: StoreReadRequest<K>): Flow<StoreReadResponse<V>> = flow {
        @Suppress("UNCHECKED_CAST")
        emit(StoreReadResponse.NoNewData(fetcherOrigin()) as StoreReadResponse<V>)
    }

    override suspend fun clear(key: K) = Unit
}

/** Throws [error] on every [MutableStore.write]. */
private class ThrowingMutableStore<K : Any, V : Any>(private val error: Throwable) : MutableStore<K, V> {

    override suspend fun <Response : Any> write(request: StoreWriteRequest<K, V, Response>): StoreWriteResponse =
        throw error

    override fun <Response : Any> stream(
        requestStream: Flow<StoreWriteRequest<K, V, Response>>,
    ): Flow<StoreWriteResponse> = flow { throw error }

    override fun <Response : Any> stream(request: StoreReadRequest<K>): Flow<StoreReadResponse<V>> = flow {
        @Suppress("UNCHECKED_CAST")
        emit(StoreReadResponse.NoNewData(fetcherOrigin()) as StoreReadResponse<V>)
    }

    override suspend fun clear(key: K) = Unit
}

@OptIn(ExperimentalUuidApi::class)
private fun fakeCtx(): WorkerContext = object : WorkerContext {
    override val id: Uuid = Uuid.random()
    override val inputData: WorkData = workDataOf()
    override val tags: Set<String> = emptySet()
    override suspend fun setProgress(progress: WorkProgress) = Unit
}

private class TestMutableStoreSyncWorker(
    context: WorkerContext,
    store: MutableStore<String, Int>,
    key: String,
    value: Int,
    private val mapper: (StoreWriteResponse.Success) -> WorkData = { workDataOf() },
) : MutableStoreSyncWorker<String, Int>(context, store, key, value) {
    override fun mapWriteResponseToWorkData(response: StoreWriteResponse.Success): WorkData = mapper(response)
}

private class DefaultMappingMutableStoreSyncWorker(
    context: WorkerContext,
    store: MutableStore<String, Int>,
    key: String,
    value: Int,
) : MutableStoreSyncWorker<String, Int>(context, store, key, value)

private class RetryableMutableStoreSyncWorker(
    context: WorkerContext,
    store: MutableStore<String, Int>,
    key: String,
    value: Int,
) : MutableStoreSyncWorker<String, Int>(context, store, key, value) {
    override fun Throwable.isRetryable(): Boolean = true
}
