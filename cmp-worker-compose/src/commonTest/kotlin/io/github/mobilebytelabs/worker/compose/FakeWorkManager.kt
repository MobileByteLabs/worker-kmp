package io.github.mobilebytelabs.worker.compose

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

class FakeWorkManager : WorkManager {

    private val _store = MutableStateFlow<Map<String, List<WorkInfo>>>(emptyMap())

    val enqueuedRequests = mutableListOf<OneTimeWorkRequest>()
    val periodicRequests = mutableListOf<Pair<String, PeriodicWorkRequest>>()
    val cancelledIds = mutableListOf<Uuid>()
    val cancelledTags = mutableListOf<String>()

    fun putInfos(tag: String, vararg infos: WorkInfo) {
        _store.value = _store.value + (tag to infos.toList())
    }

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        enqueuedRequests += request
        return request.id
    }

    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid {
        periodicRequests += uniqueWorkName to request
        return request.id
    }

    override suspend fun cancelWorkById(id: Uuid) {
        cancelledIds += id
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        cancelledTags += tag
    }

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = _store.map { it[tag] ?: emptyList() }

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? =
        _store.value.values.flatten().firstOrNull { it.id == id }
}
