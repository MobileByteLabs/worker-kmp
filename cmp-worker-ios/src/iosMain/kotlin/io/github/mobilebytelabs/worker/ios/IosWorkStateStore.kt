package io.github.mobilebytelabs.worker.ios

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal class IosWorkStateStore(
    private val persistence: IosWorkPersistence = NoOpIosWorkPersistence,
    private val scope: CoroutineScope? = null,
) {
    private val store = MutableStateFlow<Map<Uuid, WorkInfo>>(emptyMap())

    // RUNNING → ENQUEUED on restore (worker was interrupted by an app kill mid-execution).
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

    fun initWork(id: Uuid, tags: Set<String>) {
        var added = false
        store.update { map ->
            if (map[id] != null) return@update map
            added = true
            map + (id to WorkInfo(id = id, state = WorkInfo.State.ENQUEUED, tags = tags))
        }
        if (added) store.value[id]?.let { persist(it) }
    }

    fun transitionToRunning(id: Uuid): Boolean {
        // CAS-loop prevents a concurrent cancelWorkById from being silently overwritten.
        // Periodic work re-runs from SUCCEEDED, so only CANCELLED is blocked (not all states).
        var didTransition = false
        store.update { map ->
            val info = map[id]
            if (info == null || info.state == WorkInfo.State.CANCELLED) {
                didTransition = false
                return@update map
            }
            didTransition = true
            map + (id to info.copy(state = WorkInfo.State.RUNNING))
        }
        if (didTransition) store.value[id]?.let { persist(it) }
        return didTransition
    }

    fun updateState(id: Uuid, state: WorkInfo.State, outputData: WorkData = WorkData.EMPTY) {
        store.update { map ->
            val info = map[id] ?: WorkInfo(id = id, state = state, outputData = outputData)
            map + (id to info.copy(state = state, outputData = outputData))
        }
        if (state.isTerminal()) {
            deleteFromPersistence(id)
        } else {
            store.value[id]?.let { persist(it) }
        }
    }

    fun updateProgress(id: Uuid, progress: WorkProgress) {
        store.update { map ->
            val info = map[id] ?: return@update map
            map + (id to info.copy(progress = progress))
        }
        store.value[id]?.let { persist(it) }
    }

    fun incrementAttempt(id: Uuid) {
        store.update { map ->
            val info = map[id] ?: return@update map
            map + (id to info.copy(runAttemptCount = info.runAttemptCount + 1))
        }
        store.value[id]?.let { persist(it) }
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
