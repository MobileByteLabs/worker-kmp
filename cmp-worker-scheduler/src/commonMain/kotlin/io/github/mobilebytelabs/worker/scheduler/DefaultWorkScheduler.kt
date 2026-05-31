package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy
import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.OutOfQuotaPolicy
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.WorkInfo
import io.github.mobilebytelabs.worker.WorkManager
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

/**
 * Default [WorkScheduler] backed by the library's [WorkManager].
 *
 * ## Strategy
 *
 * - Every scheduled work gets an `addTag(uniqueName)` so the WorkManager API's tag-based
 *   methods ([WorkManager.getWorkInfosByTag], [WorkManager.cancelAllWorkByTag]) serve as
 *   the unique-name lookup channel for [observeWork] / [cancelWork].
 * - Suspend bridging: WorkManager's enqueue methods are suspend; the [WorkScheduler] interface
 *   exposes non-suspend `fun schedule…(): WorkHandle` for ergonomic call sites. The impl
 *   holds a [CoroutineScope] and `launch`-es the suspend enqueue. [WorkHandle] is returned
 *   immediately with the request's pre-allocated Uuid.
 * - Worker-class threading: each schedule method receives a [KClass]`<W : AbstractDataSyncWorker>`
 *   parameter; the impl reads `simpleName` and passes it to [OneTimeWorkRequestBuilder] /
 *   [PeriodicWorkRequestBuilder]. WorkManager looks up the matching factory in
 *   [io.github.mobilebytelabs.worker.registry.WorkerRegistry] at execution time.
 */
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class DefaultWorkScheduler(
    private val workManager: WorkManager,
    @Suppress("UNUSED_PARAMETER") private val persister: SyncStatePersister,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : WorkScheduler {

    override fun <W : AbstractDataSyncWorker> enqueueDataSync(
        workerClass: KClass<W>,
        mode: WorkMode,
        payload: WorkData,
    ): WorkHandle {
        val name = workerClass.requireSimpleName()
        val request = OneTimeWorkRequestBuilder<W>(name).apply {
            setConstraints(SyncConstraints)
            setInputData(payload)
            addTag(SYNC_WORK_NAME)
            if (mode == WorkMode.Foreground) {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
        }.build()
        scope.launch { workManager.enqueue(request) }
        return WorkHandle(id = request.id, uniqueName = SYNC_WORK_NAME)
    }

    override fun <W : AbstractDataSyncWorker> scheduleDailyDataSync(
        workerClass: KClass<W>,
        timeOfDay: LocalTime,
        timeZone: TimeZone,
        payload: WorkData,
    ): WorkHandle {
        val name = workerClass.requireSimpleName()
        val nowInstant = Clock.System.now()
        val nextOccurrence = nowInstant.nextOccurrenceOf(timeOfDay, timeZone)
        val initialDelay = nextOccurrence - nowInstant
        val request = PeriodicWorkRequestBuilder<W>(name, repeatInterval = 24.hours).apply {
            setInitialDelay(initialDelay)
            setInputData(payload)
            setConstraints(SyncConstraints)
            addTag(DAILY_SYNC_WORK_NAME)
        }.build()
        scope.launch {
            workManager.enqueueUniquePeriodicWork(DAILY_SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
        return WorkHandle(id = request.id, uniqueName = DAILY_SYNC_WORK_NAME)
    }

    override fun <W : AbstractDataSyncWorker> schedulePeriodicDataSync(
        workerClass: KClass<W>,
        interval: Duration,
        initialDelay: Duration,
        payload: WorkData,
    ): WorkHandle {
        val name = workerClass.requireSimpleName()
        val clampedInterval = maxOf(interval, 15.minutes)
        val request = PeriodicWorkRequestBuilder<W>(name, repeatInterval = clampedInterval).apply {
            setInitialDelay(initialDelay)
            setInputData(payload)
            setConstraints(SyncConstraints)
            addTag(PERIODIC_SYNC_WORK_NAME)
        }.build()
        scope.launch {
            workManager.enqueueUniquePeriodicWork(PERIODIC_SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
        return WorkHandle(id = request.id, uniqueName = PERIODIC_SYNC_WORK_NAME)
    }

    override fun <W : AbstractDataSyncWorker> scheduleDataSyncAt(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode,
        payload: WorkData,
    ): WorkHandle {
        val name = workerClass.requireSimpleName()
        val delay = (instant - Clock.System.now()).coerceAtLeast(Duration.ZERO)
        val uniqueName = "data-sync-at-${instant.toEpochMilliseconds()}"
        val request = OneTimeWorkRequestBuilder<W>(name).apply {
            setInitialDelay(delay)
            setInputData(payload)
            setConstraints(SyncConstraints)
            addTag(uniqueName)
            if (mode == WorkMode.Foreground) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }.build()
        scope.launch { workManager.enqueue(request) }
        return WorkHandle(id = request.id, uniqueName = uniqueName)
    }

    /**
     * Default common-path: delegates to [scheduleDataSyncAt] (flex-window).
     * Per-platform [ExactAlarmScheduler] actual provides the real exact-tier path
     * (AlarmManager on Android, BGProcessingTaskRequest on iOS, ScheduledExecutorService on JVM).
     */
    override fun <W : AbstractDataSyncWorker> scheduleDataSyncAtExact(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode,
        payload: WorkData,
    ): WorkHandle = scheduleDataSyncAt(workerClass, instant, mode, payload)

    override fun observeWork(name: String): Flow<WorkStatus> = workManager.getWorkInfosByTag(name).map { infos ->
        when (infos.firstOrNull()?.state) {
            WorkInfo.State.ENQUEUED -> WorkStatus.Pending
            WorkInfo.State.RUNNING -> WorkStatus.Running
            WorkInfo.State.SUCCEEDED -> WorkStatus.Succeeded
            WorkInfo.State.FAILED -> WorkStatus.Failed
            WorkInfo.State.CANCELLED -> WorkStatus.Cancelled
            WorkInfo.State.BLOCKED -> WorkStatus.Pending
            null -> WorkStatus.Pending
        }
    }

    override fun cancelWork(name: String) {
        scope.launch { workManager.cancelAllWorkByTag(name) }
    }

    private fun KClass<*>.requireSimpleName(): String = simpleName
        ?: error("WorkScheduler requires a named subclass of AbstractDataSyncWorker; got an anonymous class")

    private fun Instant.nextOccurrenceOf(timeOfDay: LocalTime, tz: TimeZone): Instant {
        val today = toLocalDateTime(tz).date
        val todayAtTime = LocalDateTime(today, timeOfDay).toInstant(tz)
        return if (todayAtTime > this) {
            todayAtTime
        } else {
            LocalDateTime(today.plus(1, DateTimeUnit.DAY), timeOfDay).toInstant(tz)
        }
    }
}
