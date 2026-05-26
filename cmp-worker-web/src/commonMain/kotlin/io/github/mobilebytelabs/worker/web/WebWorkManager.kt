package io.github.mobilebytelabs.worker.web

import io.github.mobilebytelabs.worker.BackoffPolicy
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.ExperimentalWorkerApi
import io.github.mobilebytelabs.worker.OneTimeWorkRequest
import io.github.mobilebytelabs.worker.PeriodicWorkRequest
import io.github.mobilebytelabs.worker.RetryConfig
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.WorkRequest
import io.github.mobilebytelabs.worker.WorkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalWorkerApi::class)
class WebWorkManager(private val workerFactory: WebWorkerFactory) : WorkManager {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("WebWorkManager"),
    )
    private val jobRegistry = mutableMapOf<Uuid, Job>()
    private val stateStore = WebWorkStateStore()

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        stateStore.initWork(request.id, request.tags)
        val job = scope.launch { executeWorker(request) }
        jobRegistry[request.id] = job
        return request.id
    }

    override suspend fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Uuid {
        if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.REPLACE) {
            cancelAllWorkByTag(uniqueWorkName)
        }
        stateStore.initWork(request.id, request.tags + uniqueWorkName)
        val job = scope.launch {
            while (isActive) {
                executeWorker(request)
                delay(request.repeatInterval.inWholeMilliseconds)
            }
        }
        jobRegistry[request.id] = job
        return request.id
    }

    override suspend fun cancelWorkById(id: Uuid) {
        val job = jobRegistry.remove(id) ?: return
        job.cancel()
        stateStore.updateState(id, WorkInfo.State.CANCELLED)
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        stateStore.snapshot().values
            .filter { tag in it.tags }
            .forEach { cancelWorkById(it.id) }
    }

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = stateStore.observeByTag(tag)

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = stateStore.getById(id)

    private suspend fun executeWorker(request: WorkRequest) {
        val context = WebWorkerContext(
            id = request.id,
            inputData = request.inputData,
            tags = request.tags,
            stateStore = stateStore,
        )
        val worker = workerFactory.create(request.workerClass, context)
        if (!stateStore.transitionToRunning(request.id)) return

        var attempt = 0
        var result: WorkResult
        try {
            do {
                stateStore.incrementAttempt(request.id)
                result = try {
                    worker.doWork()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    WorkResult.Failure(e.message ?: "Unknown error")
                }

                if (result is WorkResult.Retry && attempt < request.retryConfig.maxAttempts - 1) {
                    delay(webBackoffDelay(request.retryConfig, attempt).inWholeMilliseconds)
                    attempt++
                } else {
                    break
                }
            } while (true)

            val finalState = when (result) {
                is WorkResult.Success -> WorkInfo.State.SUCCEEDED
                else -> WorkInfo.State.FAILED
            }
            stateStore.updateState(
                request.id,
                finalState,
                (result as? WorkResult.Success)?.outputData ?: WorkData.EMPTY,
            )
        } catch (e: CancellationException) {
            stateStore.updateState(request.id, WorkInfo.State.CANCELLED)
            throw e
        }
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
    }
}

internal fun webBackoffDelay(config: RetryConfig, attempt: Int): Duration {
    val delayMs = when (config.backoffPolicy) {
        BackoffPolicy.EXPONENTIAL ->
            (config.initialDelay.inWholeMilliseconds * config.multiplier.pow(attempt)).toLong()

        BackoffPolicy.LINEAR ->
            config.initialDelay.inWholeMilliseconds * (attempt + 1)
    }.coerceAtMost(config.maxDelay.inWholeMilliseconds)
    return delayMs.milliseconds
}
