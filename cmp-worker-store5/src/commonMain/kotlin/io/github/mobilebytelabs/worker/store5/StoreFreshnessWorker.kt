package io.github.mobilebytelabs.worker.store5

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.Validator

/**
 * [CoroutineWorker] that checks a [Validator] before fetching — if the cached data is
 * still fresh per the validator, the worker no-ops with [WorkResult.success] (saves
 * bandwidth + network round-trip). If stale or missing, delegates to the same
 * `store.stream(StoreReadRequest.fresh(key))` path as [StoreBackedWorker].
 *
 * Added in v3.0.0-alpha02.X (Phase 2 extension).
 *
 * Example consumer:
 * ```kotlin
 * class UserProfileFreshnessWorker(
 *     context: WorkerContext,
 *     store: Store<UserId, UserProfile>,
 *     userId: UserId,
 *     validator: Validator<UserProfile> = Validator.by { it.fetchedAt > now - 6.hours },
 * ) : StoreFreshnessWorker<UserId, UserProfile>(context, store, userId, validator)
 * ```
 *
 * Output [WorkData] semantics:
 * - On skip (cache still fresh): `workDataOf("skipped" to "fresh")` — surface via
 *   `WorkInfo.outputData.getString("skipped")` to detect bandwidth-saved invocations.
 * - On successful fresh fetch: empty WorkData (consumers rely on the Store cache write).
 *
 * @param K Store key type.
 * @param Output Store output type.
 */
public abstract class StoreFreshnessWorker<K : Any, Output : Any>(
    context: WorkerContext,
    private val store: Store<K, Output>,
    private val key: K,
    private val validator: Validator<Output>,
) : CoroutineWorker(context) {

    override suspend fun doWork(): WorkResult {
        return runCatching {
            // Read cached value (cache-only, no network).
            val cached = readCached()
            if (cached != null && validator.isValid(cached)) {
                // Still fresh — skip network fetch.
                return WorkResult.success(workDataOf(KEY_SKIPPED to VALUE_FRESH))
            }
            // Stale or missing — force fresh.
            val terminal: StoreReadResponse<Output> = store
                .stream(StoreReadRequest.fresh(key))
                .first { response -> response is StoreReadResponse.Data || response is StoreReadResponse.Error }
            terminal.requireData()
        }.fold(
            onSuccess = { WorkResult.success(workDataOf()) },
            onFailure = { e ->
                if (e.isRetryable()) WorkResult.retry(e.message ?: "Store5 retryable error")
                else WorkResult.failure(e.message ?: "Store5 fatal error")
            },
        )
    }

    /**
     * Read the currently-cached value without triggering a network fetch.
     * Uses [StoreReadRequest.cached] with `refresh = false`.
     *
     * Returns null on any error / no-data response — caller treats null as "stale".
     */
    private suspend fun readCached(): Output? = runCatching {
        var data: Output? = null
        store.stream(StoreReadRequest.cached(key, refresh = false)).first { response ->
            when (response) {
                is StoreReadResponse.Data -> {
                    data = response.value
                    true
                }
                is StoreReadResponse.NoNewData -> true
                is StoreReadResponse.Error -> true
                else -> false
            }
        }
        data
    }.getOrNull()

    /**
     * Retry heuristic — override for domain-specific logic. Default returns `false`.
     * Mirrors [StoreBackedWorker.isRetryable].
     */
    protected open fun Throwable.isRetryable(): Boolean = false

    public companion object {
        /** WorkData key surfaced when the freshness check skipped the network fetch. */
        public const val KEY_SKIPPED: String = "skipped"

        /** WorkData value for [KEY_SKIPPED] when the cached value passed the Validator. */
        public const val VALUE_FRESH: String = "fresh"
    }
}
