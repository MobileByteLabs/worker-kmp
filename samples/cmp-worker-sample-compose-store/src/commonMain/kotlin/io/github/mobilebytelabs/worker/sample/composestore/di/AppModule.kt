package io.github.mobilebytelabs.worker.sample.composestore.di

import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.koin.workKoinModule
import io.github.mobilebytelabs.worker.registry.WorkerRegistry
import io.github.mobilebytelabs.worker.registry.workerRegistry
import io.github.mobilebytelabs.worker.sample.composestore.domain.Article
import io.github.mobilebytelabs.worker.sample.composestore.workers.ArticleSyncWorker
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mobilenativefoundation.store.store5.Store

/**
 * Domain module — exposes the shared [Store] to the UI graph.
 *
 * Built outside Koin (platform `Main.kt`) so the same instance can be captured by both
 * the UI graph (this module) and the worker registry below — Store5's contract is that
 * a worker's key is fixed at construction, so the registry's factory closure must hold
 * a direct reference, not a lazy lookup.
 */
fun appModule(store: Store<String, Article>): Module = module {
    single<Store<String, Article>> { store }
}

/**
 * Worker module — wires the WorkManager backend (via [workKoinModule]) and registers
 * [ArticleSyncWorker] against the shared [store]. The article id is per-request
 * (read from `inputData`); the Store is shared across all firings.
 */
fun articleWorkerKoinModule(
    store: Store<String, Article>,
    factory: WorkManagerFactory,
): Module = workKoinModule(
    config = WorkerConfig(),
    workers = articleWorkerRegistry(store),
    factory = factory,
)

private fun articleWorkerRegistry(store: Store<String, Article>): WorkerRegistry = workerRegistry {
    register<ArticleSyncWorker> { ctx ->
        val articleId = ctx.inputData.getString("articleId") ?: "1"
        ArticleSyncWorker(ctx, store, articleId)
    }
}
