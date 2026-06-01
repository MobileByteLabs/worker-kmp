package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkManagerFactory
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.WorkerRegistry
import io.github.mobilebytelabs.worker.registry.workerRegistry
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
    fun workKoinModule_resolvesWorkManagerFromFactory() {
        val stub = StubWorkManager()
        val factory = WorkManagerFactory { _, _ -> stub }
        val koin = startKoin {
            modules(workKoinModule(factory = factory))
        }.koin
        assertNotNull(koin.get<WorkManager>())
        assertSame(stub, koin.get<WorkManager>())
    }

    @Test
    fun workKoinModule_workManagerIsSingleton() {
        val stub = StubWorkManager()
        val factory = WorkManagerFactory { _, _ -> stub }
        val koin = startKoin {
            modules(workKoinModule(factory = factory))
        }.koin
        assertSame(koin.get<WorkManager>(), koin.get<WorkManager>())
    }

    @Test
    fun workKoinModule_invokesFactoryWithProvidedConfigAndRegistry() {
        val stub = StubWorkManager()
        var receivedConfig: WorkerConfig? = null
        var receivedWorkers: WorkerRegistry? = null
        val factory = WorkManagerFactory { config, workers ->
            receivedConfig = config
            receivedWorkers = workers
            stub
        }
        val cfg = WorkerConfig()
        val workers = workerRegistry { }
        val koin = startKoin {
            modules(workKoinModule(config = cfg, workers = workers, factory = factory))
        }.koin
        // Resolution triggers factory.create — assert it received our inputs.
        koin.get<WorkManager>()
        assertSame(cfg, receivedConfig)
        assertSame(workers, receivedWorkers)
    }

    @Test
    fun workKoinModule_exposesConfigAndRegistryAsSingles() {
        val stub = StubWorkManager()
        val factory = WorkManagerFactory { _, _ -> stub }
        val cfg = WorkerConfig()
        val workers = workerRegistry { }
        val koin = startKoin {
            modules(workKoinModule(config = cfg, workers = workers, factory = factory))
        }.koin
        assertSame(cfg, koin.get<WorkerConfig>())
        assertSame(workers, koin.get<WorkerRegistry>())
    }
}

internal class StubWorkManager : WorkManager {
    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid = Uuid.random()
    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid = Uuid.random()
    override suspend fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: io.github.mobilebytelabs.worker.ExistingWorkPolicy,
        request: OneTimeWorkRequest,
    ): Uuid = Uuid.random()
    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = flowOf(emptyList())
    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = null
    override suspend fun cancelWorkById(id: Uuid) = Unit
    override suspend fun cancelAllWorkByTag(tag: String) = Unit
}
