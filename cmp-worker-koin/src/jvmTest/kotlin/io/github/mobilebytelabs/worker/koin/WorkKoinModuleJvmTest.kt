package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.PlatformWorkManager
import io.github.mobilebytelabs.worker.WorkManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * JVM tests that exercise [workKoinModule] with a real [PlatformWorkManager] configured.
 * These tests live in jvmTest because [PlatformWorkManager.configure] is only available
 * on platform actuals, not in the expect declaration.
 */
class WorkKoinModuleJvmTest {
    private val fakeWm = JvmFakeWorkManager()

    @BeforeTest
    fun setUp() {
        PlatformWorkManager.configure(fakeWm)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        PlatformWorkManager.configure(null)
    }

    @Test
    fun workKoinModule_resolvesWorkManagerFromPlatform() {
        val koin = startKoin { modules(workKoinModule) }.koin
        assertNotNull(koin.get<WorkManager>())
    }

    @Test
    fun workKoinModule_returnsSameInstanceAsPlatformWorkManager() {
        val koin = startKoin { modules(workKoinModule) }.koin
        assertSame(fakeWm, koin.get<WorkManager>())
    }

    @Test
    fun workKoinModule_workManagerIsSingleton() {
        val koin = startKoin { modules(workKoinModule) }.koin
        assertSame(koin.get<WorkManager>(), koin.get<WorkManager>())
    }
}
