package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Foreground = setExpedited; Background = default WorkManager scheduling. */
enum class WorkMode { Foreground, Background }

@OptIn(ExperimentalUuidApi::class)
data class WorkHandle(val id: Uuid, val uniqueName: String? = null) {
    constructor(uniqueName: String?) : this(id = Uuid.random(), uniqueName = uniqueName)
}

enum class WorkStatus { Pending, Running, Succeeded, Failed, Cancelled }

/**
 * Koin-injectable façade over worker-kmp's [io.github.mobilebytelabs.worker.WorkManager].
 * Schedules sync work (subclasses of [AbstractDataSyncWorker]) at flex-window, exact-time,
 * periodic, or daily cadences.
 *
 * ## Responsibility split
 *
 * - **Library** = **when** sync work runs (daily 9 AM / every 6 hours / at instant X).
 * - **Consumer** = **what the work does** — your concrete `class AppSyncWorker : AbstractDataSyncWorker(...)`
 *   subclass declares which [io.github.mobilebytelabs.worker.scheduler.sync.Syncable] adopters
 *   fan out in parallel. The library schedules YOUR class by name.
 *
 * ## Typed worker API
 *
 * Every schedule method takes a [KClass]`<W : AbstractDataSyncWorker>` parameter, mirroring
 * `cmp-worker-kmp`'s `oneTimeWorkRequest<W> { ... }` DSL. The reified inline extension
 * functions in this file let you write the type at the call site:
 *
 * ```kotlin
 * scheduler.scheduleDailyDataSync<AppSyncWorker>(timeOfDay = LocalTime(9, 0))
 * scheduler.enqueueDataSync<AppSyncWorker>(payload = workDataOf("currency.base" to "USD"))
 * ```
 *
 * Multi-worker apps (one daily sync + one hourly analytics rollup) just call with different
 * `<W>` — one `WorkScheduler` instance serves all of them. The library reads `W::simpleName`
 * at enqueue time and threads it to `WorkManager` — your consumer registers each worker class
 * with [io.github.mobilebytelabs.worker.registry.WorkerRegistry] exactly once at app start.
 *
 * ## Non-sync workers
 *
 * For workers that are NOT [AbstractDataSyncWorker] subclasses (notifications, custom
 * domain workers), use raw [io.github.mobilebytelabs.worker.WorkManager.enqueue] with
 * `oneTimeWorkRequest<MyWorker> { ... }` directly — this scheduler is sync-only.
 */
@OptIn(ExperimentalTime::class)
interface WorkScheduler {
    /** One-time, immediate sync (or expedited if [WorkMode.Foreground]). */
    fun <W : AbstractDataSyncWorker> enqueueDataSync(
        workerClass: KClass<W>,
        mode: WorkMode = WorkMode.Background,
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /**
     * Periodic — daily at [timeOfDay] in [timeZone].
     * Backed by PeriodicWorkRequest + setInitialDelay(nextOccurrence - now) + 24h repeat,
     * enqueued via `enqueueUniquePeriodicWork(DAILY_SYNC_WORK_NAME, KEEP, ...)`.
     * Flex window ~1-15min (WorkManager-managed; battery-friendly).
     */
    fun <W : AbstractDataSyncWorker> scheduleDailyDataSync(
        workerClass: KClass<W>,
        timeOfDay: LocalTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /**
     * Periodic — repeats every [interval] starting after [initialDelay].
     * Minimum [interval] is 15min per WorkManager rules; smaller values clamp to 15min.
     */
    fun <W : AbstractDataSyncWorker> schedulePeriodicDataSync(
        workerClass: KClass<W>,
        interval: Duration,
        initialDelay: Duration = Duration.ZERO,
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /**
     * One-time sync at [instant]. Uses setInitialDelay(instant - now); subject to
     * WorkManager flex window. Cross-platform.
     */
    fun <W : AbstractDataSyncWorker> scheduleDataSyncAt(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode = WorkMode.Background,
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /**
     * One-time sync at exact [instant] (opt-in exact tier).
     * Android: AlarmManager.setExactAndAllowWhileIdle (needs SCHEDULE_EXACT_ALARM permission).
     * iOS: BGProcessingTaskRequest with earliestBeginDate.
     * Common default: falls back to scheduleDataSyncAt (flex-window).
     */
    fun <W : AbstractDataSyncWorker> scheduleDataSyncAtExact(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode = WorkMode.Background,
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /** Observe live status of work tagged with [name]. Returns a hot Flow. */
    fun observeWork(name: String): Flow<WorkStatus>

    /** Cancel all work tagged with [name]. */
    fun cancelWork(name: String)
}

// ──────────────────────────────────────────────────────────────────────────────
// Reified inline extensions — the consumer-facing call shape.
// ──────────────────────────────────────────────────────────────────────────────

/** Reified: `scheduler.enqueueDataSync<AppSyncWorker>(...)`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.enqueueDataSync(
    mode: WorkMode = WorkMode.Background,
    payload: WorkData = workDataOf(),
): WorkHandle = enqueueDataSync(W::class, mode, payload)

/** Reified: `scheduler.scheduleDailyDataSync<AppSyncWorker>(timeOfDay = LocalTime(9, 0))`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.scheduleDailyDataSync(
    timeOfDay: LocalTime,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    payload: WorkData = workDataOf(),
): WorkHandle = scheduleDailyDataSync(W::class, timeOfDay, timeZone, payload)

/** Reified: `scheduler.schedulePeriodicDataSync<AppSyncWorker>(interval = 6.hours)`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.schedulePeriodicDataSync(
    interval: Duration,
    initialDelay: Duration = Duration.ZERO,
    payload: WorkData = workDataOf(),
): WorkHandle = schedulePeriodicDataSync(W::class, interval, initialDelay, payload)

/** Reified: `scheduler.scheduleDataSyncAt<AppSyncWorker>(instant = meetingStart)`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.scheduleDataSyncAt(
    instant: Instant,
    mode: WorkMode = WorkMode.Background,
    payload: WorkData = workDataOf(),
): WorkHandle = scheduleDataSyncAt(W::class, instant, mode, payload)

/** Reified: `scheduler.scheduleDataSyncAtExact<AppSyncWorker>(instant = paymentDueAt)`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.scheduleDataSyncAtExact(
    instant: Instant,
    mode: WorkMode = WorkMode.Background,
    payload: WorkData = workDataOf(),
): WorkHandle = scheduleDataSyncAtExact(W::class, instant, mode, payload)

// ──────────────────────────────────────────────────────────────────────────────
// Standard tag names — passed to addTag() by impls so observe/cancel by name works.
// Consumers can use these constants OR their own tags when scheduling via raw WorkManager.
// ──────────────────────────────────────────────────────────────────────────────

const val DAILY_SYNC_WORK_NAME = "data-sync-daily"
const val PERIODIC_SYNC_WORK_NAME = "data-sync-periodic"
