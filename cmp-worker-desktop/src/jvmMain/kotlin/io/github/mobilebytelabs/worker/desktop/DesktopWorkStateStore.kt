package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal class DesktopWorkStateStore(
    private val persistence: DesktopWorkPersistence = NoOpDesktopWorkPersistence,
    private val scope: CoroutineScope? = null,
) {
    private val store = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    // RUNNING → ENQUEUED on restore (worker was interrupted mid-execution by a JVM shutdown).
    // Only populates when the store is still empty — prevents clobbering in-flight mutations.
    suspend fun restoreFromPersistence() {
        val saved = persistence.loadAll()
        if (saved.isEmpty()) return
        val restored = saved.associate { info ->
            val state = if (info.state == WorkInfo.State.RUNNING) WorkInfo.State.ENQUEUED else info.state
            info.id to info.copy(state = state)
        }
        store.compareAndSet(emptyMap(), restored)
    }

    fun updateState(id: Uuid, state: WorkInfo.State, outputData: WorkData = WorkData.EMPTY) {
        while (true) {
            val current = store.value
            val info = current[id] ?: WorkInfo(id = id, state = state, outputData = outputData)
            val updated = current + (id to info.copy(state = state, outputData = outputData))
            if (store.compareAndSet(current, updated)) break
        }
        if (state.isTerminal()) deleteFromPersistence(id) else store.value[id]?.let { persist(it) }
    }

    fun updateProgress(id: Uuid, progress: WorkProgress) {
        while (true) {
            val current = store.value
            val info = current[id] ?: return
            val updated = current + (id to info.copy(progress = progress))
            if (store.compareAndSet(current, updated)) return
        }
        store.value[id]?.let { persist(it) }
    }

    fun incrementAttempt(id: Uuid) {
        while (true) {
            val current = store.value
            val info = current[id] ?: return
            val updated = current + (id to info.copy(runAttemptCount = info.runAttemptCount + 1))
            if (store.compareAndSet(current, updated)) return
        }
        store.value[id]?.let { persist(it) }
    }

    fun initWork(id: Uuid, tags: Set<String>) {
        var added = false
        while (true) {
            val current = store.value
            if (current[id] != null) return
            val updated = current + (id to WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = tags))
            if (store.compareAndSet(current, updated)) {
                added = true
                break
            }
        }
        if (added) store.value[id]?.let { persist(it) }
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
            if (store.compareAndSet(current, updated)) {
                store.value[id]?.let { persist(it) }
                return true
            }
        }
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
