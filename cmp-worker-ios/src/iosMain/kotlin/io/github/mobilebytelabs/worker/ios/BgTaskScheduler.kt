@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.mobilebytelabs.worker.ios

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler

// Must be called before applicationDidFinishLaunching returns.
// Safe to call with empty identifier (no-op guard).
internal fun registerBgProcessingTask(identifier: String, scope: CoroutineScope, onTask: suspend () -> Boolean) {
    if (identifier.isEmpty()) return
    BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
        identifier = identifier,
        usingQueue = null,
    ) { bgTask ->
        val task = bgTask as? BGProcessingTask
        if (task == null) {
            bgTask?.setTaskCompletedWithSuccess(false)
            return@registerForTaskWithIdentifier
        }
        val job = scope.launch {
            val success = runCatching { onTask() }.getOrDefault(false)
            task.setTaskCompletedWithSuccess(success)
        }
        task.expirationHandler = {
            job.cancel()
            task.setTaskCompletedWithSuccess(false)
        }
    }
}

// Schedules a BGProcessingTask wake-up for pending constrained work.
// Errors are swallowed — the in-process constraint loop continues as fallback.
internal fun scheduleBgProcessingTask(identifier: String, requiresNetwork: Boolean, requiresCharging: Boolean) {
    if (identifier.isEmpty()) return
    runCatching {
        val request = BGProcessingTaskRequest(identifier = identifier)
        request.requiresNetworkConnectivity = requiresNetwork
        request.requiresExternalPower = requiresCharging
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
    }
}
