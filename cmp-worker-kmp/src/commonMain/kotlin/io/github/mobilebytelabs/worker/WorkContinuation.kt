package io.github.mobilebytelabs.worker

import kotlinx.coroutines.delay
import kotlin.uuid.Uuid

/**
 * A chainable sequence of [OneTimeWorkRequest] tasks submitted together as a unit.
 *
 * Build a chain with [WorkManager.beginWith], then append steps with [then], and finally
 * call [enqueue] to submit the whole chain:
 *
 * ```kotlin
 * workManager
 *     .beginWith(downloadRequest)
 *     .then(parseRequest)
 *     .then(listOf(notifyRequest, cacheRequest)) // parallel step
 *     .enqueue()
 * ```
 *
 * All requests in a chain share the same logical group. Parallel steps (a [List] passed
 * to [then]) start concurrently once the preceding step finishes. The chain halts if any
 * step transitions to [WorkInfo.State.FAILED] or [WorkInfo.State.CANCELLED].
 *
 * Note: the current default implementation ([DefaultWorkContinuation]) enqueues steps
 * sequentially rather than enforcing hard dependencies on platforms that lack native
 * chaining support. Android's implementation delegates to `androidx.work` chaining.
 */
interface WorkContinuation {
    /**
     * Appends a single sequential step after the current chain tail.
     *
     * @param work the next work unit to run after all current steps complete.
     * @return a new [WorkContinuation] with [work] appended.
     */
    fun then(work: OneTimeWorkRequest): WorkContinuation

    /**
     * Appends multiple parallel steps after the current chain tail.
     *
     * All items in [works] will start concurrently once the preceding step completes.
     *
     * @param works the parallel work units to run after all current steps complete.
     * @return a new [WorkContinuation] with [works] appended as a parallel step.
     */
    fun then(works: List<OneTimeWorkRequest>): WorkContinuation

    /**
     * Submits the entire chain to [WorkManager] and returns the assigned IDs.
     *
     * IDs are returned in submission order: initial work first, then each subsequent step
     * left-to-right.
     *
     * @return list of [Uuid] values assigned to each enqueued request.
     * @throws WorkEnqueueException if any individual enqueue call fails.
     */
    suspend fun enqueue(): List<Uuid>
}

/**
 * Begins a [WorkContinuation] chain starting with a single [work] request.
 *
 * @param work the first work unit in the chain.
 * @return a [WorkContinuation] ready for further chaining or [WorkContinuation.enqueue].
 */
fun WorkManager.beginWith(work: OneTimeWorkRequest): WorkContinuation = beginWith(listOf(work))

/**
 * Begins a [WorkContinuation] chain starting with a set of parallel [works].
 *
 * @param works the initial parallel work units.
 * @return a [WorkContinuation] ready for further chaining or [WorkContinuation.enqueue].
 */
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
        var carried = WorkData.EMPTY

        val initialInfos = executeStep(initialWork, carried, ids) ?: return ids
        carried = mergeOutputs(initialInfos)

        for (step in chain) {
            val stepInfos = executeStep(step, carried, ids) ?: return ids
            carried = carried.mergeWith(mergeOutputs(stepInfos))
        }

        return ids
    }

    private suspend fun executeStep(
        requests: List<OneTimeWorkRequest>,
        inputOverride: WorkData,
        ids: MutableList<Uuid>,
    ): List<WorkInfo>? {
        val enqueued = requests.map { req ->
            val merged = if (inputOverride != WorkData.EMPTY) {
                req.copy(inputData = inputOverride.mergeWith(req.inputData))
            } else {
                req
            }
            workManager.enqueue(merged).also { ids.add(it) }
        }

        val finished = enqueued.map { id -> awaitFinished(id) }
        return if (finished.any { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }) {
            null
        } else {
            finished
        }
    }

    private suspend fun awaitFinished(id: Uuid): WorkInfo {
        while (true) {
            val info = workManager.getWorkInfoById(id)
            if (info != null && info.isFinished) return info
            delay(POLL_MS)
        }
    }

    private fun mergeOutputs(infos: List<WorkInfo>): WorkData =
        infos.fold(WorkData.EMPTY) { acc, info -> acc.mergeWith(info.outputData) }

    private companion object {
        const val POLL_MS = 50L
    }
}

internal fun WorkData.mergeWith(other: WorkData): WorkData {
    if (other == WorkData.EMPTY) return this
    if (this == WorkData.EMPTY) return other
    val combined = HashMap(keyValueMap())
    combined.putAll(other.keyValueMap())
    return WorkData(*combined.entries.map { it.key to it.value }.toTypedArray())
}
