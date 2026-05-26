package io.github.mobilebytelabs.worker

import kotlin.uuid.Uuid

interface WorkContinuation {
    /** Append a sequential step after the current chain tail. */
    fun then(work: OneTimeWorkRequest): WorkContinuation

    /** Append multiple parallel steps after the current chain tail. */
    fun then(works: List<OneTimeWorkRequest>): WorkContinuation

    /** Enqueue the full chain. Returns all assigned IDs in submission order. */
    suspend fun enqueue(): List<Uuid>
}

fun WorkManager.beginWith(work: OneTimeWorkRequest): WorkContinuation = beginWith(listOf(work))

fun WorkManager.beginWith(works: List<OneTimeWorkRequest>): WorkContinuation =
    DefaultWorkContinuation(workManager = this, initialWork = works)

internal class DefaultWorkContinuation(
    private val workManager: WorkManager,
    private val initialWork: List<OneTimeWorkRequest>,
    private val chain: List<List<OneTimeWorkRequest>> = emptyList(),
) : WorkContinuation {

    override fun then(work: OneTimeWorkRequest): WorkContinuation = then(listOf(work))

    override fun then(works: List<OneTimeWorkRequest>): WorkContinuation =
        DefaultWorkContinuation(workManager, initialWork, chain + listOf(works))

    override suspend fun enqueue(): List<Uuid> {
        val ids = mutableListOf<Uuid>()
        initialWork.forEach { ids.add(workManager.enqueue(it)) }
        chain.forEach { step -> step.forEach { ids.add(workManager.enqueue(it)) } }
        return ids
    }
}
