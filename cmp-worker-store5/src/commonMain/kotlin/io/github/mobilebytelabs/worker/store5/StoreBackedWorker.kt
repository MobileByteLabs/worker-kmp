package io.github.mobilebytelabs.worker.store5

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * [CoroutineWorker] that delegates to a [Store] from Mobile Native Foundation Store5.
 *
 * On [doWork], calls `store.stream(StoreReadRequest.fresh(key))` to force a fresh fetch
 * (bypassing cache) + cache write. The first non-`Loading`/non-`NoNewData` response is
 * surfaced: `Data` becomes [WorkResult.Success], errors are mapped to retry/failure based
 * on [isRetryable].
 *
 * Added in v3.0.0-alpha02 (Phase 2 of worker-kmp v3.0.0 epic).
 *
 * Example:
 * ```kotlin
 * class UserProfileSyncWorker(
 *     context: WorkerContext,
 *     userStore: Store<UserId, UserProfile>,
 * ) : StoreBackedWorker<UserId, UserProfile>(context, userStore, key = currentUserId()) {
 *     override fun mapOutputToWorkData(output: UserProfile) = workDataOf(
 *         "name" to output.name,
 *         "fetchedAt" to output.fetchedAt.toString(),
 *     )
 * }
 * ```
 *
 * @param K The store's key type.
 * @param Output The store's output (refreshed) type.
 * @param context Worker context propagated from the platform scheduler.
 * @param store The [Store] instance to drive.
 * @param key The key to fetch from the store.
 */
public abstract class StoreBackedWorker<K : Any, Output : Any>(
    context: WorkerContext,
    private val store: Store<K, Output>,
    private val key: K,
) : CoroutineWorker(context) {

    override suspend fun doWork(): WorkResult = runCatching {
        // Force fresh fetch — Store5 will bypass cache + invoke the fetcher + write back.
        // We collect the first terminal response (Data or any Error variant) and propagate it.
        // Loading / NoNewData / Initial are filtered out (transient signals).
        val terminal: StoreReadResponse<Output> = store
            .stream(StoreReadRequest.fresh(key))
            .first { response -> response is StoreReadResponse.Data || response is StoreReadResponse.Error }
        // requireData() returns Output on Data, and throws the underlying Throwable on
        // any Error variant (Exception / Message / Custom) — exactly the surface we want.
        terminal.requireData()
    }.fold(
        onSuccess = { output -> WorkResult.success(mapOutputToWorkData(output)) },
        onFailure = { e ->
            if (e.isRetryable()) {
                WorkResult.retry(e.message ?: "Store5 retryable error")
            } else {
                WorkResult.failure(e.message ?: "Store5 fatal error")
            }
        },
    )

    /**
     * Maps the freshly-loaded [Output] to [WorkData] for worker output.
     *
     * Override to project the type-safe output into the WorkData key-value space (which is
     * the only data shape WorkManager observers can consume across the WorkInfo API surface).
     * Default returns empty WorkData — consumers can rely on the [Store] cache update as the
     * sole side-effect.
     */
    protected open fun mapOutputToWorkData(output: Output): WorkData = workDataOf()

    /**
     * Should this exception result in [WorkResult.retry] rather than [WorkResult.failure]?
     *
     * Default returns `false` — every error is fatal. Override per-platform or per-worker to
     * recognise transient errors (e.g. JVM `java.io.IOException`, iOS `NSURLErrorTimedOut`,
     * JS `TypeError("Failed to fetch")`) and return `true` so the scheduler retries per the
     * [io.github.mobilebytelabs.worker.RetryConfig].
     *
     * The default is intentionally conservative — false retries are worse than false failures
     * for most use cases. `java.io.IOException` cannot be referenced from commonMain so we
     * leave the heuristic to consumers.
     */
    protected open fun Throwable.isRetryable(): Boolean = false
}
