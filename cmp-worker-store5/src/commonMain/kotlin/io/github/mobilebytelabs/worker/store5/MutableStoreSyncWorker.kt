package io.github.mobilebytelabs.worker.store5

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse

/**
 * [CoroutineWorker] that pushes pending mutations to the network via a Store5
 * [MutableStore]. On [doWork], invokes `mutableStore.write(StoreWriteRequest.of(key, value))`
 * which triggers the configured `Updater` + `Bookkeeper`. Maps Store5 errors to
 * [WorkResult.retry] or [WorkResult.failure] via [isRetryable].
 *
 * Added in v3.0.0-alpha02.X (Phase 2 extension).
 *
 * Example consumer:
 * ```kotlin
 * class ProfileEditSyncWorker(
 *     context: WorkerContext,
 *     mutableStore: MutableStore<UserId, UserProfile>,
 *     key: UserId,
 *     val edits: UserProfile,
 * ) : MutableStoreSyncWorker<UserId, UserProfile>(context, mutableStore, key, edits)
 * ```
 *
 * Note: Store5's [MutableStore] is annotated with `@ExperimentalStoreApi`; this worker
 * propagates that opt-in to subclasses via the class-level [OptIn]. Consumers are
 * responsible for understanding the API stability guarantees.
 *
 * @param K Store key type.
 * @param V Mutation payload type (the Store's `Output` / "Input" type).
 */
@OptIn(ExperimentalStoreApi::class)
public abstract class MutableStoreSyncWorker<K : Any, V : Any>(
    context: WorkerContext,
    private val mutableStore: MutableStore<K, V>,
    private val key: K,
    private val value: V,
) : CoroutineWorker(context) {

    override suspend fun doWork(): WorkResult {
        return runCatching {
            val request: StoreWriteRequest<K, V, Any> = StoreWriteRequest.of(key, value)
            mutableStore.write(request)
        }.fold(
            onSuccess = { response ->
                when (response) {
                    is StoreWriteResponse.Success -> WorkResult.success(mapWriteResponseToWorkData(response))
                    is StoreWriteResponse.Error.Exception -> {
                        val e = response.error
                        if (e.isRetryable()) WorkResult.retry(e.message ?: "Store5 mutation retryable error")
                        else WorkResult.failure(e.message ?: "Store5 mutation fatal error")
                    }
                    is StoreWriteResponse.Error.Message -> WorkResult.failure(response.message)
                }
            },
            onFailure = { e ->
                if (e.isRetryable()) WorkResult.retry(e.message ?: "Store5 mutation retryable error")
                else WorkResult.failure(e.message ?: "Store5 mutation fatal error")
            },
        )
    }

    /**
     * Map the [MutableStore.write] success response to worker output data.
     * Default returns empty [WorkData] — consumers can rely on Store5's Updater/Bookkeeper
     * side-effects as the sole output.
     *
     * @param response one of [StoreWriteResponse.Success.Typed] or [StoreWriteResponse.Success.Untyped].
     */
    protected open fun mapWriteResponseToWorkData(response: StoreWriteResponse.Success): WorkData = workDataOf()

    /**
     * Retry heuristic — override for domain-specific logic. Default returns `false`
     * (every error is fatal). Mirrors [StoreBackedWorker.isRetryable].
     */
    protected open fun Throwable.isRetryable(): Boolean = false
}
