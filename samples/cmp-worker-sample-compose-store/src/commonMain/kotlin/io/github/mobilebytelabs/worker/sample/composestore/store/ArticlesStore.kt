package io.github.mobilebytelabs.worker.sample.composestore.store

import io.github.mobilebytelabs.worker.sample.composestore.domain.Article
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import kotlin.time.Duration.Companion.minutes

/**
 * Builds a Store5 [Store] keyed by article id with a fake network fetcher.
 *
 * The fetcher simulates a ~400ms remote call and occasionally fails (every 4th call)
 * to demonstrate the [io.github.mobilebytelabs.worker.store5.StoreBackedWorker] retry
 * path under the worker scheduler.
 *
 * Memory policy: holds up to 32 entries, each fresh for 5 minutes. After expiry the
 * Store re-fetches from the network — driven by [StoreBackedWorker] firings.
 */
fun buildArticlesStore(): Store<String, Article> {
    val fetcher: Fetcher<String, Article> = Fetcher.ofFlow(flowFactory = ::fetchArticleFromFakeNetwork)
    val memoryPolicy: MemoryPolicy<String, Article> = MemoryPolicy
        .builder<String, Article>()
        .setMaxSize(32)
        .setExpireAfterWrite(5.minutes)
        .build()
    return StoreBuilder
        .from(fetcher)
        .cachePolicy(memoryPolicy)
        .build()
}

private fun fetchArticleFromFakeNetwork(id: String): Flow<Article> = flow {
    delay(400)
    val attempt = totalFetches.incrementAndGet()
    if (attempt % 4 == 0) {
        error("Simulated transient network error for article=$id (attempt #$attempt)")
    }
    emit(
        Article(
            id = id,
            title = "Article #$id — refreshed (fetch #$attempt)",
            body = "Fetched via the fake network fetcher; this is fetch attempt #$attempt for id=$id.",
            fetchedAtEpochMs = attempt.toLong(),
        ),
    )
}

private val totalFetches = MonotonicCounter()

private class MonotonicCounter {
    private var count = 0
    fun incrementAndGet(): Int {
        count += 1
        return count
    }
}
