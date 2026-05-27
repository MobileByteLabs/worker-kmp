package io.github.mobilebytelabs.worker.storeflow.paging

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf

/**
 * Generic worker that prefetches N pages ahead of the cursor for a paginated data
 * source. Consumer subclasses implement [fetchPage] (which persists each page into
 * the consumer's cache) and supply the cursor via `WorkRequest.setInputData(...)`.
 *
 * Added in v3.0.0-alpha03.X (Phase 3 extension).
 *
 * Adapted from `kmp-project-template/core-base/store/paging/StorePagingSource.kt` —
 * worker-anchored variant: prefetch happens off the UI thread via worker-kmp's
 * platform-native scheduler.
 *
 * Input data keys:
 * - [KEY_START_PAGE] (Int, required) — the first page number to fetch.
 * - [KEY_PAGE_COUNT] (Int, optional; default `3`) — how many pages ahead to fetch.
 *
 * Output data:
 * - [KEY_PAGES_FETCHED] (Int) — count of pages successfully fetched before terminal state.
 *
 * Result semantics:
 * - All pages fetched cleanly → [WorkResult.success] + `KEY_PAGES_FETCHED` count.
 * - Any page fetch throws → [WorkResult.retry] (scheduler retries per [io.github.mobilebytelabs.worker.RetryConfig]).
 *
 * Example consumer:
 * ```kotlin
 * class FeedPrefetchWorker(
 *     context: WorkerContext,
 *     private val feedRepo: FeedRepository,
 * ) : PrefetchPagingWorker(context) {
 *     override suspend fun fetchPage(pageNumber: Int) {
 *         feedRepo.loadAndCache(pageNumber)
 *     }
 * }
 *
 * // Schedule:
 * workManager.enqueue(OneTimeWorkRequestBuilder<FeedPrefetchWorker>().apply {
 *     setInputData(workDataOf(
 *         PrefetchPagingWorker.KEY_START_PAGE to currentPage + 1,
 *         PrefetchPagingWorker.KEY_PAGE_COUNT to 5,
 *     ))
 * }.build())
 * ```
 */
public abstract class PrefetchPagingWorker(context: WorkerContext) : CoroutineWorker(context) {

    override suspend fun doWork(): WorkResult {
        val startPage = inputData.getInt(KEY_START_PAGE)
        val pageCount = inputData.getInt(KEY_PAGE_COUNT, default = DEFAULT_PAGE_COUNT)
            .takeIf { it > 0 } ?: DEFAULT_PAGE_COUNT
        var fetched = 0
        for (page in startPage until (startPage + pageCount)) {
            runCatching { fetchPage(page) }
                .onSuccess { fetched++ }
                .onFailure { e ->
                    return WorkResult.retry(e.message ?: "Page $page fetch failed")
                }
        }
        return WorkResult.success(workDataOf(KEY_PAGES_FETCHED to fetched))
    }

    /**
     * Fetch one page and persist it into the consumer's cache. Throw on transient failure
     * — [doWork] maps any throw to [WorkResult.retry].
     */
    protected abstract suspend fun fetchPage(pageNumber: Int)

    public companion object {
        /** Input data key: starting page number (Int, required). */
        public const val KEY_START_PAGE: String = "prefetch.startPage"

        /** Input data key: number of pages to fetch ahead (Int, optional; default 3). */
        public const val KEY_PAGE_COUNT: String = "prefetch.pageCount"

        /** Output data key: count of pages successfully fetched (Int). */
        public const val KEY_PAGES_FETCHED: String = "prefetch.pagesFetched"

        /** Default page count when [KEY_PAGE_COUNT] is unset / non-positive. */
        public const val DEFAULT_PAGE_COUNT: Int = 3
    }
}
