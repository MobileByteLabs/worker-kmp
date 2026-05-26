package io.github.mobilebytelabs.worker

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
        initialWork.forEach { ids.add(workManager.enqueue(it)) }
        chain.forEach { step -> step.forEach { ids.add(workManager.enqueue(it)) } }
        return ids
    }
}
