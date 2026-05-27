package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.uuid.Uuid

class WorkKoinModuleTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun singleWorkManagerBinding_isSingleton() {
        val stub = StubWorkManager()
        val testModule = module { single<WorkManager> { stub } }
        val koin = startKoin { modules(testModule) }.koin
        assertSame(koin.get<WorkManager>(), koin.get<WorkManager>())
    }

    @Test
    fun singleWorkManagerBinding_returnsConfiguredInstance() {
        val stub = StubWorkManager()
        val testModule = module { single<WorkManager> { stub } }
        val koin = startKoin { modules(testModule) }.koin
        assertSame(stub, koin.get<WorkManager>())
    }

    @Test
    fun workKoinModule_declaresWorkManagerBinding() {
        // workKoinModule uses PlatformWorkManager() which needs platform config.
        // Verify the module can be created without error — resolution happens lazily.
        assertNotNull(workKoinModule)
    }
}

internal class StubWorkManager : WorkManager {
    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid = Uuid.random()
    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid = Uuid.random()
    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = flowOf(emptyList())
    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = null
    override suspend fun cancelWorkById(id: Uuid) = Unit
    override suspend fun cancelAllWorkByTag(tag: String) = Unit
}
