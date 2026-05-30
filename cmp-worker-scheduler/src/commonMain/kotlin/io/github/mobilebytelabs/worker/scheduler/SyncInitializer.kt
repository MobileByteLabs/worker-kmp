package io.github.mobilebytelabs.worker.scheduler

/** Convenience initializer — enqueues a startup data sync via the injected scheduler. */
object Sync {
    fun initialize(scheduler: WorkScheduler) {
        scheduler.enqueueDataSync(mode = WorkMode.Background)
    }
}
