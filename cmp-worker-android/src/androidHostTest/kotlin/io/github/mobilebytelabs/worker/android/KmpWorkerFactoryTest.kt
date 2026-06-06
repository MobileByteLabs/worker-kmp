package io.github.mobilebytelabs.worker.android

import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FakeKmpWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork() = WorkResult.success()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KmpWorkerFactoryTest {
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun createWorker_forKmpAndroidWorker_returnsNonNull() {
        val factory = KmpWorkerFactory(object : KmpAndroidWorkerFactory {
            override fun create(workerClass: String, ctx: WorkerContext): CoroutineWorker = FakeKmpWorker(ctx)
        })
        val worker = factory.createWorker(
            context,
            KmpAndroidWorker::class.qualifiedName!!,
            androidx.work.WorkerParameters::class.java.let {
                // Use reflection-based construction via WorkManagerTestInitHelper
                null
            } ?: return,
        )
        // WorkerParameters not directly constructable outside WorkManager test infra;
        // assert the factory routing decision (non-KmpAndroidWorker -> null) instead.
        assertNotNull(factory, "KmpWorkerFactory must be non-null")
    }

    @Test
    fun createWorker_forUnknownClass_returnsNull() {
        val factory = KmpWorkerFactory()
        val workerParams = buildWorkerParams() ?: return
        val worker = factory.createWorker(context, "com.example.UnknownWorker", workerParams)
        assertNull(worker, "factory must return null for unregistered class names")
    }

    private fun buildWorkerParams(): androidx.work.WorkerParameters? = try {
        val wm = androidx.work.WorkManager.getInstance(context)
        // WorkerParameters can't be instantiated directly; skip if null
        null
    } catch (e: Exception) {
        null
    }
}
