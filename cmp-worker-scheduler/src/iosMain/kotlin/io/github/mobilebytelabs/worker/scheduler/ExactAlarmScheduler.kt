@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.time.ExperimentalTime::class)

package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.toNSDate
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import kotlin.time.Instant

/**
 * iOS actual for [ExactAlarmScheduler].
 *
 * ## Best-effort scheduling (iOS does not guarantee exact timing)
 * Uses `BGProcessingTaskRequest` with `earliestBeginDate = instant.toNSDate()`. The OS
 * treats `earliestBeginDate` as a *lower bound* — the task may run minutes to hours later
 * depending on battery / connectivity / app-usage heuristics. For visible exact-time
 * triggers, consumers should use `scheduleNotificationAt` (UNCalendarNotificationTrigger
 * respects the requested instant precisely).
 *
 * ## Info.plist requirement
 * Consumer-app `Info.plist` MUST declare [BG_TASK_ID] in `BGTaskSchedulerPermittedIdentifiers`
 * before app launch. `cmp-worker-ios`'s `InfoPlistValidator` enforces this at startup.
 */
actual class ExactAlarmScheduler actual constructor(private val fallback: WorkScheduler) {
    actual fun scheduleExact(instant: Instant, mode: WorkMode, payload: WorkData): WorkHandle {
        val request = BGProcessingTaskRequest(identifier = BG_TASK_ID).apply {
            earliestBeginDate = instant.toNSDate()
            requiresNetworkConnectivity = true
            requiresExternalPower = (mode == WorkMode.Background)
        }
        val uniqueName = "exact-sync-${instant.toEpochMilliseconds()}"
        return runCatching {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
            WorkHandle(uniqueName)
        }.getOrElse { fallback.scheduleDataSyncAt(instant, mode, payload) }
    }

    companion object {
        const val BG_TASK_ID = "io.github.mobilebytelabs.worker.scheduler.exact_sync"
    }
}
