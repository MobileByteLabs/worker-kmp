package io.github.mobilebytelabs.worker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

class TestWorkManager : WorkManager {
    private val stateStore = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    val enqueuedRequests: List<WorkRequest> get() = _enqueuedRequests.toList()
    val cancelledIds: Set<Uuid> get() = _cancelledIds.toSet()

    private val _enqueuedRequests = mutableListOf<WorkRequest>()
    private val _cancelledIds = mutableSetOf<Uuid>()

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        _enqueuedRequests.add(request)
        updateState(request.id, WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags))
        return request.id
    }

    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid {
        _enqueuedRequests.add(request)
        updateState(request.id, WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags))
        return request.id
    }

    override suspend fun cancelWorkById(id: Uuid) {
        _cancelledIds.add(id)
        stateStore.value[id]?.let { updateState(id, it.copy(state = WorkInfo.State.CANCELLED)) }
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        stateStore.value.values.filter { tag in it.tags }.forEach { cancelWorkById(it.id) }
    }

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> =
        stateStore.map { infos -> infos.values.filter { tag in it.tags } }

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = stateStore.value[id]

    fun simulateSuccess(id: Uuid, outputData: WorkData = WorkData.EMPTY) {
        stateStore.value[id]?.let {
            updateState(id, it.copy(state = WorkInfo.State.SUCCEEDED, outputData = outputData))
        }
    }

    fun simulateFailure(id: Uuid, message: String = "") {
        stateStore.value[id]?.let { updateState(id, it.copy(state = WorkInfo.State.FAILED)) }
    }

    fun simulateProgress(id: Uuid, progress: WorkProgress) {
        stateStore.value[id]?.let { updateState(id, it.copy(progress = progress)) }
    }

    fun simulateStateChange(id: Uuid, workState: WorkInfo.State) {
        stateStore.value[id]?.let { updateState(id, it.copy(state = workState)) }
    }

    fun reset() {
        _enqueuedRequests.clear()
        _cancelledIds.clear()
        stateStore.value = emptyMap()
    }

    private fun updateState(id: Uuid, info: WorkInfo) {
        stateStore.value = stateStore.value + (id to info)
    }
}
