@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlin.test.Test
import kotlin.test.assertNotNull

class IosWorkerFactoryTest {

    private class StubWorker(ctx: WorkerContext) : CoroutineWorker(ctx) {
        override suspend fun doWork() = WorkResult.success()
    }

    private val factory = object : IosWorkerFactory {
        override fun create(workerClass: String, context: WorkerContext): CoroutineWorker = StubWorker(context)
    }

    @Test
    fun create_returnsWorkerInstance() {
        val ctx = IosWorkerContext(
            id = kotlin.uuid.Uuid.random(),
            inputData = io.github.mobilebytelabs.worker.WorkData.EMPTY,
            tags = emptySet(),
            stateStore = IosWorkStateStore(persistence = InMemoryIosWorkPersistence()),
        )
        val worker = factory.create("StubWorker", ctx)
        assertNotNull(worker)
    }
}
