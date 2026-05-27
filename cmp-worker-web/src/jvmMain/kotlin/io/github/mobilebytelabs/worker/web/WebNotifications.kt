package io.github.mobilebytelabs.worker.web

import co.touchlab.kermit.Logger

// JVM target exists only for test compilation — no browser context available.
public actual suspend fun requestNotificationPermission(): NotificationPermission =
    NotificationPermission.DEFAULT

public actual fun showWorkerNotification(id: String, title: String, body: String, progress: Int?) {
    Logger.withTag("worker-kmp.web.notifications").d {
        "showWorkerNotification(id=$id, title=$title, progress=$progress) on JVM — no-op."
    }
}
