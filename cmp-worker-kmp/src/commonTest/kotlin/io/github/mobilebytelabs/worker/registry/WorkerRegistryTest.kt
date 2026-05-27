package io.github.mobilebytelabs.worker.registry

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.TestWorkerContext
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult = WorkResult.Success()
}

class WorkerRegistryTest {

    @Test
    fun workerRegistry_dslBuildsRegistry() {
        val registry = workerRegistry {
            register<FakeWorker> { ctx -> FakeWorker(ctx) }
        }
        assertTrue(FakeWorker::class.simpleName!! in registry.registeredClassNames())
    }

    @Test
    fun register_typedFactoryStoredBySimpleName() {
        val registry = workerRegistry {
            register<FakeWorker> { ctx -> FakeWorker(ctx) }
        }
        val created = registry.create(FakeWorker::class.simpleName!!, TestWorkerContext())
        assertNotNull(created)
        assertTrue(created is FakeWorker)
    }

    @Test
    fun create_unknownClassNameReturnsNull() {
        val registry = workerRegistry { }
        val created = registry.create("com.example.Unknown", TestWorkerContext())
        assertNull(created)
    }

    @Test
    fun register_afterLockThrowsAlreadyLoaded() {
        val registry = workerRegistry {
            register<FakeWorker> { ctx -> FakeWorker(ctx) }
        }
        registry.lock()
        val ex = assertFailsWith<WorkerRegistryAlreadyLoadedException> {
            registry.register("com.example.Late") { ctx -> FakeWorker(ctx) }
        }
        assertEquals("com.example.Late", ex.attemptedRegistration)
    }

    @Test
    fun register_classNameWithPathTraversalRejected() {
        val registry = workerRegistry { }
        assertFailsWith<IllegalArgumentException> {
            registry.register("../com.example.Bad") { ctx -> FakeWorker(ctx) }
        }
    }

    @Test
    fun register_classNameWithSlashRejected() {
        val registry = workerRegistry { }
        assertFailsWith<IllegalArgumentException> {
            registry.register("com/example/Bad") { ctx -> FakeWorker(ctx) }
        }
    }

    @Test
    fun register_classNameWithSpaceRejected() {
        val registry = workerRegistry { }
        assertFailsWith<IllegalArgumentException> {
            registry.register("com.example Bad") { ctx -> FakeWorker(ctx) }
        }
    }
}
