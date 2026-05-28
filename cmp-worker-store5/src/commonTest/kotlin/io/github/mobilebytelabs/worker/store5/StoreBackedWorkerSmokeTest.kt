package io.github.mobilebytelabs.worker.store5

import io.github.mobilebytelabs.worker.store5.koin.workStore5KoinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Smoke test — verifies the public types resolve + the dependency on Store5 + cmp-worker-kmp
 * is wired correctly. Full integration tests against a real Store ship in v3.0.0-alpha02.X
 * follow-ups (require mock Store5 fixtures).
 *
 * Added in v3.0.0-alpha02 (Phase 2 of worker-kmp v3.0.0 epic).
 */
class StoreBackedWorkerSmokeTest {

    @Test
    fun publicTypes_resolve_fromClasspath() {
        assertNotNull(StoreBackedWorker::class)
        assertNotNull(StoreRefreshScheduler::class)
    }

    @Test
    fun refreshTagPrefix_isStable() {
        // Tag shape is part of the public observability contract — consumers may rely on
        // `getWorkInfosByTag("store5-refresh:foo")` outside the scheduler API.
        assertEquals("store5-refresh:", StoreRefreshScheduler.TAG_PREFIX)
    }

    @Test
    fun workStore5KoinModule_isNonNull() {
        // The module declares `single { StoreRefreshScheduler(...) }`. Resolving the binding
        // requires a host koin container with `workKoinModule(...)` registered first; full
        // DI integration tests live in v3.0.0-alpha02.X follow-ups.
        assertNotNull(workStore5KoinModule)
    }
}
