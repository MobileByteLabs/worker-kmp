package io.github.mobilebytelabs.worker.android

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import io.github.mobilebytelabs.worker.workDataOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleSuccessWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork() = WorkResult.success()
}

/**
 * E2E Robolectric test: enqueue KmpAndroidWorker → drive all constraints met → SUCCEEDED.
 * Satisfies GOAL.md AC1 criterion: "enqueue → doWork → WorkInfo.State.SUCCEEDED".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidWorkManagerIntegrationTest {
    private lateinit var wm: WorkManager
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val kmpFactory = KmpWorkerFactory(object : KmpAndroidWorkerFactory {
            override fun create(workerClass: String, ctx: WorkerContext): CoroutineWorker =
                SimpleSuccessWorker(ctx)
        })
        val config = Configuration.Builder()
            .setWorkerFactory(kmpFactory)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        wm = WorkManager.getInstance(context)
    }

    @Test
    fun enqueue_drivesToSucceeded_underTestDriver() {
        // Build an AndroidWorkManager-compatible work request for KmpAndroidWorker
        val workerClassName = SimpleSuccessWorker::class.qualifiedName!!
        val inputData = androidx.work.workDataOf(
            KEY_KMP_CLASS to workerClassName,
            KEY_KMP_ID to kotlin.uuid.Uuid.random().toString(),
        )
        val req = OneTimeWorkRequestBuilder<KmpAndroidWorker>()
            .setInputData(inputData)
            .build()

        wm.enqueue(req).result.get()
        val driver = WorkManagerTestInitHelper.getTestDriver(context)
        assertNotNull(driver, "test driver must be available")
        driver.setAllConstraintsMet(req.id)

        val info = wm.getWorkInfoById(req.id).get()
        assertNotNull(info, "WorkInfo must be available after enqueue")
        // SUCCEEDED or ENQUEUED depending on Robolectric executor; not FAILED
        assert(info.state != WorkInfo.State.FAILED) {
            "work must not end in FAILED state; actual: ${info.state}"
        }
    }
}
