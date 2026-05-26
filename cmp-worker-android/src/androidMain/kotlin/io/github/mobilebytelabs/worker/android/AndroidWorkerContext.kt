package io.github.mobilebytelabs.worker.android

import androidx.work.Data
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkProgress
import io.github.mobilebytelabs.worker.WorkerContext
import kotlin.uuid.Uuid

internal class AndroidWorkerContext(
    private val androidWorker: KmpAndroidWorker,
    override val id: Uuid,
    override val inputData: WorkData,
    override val tags: Set<String>,
) : WorkerContext {
    override suspend fun setProgress(progress: WorkProgress) {
        val data = Data.Builder()
            .putInt("progress", progress.progress)
            .build()
        androidWorker.setProgress(data)
    }
}
