package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import io.github.mobilebytelabs.worker.scheduler.sync.AbstractDataSyncWorker
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * JS (legacy / Kotlin/JS IR) actual for [ExactAlarmScheduler].
 *
 * Mirrors the wasmJs actual: `setTimeout(handler, delayMs)` for in-tab scheduling.
 * In-tab only — pending timers cancel when the tab closes or the device sleeps.
 *
 * v1.1 roadmap: Service Worker `periodicSync` API for background scheduling.
 */
@OptIn(ExperimentalTime::class)
actual class ExactAlarmScheduler actual constructor(private val fallback: WorkScheduler) {
    actual fun <W : AbstractDataSyncWorker> scheduleExact(
        workerClass: KClass<W>,
        instant: Instant,
        mode: WorkMode,
        payload: WorkData,
    ): WorkHandle {
        val delayMs = (instant.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
            .coerceAtLeast(0)
            .toInt()
        val uniqueName = "exact-sync-${instant.toEpochMilliseconds()}"
        setTimeout({ fallback.enqueueDataSync(workerClass, mode, payload) }, delayMs)
        return WorkHandle(uniqueName)
    }
}

private external fun setTimeout(handler: () -> Unit, delayMs: Int): Int
