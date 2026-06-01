package io.github.mobilebytelabs.worker

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the commonMain [WorkManager.enqueueUniqueWork] entry point. The Android
 * actual delegates to `androidx.work.WorkManager.enqueueUniqueWork(...)` (verified by
 * the Android-side test); commonTest exercises the default impl behaviour on platforms
 * that don't override (iOS / Desktop / Web today fall through to plain `enqueue`).
 *
 * Added by cross-platform-worker-parity-audit sub-plan 03 (closes G2).
 */
class EnqueueUniqueWorkTest {

    @Test
    fun defaultImpl_routesToEnqueue_returningSameUuid() = runTest {
        // Call via WorkManager interface reference so the JVM compiler emits the
        // `WorkManager$DefaultImpls.enqueueUniqueWork(...)` call site — that's the
        // bytecode kover instruments. Calling on the concrete TestWorkManager would
        // bypass the default-impl bridge.
        val wm: WorkManager = TestWorkManager()
        val request = oneTimeWorkRequest<NoopWorker> {}
        val id = wm.enqueueUniqueWork(
            uniqueWorkName = "sync",
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = request,
        )
        assertEquals(request.id, id)
    }

    @Test
    fun defaultImpl_acceptsAllPolicies() = runTest {
        val wm: WorkManager = TestWorkManager()
        for (policy in ExistingWorkPolicy.entries) {
            val request = oneTimeWorkRequest<NoopWorker> {}
            val id = wm.enqueueUniqueWork(
                uniqueWorkName = "sync-$policy",
                existingWorkPolicy = policy,
                request = request,
            )
            assertEquals(request.id, id)
        }
    }

    @Test
    fun existingWorkPolicy_allFourValues_areConstructable() {
        // Smoke-test the enum surface: all 4 values present + distinct.
        val all = listOf(
            ExistingWorkPolicy.REPLACE,
            ExistingWorkPolicy.KEEP,
            ExistingWorkPolicy.APPEND,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
        )
        assertEquals(4, all.toSet().size)
    }
}

private class NoopWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.success()
}
