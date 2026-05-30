package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext

/** Reads NotificationContent from inputData and delegates to expect/actual renderNotification. */
class NotificationWorker(ctx: WorkerContext) : CoroutineWorker(ctx) {

    override suspend fun doWork(): WorkResult {
        val title = inputData.getString("title") ?: return WorkResult.failure("missing title")
        val body = inputData.getString("body") ?: return WorkResult.failure("missing body")
        val channelId = inputData.getString("channelId")?.takeIf { it.isNotEmpty() }
        val content = NotificationContent(title, body, channelId)
        return try {
            renderNotification(content)
            WorkResult.success()
        } catch (t: Throwable) {
            WorkResult.failure(t.message ?: t::class.simpleName ?: "render failed")
        }
    }
}
