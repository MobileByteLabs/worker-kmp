package io.github.mobilebytelabs.worker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

class TestWorkManager : WorkManager {
    private val stateStore = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    val enqueuedRequests: List<WorkRequest> get() = _enqueuedRequests.toList()
    val cancelledIds: Set<Uuid> get() = _cancelledIds.toSet()

    /**
     * Lifecycle events captured by the in-test [WorkObserver] hook.
     *
     * Every simulate* call appends one or more [WorkEvent] entries to this list, in the order
     * a real platform [WorkManager] would emit them. Use this in tests instead of registering
     * a [WorkObserver] manually when you only need to assert on the recorded sequence.
     *
     * Added in v2.2.0 alongside the [WorkObserver] SPI.
     */
    val observedEvents: List<WorkEvent> get() = _observedEvents.toList()

    private val _enqueuedRequests = mutableListOf<WorkRequest>()
    private val _cancelledIds = mutableSetOf<Uuid>()
    private val _observedEvents = mutableListOf<WorkEvent>()

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        _enqueuedRequests.add(request)
        updateState(request.id, WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags))
        _observedEvents += WorkEvent.Enqueued(
            id = request.id,
            tag = request.tags.firstOrNull(),
            inputData = request.inputData,
        )
        return request.id
    }

    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid {
        _enqueuedRequests.add(request)
        updateState(request.id, WorkInfo(id = request.id, state = WorkInfo.State.ENQUEUED, tags = request.tags))
        _observedEvents += WorkEvent.Enqueued(
            id = request.id,
            tag = request.tags.firstOrNull(),
            inputData = request.inputData,
        )
        return request.id
    }

    override suspend fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        request: OneTimeWorkRequest,
    ): Uuid = enqueue(request)

    override suspend fun cancelWorkById(id: Uuid) {
        _cancelledIds.add(id)
        stateStore.value[id]?.let { updateState(id, it.copy(state = WorkInfo.State.CANCELLED)) }
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        stateStore.value.values.filter { tag in it.tags }.forEach { cancelWorkById(it.id) }
    }

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> =
        stateStore.map { infos -> infos.values.filter { tag in it.tags } }

    // v4.0.0 single-API completion — uniqueWorkName is added to the work's tag set at enqueue
    // time, so unique-work observation reduces to tag observation. Matches the implementation
    // strategy used by IosWorkManager / DesktopWorkManager / WebWorkManager.
    override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>> =
        getWorkInfosByTag(uniqueWorkName)

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = stateStore.value[id]

    fun simulateSuccess(id: Uuid, outputData: WorkData = WorkData.EMPTY) {
        stateStore.value[id]?.let {
            updateState(id, it.copy(state = WorkInfo.State.SUCCEEDED, outputData = outputData))
            _observedEvents += WorkEvent.Started(id = id, attemptCount = it.runAttemptCount.coerceAtLeast(1))
            _observedEvents += WorkEvent.Resulted(
                id = id,
                result = WorkResult.success(outputData),
                durationMs = 0L,
            )
        }
    }

    fun simulateFailure(id: Uuid, message: String = "") {
        stateStore.value[id]?.let {
            updateState(id, it.copy(state = WorkInfo.State.FAILED))
            _observedEvents += WorkEvent.Started(id = id, attemptCount = it.runAttemptCount.coerceAtLeast(1))
            _observedEvents += WorkEvent.Resulted(
                id = id,
                result = WorkResult.failure(message),
                durationMs = 0L,
            )
        }
    }

    fun simulateProgress(id: Uuid, progress: WorkProgress) {
        stateStore.value[id]?.let {
            updateState(id, it.copy(progress = progress))
            _observedEvents += WorkEvent.Progress(id = id, progress = progress)
        }
    }

    fun simulateRunning(id: Uuid) {
        stateStore.value[id]?.let {
            updateState(id, it.copy(state = WorkInfo.State.RUNNING))
            _observedEvents += WorkEvent.Started(id = id, attemptCount = it.runAttemptCount.coerceAtLeast(1))
        }
    }

    fun simulateStateChange(id: Uuid, workState: WorkInfo.State) {
        stateStore.value[id]?.let { updateState(id, it.copy(state = workState)) }
    }

    fun reset() {
        _enqueuedRequests.clear()
        _cancelledIds.clear()
        _observedEvents.clear()
        stateStore.value = emptyMap()
    }

    /**
     * Clears only the [observedEvents] list — useful between assertion phases in a single test
     * without resetting the state store or enqueue history.
     *
     * Added in v2.2.0.
     */
    fun clearObservedEvents() {
        _observedEvents.clear()
    }

    private fun updateState(id: Uuid, info: WorkInfo) {
        stateStore.value = stateStore.value + (id to info)
    }
}
