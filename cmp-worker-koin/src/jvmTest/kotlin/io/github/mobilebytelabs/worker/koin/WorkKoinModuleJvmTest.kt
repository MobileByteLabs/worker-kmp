@file:OptIn(WorkerKmpInternalApi::class)

package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * JVM tests that exercise [workKoinModule] with a real [WorkManagerFactory] supplied.
 * Refactored in v3.0.0-alpha00.X (Phase 0 deep refactor) — these tests previously
 * relied on `PlatformWorkManager.configure(fake)` which was removed alongside the
 * legacy global slot pattern. The new factory parameter makes platform wiring explicit
 * and testable from commonTest as well.
 */
class WorkKoinModuleJvmTest {
    private val fakeWm = JvmFakeWorkManager()
    private val factory = WorkManagerFactory { _, _ -> fakeWm }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun workKoinModule_resolvesWorkManagerFromFactory() {
        val koin = startKoin { modules(workKoinModulePrivateApi(factory = factory)) }.koin
        assertNotNull(koin.get<WorkManager>())
    }

    @Test
    fun workKoinModule_returnsFactoryProducedInstance() {
        val koin = startKoin { modules(workKoinModulePrivateApi(factory = factory)) }.koin
        assertSame(fakeWm, koin.get<WorkManager>())
    }

    @Test
    fun workKoinModule_workManagerIsSingleton() {
        val koin = startKoin { modules(workKoinModulePrivateApi(factory = factory)) }.koin
        assertSame(koin.get<WorkManager>(), koin.get<WorkManager>())
    }
}
