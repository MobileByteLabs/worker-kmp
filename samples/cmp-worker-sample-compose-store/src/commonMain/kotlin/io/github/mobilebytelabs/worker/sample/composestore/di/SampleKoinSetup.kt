package io.github.mobilebytelabs.worker.sample.composestore.di

import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.sample.composestore.store.buildArticlesStore
import org.koin.core.module.Module

/**
 * Returns the two Koin modules every platform launcher needs.
 *
 * Constructs the shared Store5 store inline so per-platform code never builds it
 * — keeps the launcher files at ≤10 source lines each. The only difference per
 * platform is the [WorkManagerFactory] (android / desktop / ios / web).
 *
 * Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-cmp-launchers/GOAL.md`
 * §File Structure (Sample — `SampleKoinSetup.kt`).
 */
fun sampleKoinModules(factory: WorkManagerFactory): List<Module> {
    val store = buildArticlesStore()
    return listOf(
        appModule(store),
        articleWorkerKoinModule(store = store, factory = factory),
    )
}
