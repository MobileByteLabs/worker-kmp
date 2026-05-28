package io.github.mobilebytelabs.worker.ios

import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * iOS-target tests for [workerKmpMainViewController].
 *
 * Runs against iosArm64 / iosSimulatorArm64 via Kotlin/Native test runner. Covers:
 * - UIViewController is non-null after launch.
 * - Koin starts on first call with the supplied modules.
 * - Subsequent calls do NOT throw `KoinAppAlreadyStartedException` (idempotent guard).
 *
 * The `content` lambda is an empty `{}` no-op — we only need to verify the launcher
 * mechanics here, not Compose rendering. The Compose UI integration is covered
 * end-to-end by the `cmp-worker-sample-compose-store` iOS app (xcodebuild gate).
 *
 * Spec: GOAL.md AC6 + AC10.
 */
class WorkerKmpMainViewControllerTest {

    @AfterTest
    fun teardown() {
        if (KoinPlatformTools.defaultContext().getOrNull() != null) {
            org.koin.core.context.stopKoin()
        }
    }

    @Test
    fun returns_non_null_UIViewController() {
        val vc = workerKmpMainViewController(
            koinModules = { emptyList() },
            content = {},
        )
        assertNotNull(vc)
    }

    @Test
    fun starts_koin_with_supplied_modules_on_first_call() {
        workerKmpMainViewController(
            koinModules = { listOf(module { single { "iosFlag" } }) },
            content = {},
        )
        assertEquals("iosFlag", KoinPlatformTools.defaultContext().get().get<String>())
    }

    @Test
    fun second_call_does_not_restart_koin() {
        workerKmpMainViewController(
            koinModules = { listOf(module { single { "first" } }) },
            content = {},
        )
        // Second call would throw KoinAppAlreadyStartedException without the idempotent guard.
        workerKmpMainViewController(
            koinModules = { listOf(module { single { "second" } }) },
            content = {},
        )
        assertEquals(
            "first",
            KoinPlatformTools.defaultContext().get().get<String>(),
            "Second call must not overwrite the first context",
        )
    }
}
