package io.github.mobilebytelabs.worker.desktop

import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for [startWorkerKoinIfAbsent].
 *
 * The full [launchDesktopWorkerApp] cannot be unit-tested headless (it blocks on the
 * Compose Desktop UI thread); window rendering is covered by manual smoke-test in the
 * `cmp-worker-sample-compose-store` sample (Phase 3 T18). Here we test the
 * idempotent-Koin-start side-effect — the half that the sample's per-platform launcher
 * relies on for safe multi-call behavior in test fixtures and reload scenarios.
 *
 * Spec: GOAL.md AC5 + AC10.
 */
class LaunchDesktopWorkerAppTest {

    @AfterTest
    fun teardown() {
        if (GlobalContext.getOrNull() != null) {
            GlobalContext.stopKoin()
        }
    }

    @Test
    fun `startWorkerKoinIfAbsent installs the supplied modules on first call`() {
        startWorkerKoinIfAbsent { listOf(module { single { 42 } }) }

        val koin = GlobalContext.get()
        assertNotNull(koin, "Koin should be initialized after first call")
        assertEquals(42, koin.get<Int>())
    }

    @Test
    fun `startWorkerKoinIfAbsent is a no-op when Koin already running`() {
        startWorkerKoinIfAbsent { listOf(module { single { 42 } }) }
        // Second call would normally throw `KoinAppAlreadyStartedException`; the helper
        // must short-circuit instead.
        startWorkerKoinIfAbsent { listOf(module { single { 99 } }) }

        assertEquals(
            42,
            GlobalContext.get().get<Int>(),
            "Second call must not overwrite the first context",
        )
    }
}
