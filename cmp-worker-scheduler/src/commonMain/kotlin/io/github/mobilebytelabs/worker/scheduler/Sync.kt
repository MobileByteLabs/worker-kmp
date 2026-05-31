package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker

/**
 * Convenience initializer — enqueues a startup data sync via the injected scheduler.
 *
 * Reified to match the rest of the [WorkScheduler] API; the consumer passes their
 * concrete [AbstractDataSyncWorker] subclass at the call site:
 *
 * ```kotlin
 * Sync.initialize<AppSyncWorker>(scheduler)
 * ```
 */
object Sync {
    inline fun <reified W : AbstractDataSyncWorker> initialize(scheduler: WorkScheduler) {
        scheduler.enqueueDataSync<W>(mode = WorkMode.Background)
    }
}
