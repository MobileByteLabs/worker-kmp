package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.OneTimeWorkRequestBuilder
import io.github.mobilebytelabs.worker.PeriodicWorkRequestBuilder
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
 * - **Library** = **when** sync work runs (daily 9 AM / every 6 hours / at instant X) +
 *   sensible defaults for constraints/backoff/tags.
 * - **Consumer** = **what the work does** (concrete [AbstractDataSyncWorker] subclass)
 *   + any per-call configuration tweaks via the `configure` lambda.
 *
 * ## Typed worker API
 *
 * Every schedule method takes a [KClass]`<W : AbstractDataSyncWorker>` parameter so the
 * library knows which concrete subclass to enqueue. Reified inline extensions in this file
 * let consumers write the type at the call site:
 *
 * ```kotlin
 * scheduler.scheduleDailyDataSync<AppSyncWorker>(timeOfDay = LocalTime(9, 0))
 * scheduler.enqueueDataSync<AppSyncWorker>(payload = workDataOf("currency.base" to "USD"))
 * ```
 *
 * ## Builder-block configuration
 *
 * Every schedule method also takes a `configure` lambda — a receiver over
 * [OneTimeWorkRequestBuilder]`<W>` or [PeriodicWorkRequestBuilder]`<W>` so consumers can
 * override constraints, backoff, tags, expedited policy, etc.:
 *
 * ```kotlin
 * scheduler.scheduleDailyDataSync<AppSyncWorker>(timeOfDay = LocalTime(9, 0)) {
 *     setConstraints(Constraints { setRequiredNetworkType(NetworkType.UNMETERED) })
 *     setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RetryConfig.DEFAULT)
 *     addTag("morning-refresh")
 * }
 * ```
 *
 * The library applies its defaults (e.g. [SyncConstraints], standard tags, foreground
 * expedited mapping) FIRST, then invokes the `configure` lambda — so any consumer override
 * wins. The lambda runs on the actual builder instance returned by
 * `oneTimeWorkRequest<W> { ... }` / `periodicWorkRequest<W>(interval) { ... }`.
 *
 * ## Custom WorkScheduler implementations
 *
 * [DefaultWorkScheduler] is `open` — subclass it to override individual methods (e.g. add
 * project-wide telemetry) or implement [WorkScheduler] from scratch for fully custom behavior.
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
        configure: OneTimeWorkRequestBuilder<W>.() -> Unit = {},
    ): WorkHandle

    /**
     * Periodic — daily at [timeOfDay] in [timeZone].
     * Backed by PeriodicWorkRequest + setInitialDelay(nextOccurrence - now) + 24h repeat,
     * enqueued via `enqueueUniquePeriodicWork(DAILY_SYNC_WORK_NAME, KEEP, ...)`.
     */
    fun <W : AbstractDataSyncWorker> scheduleDailyDataSync(
        workerClass: KClass<W>,
        timeOfDay: LocalTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        payload: WorkData = workDataOf(),
        configure: PeriodicWorkRequestBuilder<W>.() -> Unit = {},
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
        configure: PeriodicWorkRequestBuilder<W>.() -> Unit = {},
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
        configure: OneTimeWorkRequestBuilder<W>.() -> Unit = {},
    ): WorkHandle

    /**
     * One-time sync at exact [instant] (opt-in exact tier).
     * Android: AlarmManager.setExactAndAllowWhileIdle (needs SCHEDULE_EXACT_ALARM permission).
     * iOS: BGProcessingTaskRequest with earliestBeginDate.
     * Common default: falls back to [scheduleDataSyncAt] (flex-window).
     */
    fun <W : AbstractDataSyncWorker> scheduleDataSyncAtExact(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode = WorkMode.Background,
        payload: WorkData = workDataOf(),
        configure: OneTimeWorkRequestBuilder<W>.() -> Unit = {},
    ): WorkHandle

    /** Observe live status of work tagged with [name]. Returns a hot Flow. */
    fun observeWork(name: String): Flow<WorkStatus>

    /** Cancel all work tagged with [name]. */
    fun cancelWork(name: String)
}

// ──────────────────────────────────────────────────────────────────────────────
// Reified inline extensions — the consumer-facing call shape.
// ──────────────────────────────────────────────────────────────────────────────

/** Reified: `scheduler.enqueueDataSync<AppSyncWorker>(...) { addTag("x") }`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.enqueueDataSync(
    mode: WorkMode = WorkMode.Background,
    payload: WorkData = workDataOf(),
    noinline configure: OneTimeWorkRequestBuilder<W>.() -> Unit = {},
): WorkHandle = enqueueDataSync(W::class, mode, payload, configure)

/** Reified: `scheduler.scheduleDailyDataSync<AppSyncWorker>(LocalTime(9, 0)) { ... }`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.scheduleDailyDataSync(
    timeOfDay: LocalTime,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    payload: WorkData = workDataOf(),
    noinline configure: PeriodicWorkRequestBuilder<W>.() -> Unit = {},
): WorkHandle = scheduleDailyDataSync(W::class, timeOfDay, timeZone, payload, configure)

/** Reified: `scheduler.schedulePeriodicDataSync<AppSyncWorker>(6.hours) { ... }`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.schedulePeriodicDataSync(
    interval: Duration,
    initialDelay: Duration = Duration.ZERO,
    payload: WorkData = workDataOf(),
    noinline configure: PeriodicWorkRequestBuilder<W>.() -> Unit = {},
): WorkHandle = schedulePeriodicDataSync(W::class, interval, initialDelay, payload, configure)

/** Reified: `scheduler.scheduleDataSyncAt<AppSyncWorker>(meetingStart) { ... }`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.scheduleDataSyncAt(
    instant: Instant,
    mode: WorkMode = WorkMode.Background,
    payload: WorkData = workDataOf(),
    noinline configure: OneTimeWorkRequestBuilder<W>.() -> Unit = {},
): WorkHandle = scheduleDataSyncAt(W::class, instant, mode, payload, configure)

/** Reified: `scheduler.scheduleDataSyncAtExact<AppSyncWorker>(paymentDueAt) { ... }`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> WorkScheduler.scheduleDataSyncAtExact(
    instant: Instant,
    mode: WorkMode = WorkMode.Background,
    payload: WorkData = workDataOf(),
    noinline configure: OneTimeWorkRequestBuilder<W>.() -> Unit = {},
): WorkHandle = scheduleDataSyncAtExact(W::class, instant, mode, payload, configure)

// ──────────────────────────────────────────────────────────────────────────────
// Standard tag names — passed to addTag() by impls so observe/cancel by name works.
// ──────────────────────────────────────────────────────────────────────────────

const val DAILY_SYNC_WORK_NAME = "data-sync-daily"
const val PERIODIC_SYNC_WORK_NAME = "data-sync-periodic"
