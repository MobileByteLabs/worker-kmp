package io.github.mobilebytelabs.worker.desktop

import io.github.mobilebytelabs.worker.BackoffPolicy
import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

class DesktopWorkManager internal constructor(
    private val config: DesktopWorkManagerConfig = DesktopWorkManagerConfig.DEFAULT,
    private val workerFactory: DesktopWorkerFactory = ReflectionWorkerFactory,
    private val persistence: DesktopWorkPersistence,
) : WorkManager {

    constructor(
        config: DesktopWorkManagerConfig = DesktopWorkManagerConfig.DEFAULT,
        workerFactory: DesktopWorkerFactory = ReflectionWorkerFactory,
    ) : this(config, workerFactory, createDesktopWorkPersistence(config))

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("DesktopWorkManager"),
    )
    private val jobRegistry = ConcurrentHashMap<Uuid, Job>()
    private val stateStore = DesktopWorkStateStore(persistence, scope)
    private val constraintEvaluator = DesktopConstraintEvaluator(config)

    init {
        // Restore work that was pending when the JVM last exited.
        scope.launch { stateStore.restoreFromPersistence() }
    }

    override suspend fun enqueue(request: OneTimeWorkRequest): Uuid {
        stateStore.initWork(request.id, request.tags)
        val job = scope.launch {
            val initialDelayMs = request.initialDelay.inWholeMilliseconds
            if (initialDelayMs > 0) delay(initialDelayMs)
            if (!constraintEvaluator.evaluate(request.constraints)) {
                awaitConstraintsSatisfied(request)
            }
            executeWorker(request)
        }
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
            val initialDelayMs = request.initialDelay.inWholeMilliseconds
            if (initialDelayMs > 0) delay(initialDelayMs)
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
        // Set CANCELLED immediately; executeWorker's CancellationException catch also sets it,
        // so this is the backstop for work that hasn't entered executeWorker yet (ENQUEUED).
        stateStore.updateState(id, WorkInfo.State.CANCELLED)
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        stateStore.snapshot().values
            .filter { tag in it.tags }
            .forEach { cancelWorkById(it.id) }
    }

    // cross-platform-worker-parity-audit sub-plan 03 (closes G2) — Desktop has its own
    // in-process state map but no name-keyed unique-work primitive in the existing design,
    // so this is a thin delegate that ships uniformly. Tracked: full name-keyed semantics
    // (REPLACE / KEEP / APPEND) for Desktop = follow-up.
    override suspend fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: io.github.mobilebytelabs.worker.ExistingWorkPolicy,
        request: io.github.mobilebytelabs.worker.OneTimeWorkRequest,
    ): Uuid = enqueue(request)

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = stateStore.observeByTag(tag)

    override suspend fun getWorkInfoById(id: Uuid): WorkInfo? = stateStore.getById(id)

    private suspend fun executeWorker(request: WorkRequest) {
        val context = DesktopWorkerContext(
            id = request.id,
            inputData = request.inputData,
            tags = request.tags,
            stateStore = stateStore,
        )
        val worker = workerFactory.create(request.workerClass, context)
        if (!stateStore.transitionToRunning(request.id)) return // already cancelled

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
                    delay(backoffDelay(request.retryConfig, attempt).inWholeMilliseconds)
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

    private suspend fun awaitConstraintsSatisfied(request: WorkRequest) {
        while (!constraintEvaluator.evaluate(request.constraints)) {
            delay(config.constraintCheckIntervalMs)
        }
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
    }
}

internal fun backoffDelay(config: RetryConfig, attempt: Int): Duration {
    val delayMs = when (config.backoffPolicy) {
        BackoffPolicy.EXPONENTIAL ->
            (config.initialDelay.inWholeMilliseconds * config.multiplier.pow(attempt)).toLong()

        BackoffPolicy.LINEAR ->
            config.initialDelay.inWholeMilliseconds * (attempt + 1)
    }.coerceAtMost(config.maxDelay.inWholeMilliseconds)
    return delayMs.milliseconds
}

interface DesktopWorkerFactory {
    fun create(workerClass: String, context: io.github.mobilebytelabs.worker.WorkerContext): CoroutineWorker
}

internal object ReflectionWorkerFactory : DesktopWorkerFactory {
    override fun create(workerClass: String, context: io.github.mobilebytelabs.worker.WorkerContext): CoroutineWorker {
        val clazz = Class.forName(workerClass)
        val constructor = clazz.getDeclaredConstructor(
            io.github.mobilebytelabs.worker.WorkerContext::class.java,
        )
        return constructor.newInstance(context) as CoroutineWorker
    }
}
