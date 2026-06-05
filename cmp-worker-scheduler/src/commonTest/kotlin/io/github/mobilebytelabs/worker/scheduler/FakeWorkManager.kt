package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Minimal recording fake of [WorkManager] for testing the scheduler façade.
 *
 * Captures every enqueued [WorkRequest] in [enqueuedRequests] (in-order). Each enqueue
 * starts the request in [WorkInfo.State.ENQUEUED]; [simulateRunning] / [simulateSuccess]
 * / [simulateFailure] flip per-id state for `observeWork`-style tests.
 *
 * For periodic enqueues, [periodicPolicyByUniqueName] captures the policy passed.
 */
@OptIn(ExperimentalUuidApi::class)
internal class FakeWorkManager : WorkManager {
    private val state = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())
    private val _enqueuedRequests = mutableListOf<WorkRequest>()
    private val _cancelledIds = mutableSetOf<Uuid>()
    private val _cancelledTags = mutableSetOf<String>()
    private val _periodicPolicy = mutableMapOf<String, ExistingPeriodicWorkPolicy>()

    val enqueuedRequests: List<WorkRequest> get() = _enqueuedRequests.toList()
    val cancelledTags: Set<String> get() = _cancelledTags.toSet()
    val periodicPolicyByUniqueName: Map<String, ExistingPeriodicWorkPolicy> get() = _periodicPolicy.toMap()

    fun clearRequests() { _enqueuedRequests.clear() }

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        _enqueuedRequests += request
        update(request.id, WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags))
        return request.id
    }

    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid {
        _enqueuedRequests += request
        _periodicPolicy[uniqueWorkName] = existingPeriodicWorkPolicy
        update(request.id, WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags))
        return request.id
    }
    override suspend fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: io.github.mobilebytelabs.worker.ExistingWorkPolicy,
        request: io.github.mobilebytelabs.worker.OneTimeWorkRequest,
    ): kotlin.uuid.Uuid = enqueue(request)

    override suspend fun cancelWorkById(id: Uuid) {
        _cancelledIds += id
        state.value[id]?.let { update(id, it.copy(state = WorkInfo.State.CANCELLED)) }
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        _cancelledTags += tag
        state.value.values.filter { tag in it.tags }.forEach {
            update(it.id, it.copy(state = WorkInfo.State.CANCELLED))
        }
    }

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> =
        state.map { it.values.filter { info -> tag in info.tags } }

    // v4.0.0 single-API completion — uniqueWorkName-as-tag delegation.
    override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>> =
        getWorkInfosByTag(uniqueWorkName)

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = state.value[id]

    fun simulateRunning(id: Uuid) {
        state.value[id]?.let { update(id, it.copy(state = WorkInfo.State.RUNNING)) }
    }

    fun simulateSuccess(id: Uuid) {
        state.value[id]?.let { update(id, it.copy(state = WorkInfo.State.SUCCEEDED)) }
    }

    fun simulateFailure(id: Uuid) {
        state.value[id]?.let { update(id, it.copy(state = WorkInfo.State.FAILED)) }
    }

    fun simulateBlocked(id: Uuid) {
        state.value[id]?.let { update(id, it.copy(state = WorkInfo.State.BLOCKED)) }
    }

    private fun update(id: Uuid, info: WorkInfo) {
        state.value = state.value + (id to info)
    }
}
