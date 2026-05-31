package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Platform-specific exact-alarm scheduling primitive.
 *
 * - Android actual: AlarmManager.setExactAndAllowWhileIdle (requires SCHEDULE_EXACT_ALARM permission).
 * - iOS actual: BGProcessingTaskRequest with earliestBeginDate.
 * - Desktop (JVM) actual: ScheduledExecutorService (in-process).
 * - wasmJs / js actuals: setTimeout (in-tab).
 *
 * **Note:** [DefaultWorkScheduler.scheduleDataSyncAtExact] currently delegates to
 * `scheduleDataSyncAt` (flex-window) — this class is opt-in for consumers who need
 * the per-platform exact-tier path directly. A future release will wire the Android
 * actual into `DefaultWorkScheduler.scheduleDataSyncAtExact` automatically.
 */
@OptIn(ExperimentalTime::class)
expect class ExactAlarmScheduler(fallback: WorkScheduler) {
    fun <W : AbstractDataSyncWorker> scheduleExact(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode,
        payload: WorkData,
    ): WorkHandle
}

/** Reified call shape: `exactAlarmScheduler.scheduleExact<AppSyncWorker>(instant, mode, payload)`. */
@OptIn(ExperimentalTime::class)
inline fun <reified W : AbstractDataSyncWorker> ExactAlarmScheduler.scheduleExact(
    instant: Instant,
    mode: WorkMode = WorkMode.Background,
    payload: WorkData = io.github.mobilebytelabs.worker.workDataOf(),
): WorkHandle = scheduleExact(W::class, instant, mode, payload)
