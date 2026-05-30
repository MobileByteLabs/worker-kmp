package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.web.showWorkerNotification

/**
 * wasmJs actual for [renderNotification].
 *
 * Delegates to [showWorkerNotification] which is already implemented in
 * `cmp-worker-web`'s wasmJsMain source set and wired via the `api` dependency declared
 * in `cmp-worker-scheduler/build.gradle.kts`. No duplication of JS interop bindings.
 *
 * Permission handling (the browser's Notifications API `requestPermission()` flow) is
 * managed inside `cmp-worker-web` and is unchanged by this delegation.
 */
actual fun renderNotification(content: NotificationContent) {
    showWorkerNotification(
        id = content.channelId ?: "worker-kmp-notification",
        title = content.title,
        body = content.body,
    )
}
