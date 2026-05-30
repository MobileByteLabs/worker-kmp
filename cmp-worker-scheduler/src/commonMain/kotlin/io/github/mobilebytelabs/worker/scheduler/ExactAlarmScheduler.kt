package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Platform-specific exact-alarm scheduling.
 * Android actual: AlarmManager.setExactAndAllowWhileIdle (requires SCHEDULE_EXACT_ALARM permission).
 * iOS actual: BGProcessingTaskRequest with earliestBeginDate.
 * Desktop (JVM) actual: ScheduledExecutorService (in-process).
 * wasmJs actual: setTimeout (in-tab).
 */
@OptIn(ExperimentalTime::class)
expect class ExactAlarmScheduler(fallback: WorkScheduler) {
    fun scheduleExact(instant: Instant, mode: WorkMode, payload: WorkData): WorkHandle
}
