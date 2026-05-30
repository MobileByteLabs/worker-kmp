package io.github.mobilebytelabs.worker.scheduler

/**
 * Platform-specific notification rendering. Actuals land in Phase 3 (Android) and Phase 4 (Desktop/iOS).
 */
expect fun renderNotification(content: NotificationContent)
