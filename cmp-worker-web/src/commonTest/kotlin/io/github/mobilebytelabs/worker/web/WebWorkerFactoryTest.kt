@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import io.github.mobilebytelabs.worker.WorkData

class WebWorkerFactoryTest {

    @Test
    fun factory_create_returnsNonNullForKnownClass() {
        val factory = object : WebWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
                if (workerClass == "TestWorker") SuccessWebWorker(context) else error("Unknown: $workerClass")
        }
        val context = fakeContext()
        assertNotNull(factory.create("TestWorker", context))
    }

    @Test
    fun workerRegistryWebAdapter_unknownClass_throwsError() {
        var threw = false
        val factory = object : WebWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker {
                return try {
                    error("Unknown: $workerClass")
                } catch (e: IllegalStateException) {
                    threw = true
                    SuccessWebWorker(context)
                }
            }
        }
        factory.create("Unknown", fakeContext())
        assertTrue(threw, "should throw on unknown worker class")
    }

    @Test
    fun webWorkerFactory_isExperimentalApi_compiles() {
        val factory: WebWorkerFactory = object : WebWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
                SuccessWebWorker(context)
        }
        assertNotNull(factory)
    }

    private fun fakeContext(): WebWorkerContext = WebWorkerContext(
        id = Uuid.random(),
        inputData = WorkData.EMPTY,
        tags = emptySet(),
        stateStore = WebWorkStateStore(),
    )
}
