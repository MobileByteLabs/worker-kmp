package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

internal class WebWorkStateStore {
    private val store = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    fun initWork(id: Uuid, tags: Set<String>) {
        val current = store.value
        if (current[id] != null) return
        store.value = current + (id to WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = tags))
    }

    fun transitionToRunning(id: Uuid): Boolean {
        val current = store.value
        val info = current[id] ?: return false
        if (info.state == WorkInfo.State.CANCELLED) return false
        store.value = current + (id to info.copy(state = WorkInfo.State.RUNNING))
        return true
    }

    fun updateState(id: Uuid, state: WorkInfo.State, outputData: WorkData = WorkData.EMPTY) {
        val current = store.value
        val info = current[id] ?: WorkInfo(id = id, state = state, outputData = outputData)
        store.value = current + (id to info.copy(state = state, outputData = outputData))
    }

    fun updateProgress(id: Uuid, progress: WorkProgress) {
        val current = store.value
        val info = current[id] ?: return
        store.value = current + (id to info.copy(progress = progress))
    }

    fun incrementAttempt(id: Uuid) {
        val current = store.value
        val info = current[id] ?: return
        store.value = current + (id to info.copy(runAttemptCount = info.runAttemptCount + 1))
    }

    suspend fun getById(id: Uuid): WorkInfo? = store.value[id]

    fun observeByTag(tag: String): Flow<List<WorkInfo>> = store.map { infos -> infos.values.filter { tag in it.tags } }

    fun snapshot(): Map<Uuid, WorkInfo> = store.value
}
