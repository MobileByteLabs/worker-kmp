package io.github.mobilebytelabs.worker.sample.composestore.workers

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.sample.composestore.domain.Article
import io.github.mobilebytelabs.worker.store5.StoreBackedWorker
import io.github.mobilebytelabs.worker.workDataOf
import org.mobilenativefoundation.store.store5.Store

/**
 * Worker that refreshes a single [Article] through a Store5 [Store].
 *
 * `StoreBackedWorker` handles the heavy lifting:
 *  - forces a fresh fetch (bypasses cache, invokes the fetcher)
 *  - writes the result back to the Store cache so observers see it
 *  - maps the first terminal `Data` response to `WorkResult.success`
 *  - maps any error to `WorkResult.retry` (when [isRetryable] returns true) or
 *    `WorkResult.failure` otherwise.
 *
 * The article id is supplied via WorkData (`articleId` key) — see [AppModule.workers].
 */
class ArticleSyncWorker(
    context: WorkerContext,
    store: Store<String, Article>,
    articleId: String,
) : StoreBackedWorker<String, Article>(context, store, articleId) {

    override fun mapOutputToWorkData(output: Article): WorkData = workDataOf(
        "articleId" to output.id,
        "title" to output.title,
        "fetchAttempt" to output.fetchedAtEpochMs.toString(),
    )

    override fun Throwable.isRetryable(): Boolean = message?.startsWith("Simulated transient") == true
}
