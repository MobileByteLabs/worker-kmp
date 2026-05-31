package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
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
 * Koin-injectable façade over worker-kmp's WorkManager. Schedules data-sync work
 * (via [AbstractDataSyncWorker]) at flex-window, exact-time, periodic, or daily cadences.
 *
 * Library responsibility = **when work runs**. Consumer responsibility = **what the work
 * does** — extend [AbstractDataSyncWorker] (sync via Synchronizer/Syncable) for sync,
 * or use raw [io.github.mobilebytelabs.worker.WorkManager.enqueue] for any other Worker class
 * (e.g. notification rendering).
 *
 * Usage:
 *   class MyUseCase(private val scheduler: WorkScheduler) {
 *       fun installDailyRefresh() {
 *           scheduler.scheduleDailyDataSync(timeOfDay = LocalTime(9, 0))
 *       }
 *   }
 */
@OptIn(ExperimentalTime::class)
interface WorkScheduler {
    /** One-time, immediate sync (or expedited if WorkMode.Foreground). */
    fun enqueueDataSync(mode: WorkMode = WorkMode.Background, payload: WorkData = workDataOf()): WorkHandle

    /**
     * Periodic — daily at [timeOfDay] in [timeZone].
     * Uses PeriodicWorkRequest + setInitialDelay(nextOccurrence - now) + setRepeatInterval(24h),
     * enqueued via enqueueUniquePeriodicWork(DAILY_SYNC_WORK_NAME, KEEP, ...).
     * Flex window ~1-15min (WorkManager-managed; battery-friendly).
     */
    fun scheduleDailyDataSync(
        timeOfDay: LocalTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /**
     * Periodic — repeats every [interval] starting after [initialDelay].
     * Minimum [interval] is 15min per WorkManager rules; smaller values clamp to 15min.
     */
    fun schedulePeriodicDataSync(
        interval: Duration,
        initialDelay: Duration = Duration.ZERO,
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /**
     * One-time sync at [instant]. Uses setInitialDelay(instant - now); subject to
     * WorkManager flex window. Cross-platform.
     */
    fun scheduleDataSyncAt(
        instant: Instant,
        mode: WorkMode = WorkMode.Background,
        payload: WorkData = workDataOf(),
    ): WorkHandle

    /**
     * One-time sync at exact [instant] (opt-in exact tier).
     * Android: AlarmManager.setExactAndAllowWhileIdle (needs SCHEDULE_EXACT_ALARM permission).
     * Common default: falls back to scheduleDataSyncAt (flex-window).
     */
    fun scheduleDataSyncAtExact(
        instant: Instant,
        mode: WorkMode = WorkMode.Background,
        payload: WorkData = workDataOf(),
    ): WorkHandle

    fun observeWork(name: String): Flow<WorkStatus>
    fun cancelWork(name: String)
}

const val DAILY_SYNC_WORK_NAME = "data-sync-daily"
const val PERIODIC_SYNC_WORK_NAME = "data-sync-periodic"
