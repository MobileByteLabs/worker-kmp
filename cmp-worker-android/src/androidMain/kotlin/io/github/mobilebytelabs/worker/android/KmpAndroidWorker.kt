package io.github.mobilebytelabs.worker.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mobilebytelabs.worker.ExperimentalForegroundApi
import io.github.mobilebytelabs.worker.WorkResult
import kotlin.uuid.Uuid

/**
 * Android WorkManager worker that bridges to a KMP [io.github.mobilebytelabs.worker.CoroutineWorker].
 *
 * Reads the KMP worker class name from input data, instantiates it via the registered
 * [KmpWorkerFactory], and delegates [doWork] to it.
 *
 * Registers itself with [AndroidForegroundBridge] for the duration of the user's
 * `doWork()` so that KMP `ForegroundWorker.setForeground(info)` calls are routed
 * to `androidx.work.CoroutineWorker.setForegroundAsync(...)` instead of the
 * SystemTray-based Desktop actual.
 */
@OptIn(ExperimentalForegroundApi::class)
class KmpAndroidWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val workerClass = inputData.getString(KEY_KMP_CLASS)
            ?: return Result.failure()
        val kmpId = inputData.getString(KEY_KMP_ID)?.let {
            runCatching { Uuid.parse(it) }.getOrNull()
        } ?: return Result.failure()

        val factory = KmpWorkerFactory.instance
            ?: return Result.failure()

        val userInputData = inputData.toKmp()
        val userTags = tags.userTags()
        val context = AndroidWorkerContext(this, kmpId, userInputData, userTags)
        val worker = factory.create(workerClass, context)

        AndroidForegroundBridge.register(kmpId, this)
        return try {
            when (val result = worker.doWork()) {
                is WorkResult.Success -> Result.success(result.outputData.toAndroid())

                is WorkResult.Failure -> Result.failure(
                    androidx.work.workDataOf("error" to result.message),
                )

                is WorkResult.Retry -> Result.retry()
            }
        } finally {
            AndroidForegroundBridge.unregister(kmpId)
        }
    }
}
