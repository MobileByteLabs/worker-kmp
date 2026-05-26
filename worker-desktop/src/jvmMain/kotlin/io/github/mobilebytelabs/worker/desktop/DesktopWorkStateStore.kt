package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

internal class DesktopWorkStateStore {
    private val store = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    fun updateState(id: Uuid, state: WorkInfo.State, outputData: WorkData = WorkData.EMPTY) {
        while (true) {
            val current = store.value
            val info = current[id] ?: WorkInfo(id = id, state = state, outputData = outputData)
            val updated = current + (id to info.copy(state = state, outputData = outputData))
            if (store.compareAndSet(current, updated)) return
        }
    }

    fun updateProgress(id: Uuid, progress: WorkProgress) {
        while (true) {
            val current = store.value
            val info = current[id] ?: return
            val updated = current + (id to info.copy(progress = progress))
            if (store.compareAndSet(current, updated)) return
        }
    }

    fun incrementAttempt(id: Uuid) {
        while (true) {
            val current = store.value
            val info = current[id] ?: return
            val updated = current + (id to info.copy(runAttemptCount = info.runAttemptCount + 1))
            if (store.compareAndSet(current, updated)) return
        }
    }

    fun initWork(id: Uuid, tags: Set<String>) {
        while (true) {
            val current = store.value
            if (current[id] != null) return
            val updated = current + (id to WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = tags))
            if (store.compareAndSet(current, updated)) return
        }
    }

    /**
     * Atomically transitions [id] to RUNNING unless already CANCELLED.
     * Returns false if the work was cancelled (or not found).
     * Allows re-entry from SUCCEEDED/FAILED for periodic work loops.
     */
    fun transitionToRunning(id: Uuid): Boolean {
        while (true) {
            val current = store.value
            val info = current[id] ?: return false
            if (info.state == WorkInfo.State.CANCELLED) return false
            val updated = current + (id to info.copy(state = WorkInfo.State.RUNNING))
            if (store.compareAndSet(current, updated)) return true
        }
    }

    suspend fun getById(id: Uuid): WorkInfo? = store.value[id]

    fun observeByTag(tag: String): Flow<List<WorkInfo>> =
        store.map { infos -> infos.values.filter { tag in it.tags } }

    fun snapshot(): Map<Uuid, WorkInfo> = store.value
}
