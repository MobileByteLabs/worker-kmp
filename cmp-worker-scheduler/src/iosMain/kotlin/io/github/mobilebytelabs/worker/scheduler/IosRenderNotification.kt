@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.mobilebytelabs.worker.scheduler

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Tracks whether notification authorization has been requested this process lifetime.
 *
 * Authorization check + write is dispatched on the main queue (see below), so we don't
 * need cross-thread visibility guarantees on this flag.
 */
private var authorizationRequested = false

/**
 * iOS actual for [renderNotification].
 *
 * ## Authorization
 * On the first call, checks `UNUserNotificationCenter` authorization status.
 * If `.notDetermined`, calls `requestAuthorizationWithOptions([.alert, .sound])`
 * asynchronously. Subsequent calls skip the authorization check via the
 * in-memory [authorizationRequested] flag (process-lifetime scope).
 *
 * ## Info.plist requirement
 * The host app's `Info.plist` must include
 * `NSUserNotificationsUsageDescription` with a human-readable reason string,
 * otherwise the system will silently refuse to show the notification prompt.
 *
 * ## Threading
 * `addNotificationRequest` is dispatched on the main queue per Apple guidance.
 *
 * ## Caveat
 * User can deny authorization — subsequent `addNotificationRequest` calls
 * will silently fail (no error is surfaced to the caller by design; the
 * background sync completed successfully even if the notification was not shown).
 */
actual fun renderNotification(content: NotificationContent) {
    val center = UNUserNotificationCenter.currentNotificationCenter()

    if (!authorizationRequested) {
        authorizationRequested = true
        center.getNotificationSettingsWithCompletionHandler { settings ->
            if (settings?.authorizationStatus == UNAuthorizationStatusNotDetermined) {
                center.requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
                ) { granted, error ->
                    // Errors logged only — no retry. User can grant via Settings at any time.
                    if (error != null) {
                        println("[worker-scheduler] requestAuthorization error: ${error.localizedDescription}")
                    } else {
                        println("[worker-scheduler] Notification authorization granted=$granted")
                    }
                }
            }
        }
    }

    val notificationContent = UNMutableNotificationContent().apply {
        setTitle(content.title)
        setBody(content.body)
    }

    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = NSUUID().UUIDString,
        content = notificationContent,
        trigger = null, // deliver immediately
    )

    dispatch_async(dispatch_get_main_queue()) {
        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("[worker-scheduler] addNotificationRequest error: ${error.localizedDescription}")
            }
        }
    }
}
