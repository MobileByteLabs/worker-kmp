package io.github.mobilebytelabs.worker.koin

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.uuid.Uuid

internal class JvmFakeWorkManager : WorkManager {
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
