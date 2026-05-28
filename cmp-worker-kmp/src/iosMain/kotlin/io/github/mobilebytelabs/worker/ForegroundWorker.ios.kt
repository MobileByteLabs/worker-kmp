@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
    ExperimentalForegroundApi::class,
)

package io.github.mobilebytelabs.worker

import co.touchlab.kermit.Logger
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.dateByAddingTimeInterval
import platform.UIKit.UIDevice
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS actual for [runAsForeground].
 *
 * iOS does not allow apps to "promote" background work to foreground the way Android does.
 * The closest analogues are:
 * - **iOS 17+**: `BGContinuedProcessingTaskRequest` — explicit user-visible long-running
 *   task with progress reporting via `BGContinuedProcessingTaskUpdate`. Currently the
 *   Kotlin/Native platform bindings do NOT expose `BGContinuedProcessingTaskRequest`
 *   directly (it ships in iOS 17 SDK; binding lands in a future Kotlin/Native release).
 *   Until the binding ships we fall back to the 13-16 shim path even on iOS 17+ —
 *   functionally equivalent for the consumer (BGProcessingTask + visible notification).
 * - **iOS 13-16**: schedule a `BGProcessingTaskRequest` (to extend lifetime) + post a
 *   `UNNotification` so the user knows what's happening.
 *
 * Replaces the log-only stub from alpha01.
 *
 * Added in v3.0.0-alpha01.X (Phase 1 deep impl).
 *
 * **Known limitation**: `BGContinuedProcessingTaskRequest` (iOS 17+) is not yet bound in
 * Kotlin/Native platform-libs — we route iOS 17+ consumers through the BGProcessingTask
 * + UNNotification shim until the binding ships. Tracked for alpha01.X.1 follow-up.
 */
@ExperimentalForegroundApi
public actual suspend fun runAsForeground(worker: ForegroundWorker, info: ForegroundInfo) {
    val majorVersion = parseMajorIosVersion(UIDevice.currentDevice.systemVersion)
    if (majorVersion >= IOS_17) {
        Logger.withTag("worker-kmp.foreground.ios").d {
            "iOS $majorVersion detected; BGContinuedProcessingTaskRequest binding not yet in K/N — " +
                "falling back to BGProcessingTask + UNNotification shim. id=${info.notificationId}"
        }
    }
    scheduleProcessingWithNotification(info)
}

private const val IOS_17 = 17

internal fun parseMajorIosVersion(systemVersion: String): Int = systemVersion.substringBefore('.').toIntOrNull() ?: 0

private fun scheduleProcessingWithNotification(info: ForegroundInfo) {
    val identifier = "worker-kmp.foreground.${info.notificationId}"
    runCatching {
        val request = BGProcessingTaskRequest(identifier = identifier)
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
    }.onFailure { t ->
        Logger.withTag("worker-kmp.foreground.ios").w(t) {
            "BGProcessingTaskRequest submit failed for id=$identifier — notification shown anyway."
        }
    }
    postUserNotification(identifier, info)
}

private fun postUserNotification(identifier: String, info: ForegroundInfo) {
    runCatching {
        val content = UNMutableNotificationContent()
        content.setTitle(info.title)
        content.setBody("${info.message} (${info.progress.progress}%)")
        val notifRequest = UNNotificationRequest.requestWithIdentifier(
            identifier = "$identifier.notif",
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(notifRequest, withCompletionHandler = null)
    }.onFailure { t ->
        Logger.withTag("worker-kmp.foreground.ios").w(t) {
            "UNNotificationCenter.addNotificationRequest failed for id=$identifier"
        }
    }
}

// Suppressed — referenced only via the deferred BGContinuedProcessingTaskRequest path,
// retained as scaffolding for the alpha01.X.1 follow-up.
@Suppress("UnusedPrivateMember", "unused")
private fun nowPlus(seconds: Double): NSDate = NSDate().dateByAddingTimeInterval(seconds)

@Suppress("UnusedPrivateMember", "unused")
private fun emptyDateComponents(): NSDateComponents = NSDateComponents()
