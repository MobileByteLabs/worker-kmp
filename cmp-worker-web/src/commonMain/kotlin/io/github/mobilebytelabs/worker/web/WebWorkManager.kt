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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalWorkerApi::class)
class WebWorkManager internal constructor(
    private val workerFactory: WebWorkerFactory,
    private val config: WebWorkManagerConfig = WebWorkManagerConfig.DEFAULT,
    private val constraintEvaluator: WebConstraintEvaluator,
    private val persistence: WebWorkPersistence = createWebWorkPersistence(config),
) : WorkManager {

    constructor(
        workerFactory: WebWorkerFactory,
        config: WebWorkManagerConfig = WebWorkManagerConfig.DEFAULT,
    ) : this(workerFactory, config, defaultConstraintEvaluator())

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("WebWorkManager"),
    )
    private val jobRegistry = mutableMapOf<Uuid, Job>()
    private val stateStore = WebWorkStateStore(persistence = persistence, scope = scope)

    /**
     * Subscription to `BroadcastChannel('worker-kmp')` — receives cross-tab updates from the
     * worker-kmp Service Worker after it marks ENQUEUED entries as RUNNING from a push event.
     * On `PENDING_PROCESSED`, we re-load state from IndexedDB so the UI re-renders without a
     * manual refresh.
     *
     * Added in v3.0.0-alpha06.X (Phase 9 alpha06.X). JS-only; WasmJs + JVM actuals are
     * no-ops — they continue to discover changes via the constraint-check polling interval.
     */
    private val broadcastSubscription: WorkerKmpBroadcastSubscription =
        openWorkerKmpBroadcastChannel { eventType, _ ->
            if (eventType == "PENDING_PROCESSED") {
                scope.launch { stateStore.restoreFromPersistence() }
            }
        }

    init {
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
        val existing = stateStore.snapshot().values
            .firstOrNull { uniqueWorkName in it.tags && !it.state.isFinished }
        when (existingPeriodicWorkPolicy) {
            ExistingPeriodicWorkPolicy.KEEP -> if (existing != null) return existing.id

            ExistingPeriodicWorkPolicy.REPLACE,
            ExistingPeriodicWorkPolicy.UPDATE,
            -> cancelAllWorkByTag(uniqueWorkName)
        }
        stateStore.initWork(request.id, request.tags + uniqueWorkName)
        // Periodic Background Sync registration (best-effort — falls back to polling).
        if (config.enablePeriodicBackgroundSync) {
            val tag = "worker-kmp-periodic-${request.id}"
            registerPeriodicSyncTag(tag, request.repeatInterval.inWholeMilliseconds, config.serviceWorkerScript)
        }
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
        stateStore.updateState(id, WorkInfo.State.CANCELLED)
    }

    override suspend fun cancelAllWorkByTag(tag: String) {
        stateStore.snapshot().values
            .filter { tag in it.tags }
            .forEach { cancelWorkById(it.id) }
    }

    // cross-platform-worker-parity-audit sub-plan 03 (closes G2) — Web delegates to plain
    // enqueue. Full name-keyed REPLACE/KEEP/APPEND semantics deferred (browser tabs have
    // ephemeral state — full impl needs IndexedDB name→uuid map).
    override suspend fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: io.github.mobilebytelabs.worker.ExistingWorkPolicy,
        request: io.github.mobilebytelabs.worker.OneTimeWorkRequest,
    ): Uuid = enqueue(request)

    override fun getWorkInfosByTag(tag: String): Flow<List<WorkInfo>> = stateStore.observeByTag(tag)

    // worker-kmp-single-api-completion sub-plan 01 — uniqueWorkName is added to the work's
    // tag set at enqueue time (see line ~99), so unique-work observation reduces to tag
    // observation with the uniqueWorkName as the tag.
    override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>> =
        stateStore.observeByTag(uniqueWorkName)

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

    private suspend fun awaitConstraintsSatisfied(request: WorkRequest) {
        if (constraintEvaluator.evaluate(request.constraints)) return
        val syncTag = "worker-kmp-${request.id}"
        if (config.enableBackgroundSync) {
            registerBackgroundSyncTag(syncTag, config.serviceWorkerScript)
        }
        // Timer guarantees we re-check even on platforms without a network watcher.
        // Online-watcher fires immediately on network state changes, short-circuiting the timer.
        val timerFlow = flow {
            while (true) {
                delay(config.constraintCheckIntervalMs)
                emit(Unit)
            }
        }
        val sources = buildList {
            add(timerFlow)
            add(onlineWatcher())
            if (config.enableBackgroundSync && isBackgroundSyncSupported()) {
                add(backgroundSyncFlow(syncTag))
            }
        }
        merge(*sources.toTypedArray())
            .first { constraintEvaluator.evaluate(request.constraints) }
    }

    fun shutdown() {
        broadcastSubscription.close()
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
