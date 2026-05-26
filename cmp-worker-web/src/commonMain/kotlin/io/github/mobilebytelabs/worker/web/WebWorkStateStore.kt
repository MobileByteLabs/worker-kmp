package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal class WebWorkStateStore(
    private val persistence: WebWorkPersistence = NoOpWebWorkPersistence,
    private val scope: CoroutineScope? = null,
) {
    private val store = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    // RUNNING → ENQUEUED on restore (worker was interrupted by page reload).
    // Only populates the store when it is still empty — prevents clobbering in-flight mutations.
    suspend fun restoreFromPersistence() {
        val saved = persistence.loadAll()
        if (saved.isEmpty()) return
        val restored = saved.associate { info ->
            val state = if (info.state == WorkInfo.State.RUNNING) WorkInfo.State.ENQUEUED else info.state
            info.id to info.copy(state = state)
        }
        store.compareAndSet(emptyMap(), restored)
    }

    fun initWork(id: Uuid, tags: Set<String>) {
        val current = store.value
        if (current[id] != null) return
        val info = WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = tags)
        store.value = current + (id to info)
        persist(info)
    }

    fun transitionToRunning(id: Uuid): Boolean {
        val current = store.value
        val info = current[id] ?: return false
        if (info.state == WorkInfo.State.CANCELLED) return false
        val updated = info.copy(state = WorkInfo.State.RUNNING)
        store.value = current + (id to updated)
        persist(updated)
        return true
    }

    fun updateState(id: Uuid, state: WorkInfo.State, outputData: WorkData = WorkData.EMPTY) {
        val current = store.value
        val info = current[id] ?: WorkInfo(id = id, state = state, outputData = outputData)
        val updated = info.copy(state = state, outputData = outputData)
        store.value = current + (id to updated)
        if (state.isTerminal()) {
            deleteFromPersistence(id)
        } else {
            persist(updated)
        }
    }

    fun updateProgress(id: Uuid, progress: WorkProgress) {
        val current = store.value
        val info = current[id] ?: return
        val updated = info.copy(progress = progress)
        store.value = current + (id to updated)
        persist(updated)
    }

    fun incrementAttempt(id: Uuid) {
        val current = store.value
        val info = current[id] ?: return
        val updated = info.copy(runAttemptCount = info.runAttemptCount + 1)
        store.value = current + (id to updated)
        persist(updated)
    }

    suspend fun getById(id: Uuid): WorkInfo? = store.value[id]

    fun observeByTag(tag: String): Flow<List<WorkInfo>> = store.map { infos -> infos.values.filter { tag in it.tags } }

    fun snapshot(): Map<Uuid, WorkInfo> = store.value

    private fun persist(info: WorkInfo) {
        scope?.launch { persistence.save(info) }
    }

    private fun deleteFromPersistence(id: Uuid) {
        scope?.launch { persistence.delete(id) }
    }
}

private fun WorkInfo.State.isTerminal() = this == WorkInfo.State.SUCCEEDED ||
    this == WorkInfo.State.FAILED ||
    this == WorkInfo.State.CANCELLED

private object NoOpWebWorkPersistence : WebWorkPersistence {
    override suspend fun save(info: WorkInfo) = Unit
    override suspend fun loadAll(): List<WorkInfo> = emptyList()
    override suspend fun delete(id: Uuid) = Unit
}
