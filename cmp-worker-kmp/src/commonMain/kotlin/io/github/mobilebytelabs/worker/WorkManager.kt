package io.github.mobilebytelabs.worker

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface WorkManager {
    /** Enqueue a one-time work request. Returns the work ID. Idempotent on same request.id. */
    suspend fun enqueue(request: OneTimeWorkRequest): Uuid

    /** Enqueue a periodic work request with uniqueness policy. */
    suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid

    /** Cancel work by ID. No-op if already SUCCEEDED/FAILED. */
    suspend fun cancelWorkById(id: Uuid)

    /** Cancel all work matching the given tag. */
    suspend fun cancelAllWorkByTag(tag: String)

    /**
     * Hot Flow that emits the current list of work items matching [tag] whenever state changes.
     * Never completes — cancel the collector to stop observing.
     */
    fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>>

    /** Snapshot of a single work item. Returns null if unknown. */
    suspend fun getWorkInfoById(id: Uuid): WorkInfo?
}
