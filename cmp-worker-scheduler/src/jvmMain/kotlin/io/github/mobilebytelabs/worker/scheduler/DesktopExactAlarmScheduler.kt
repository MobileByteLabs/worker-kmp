package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Desktop (JVM) actual for [ExactAlarmScheduler] — uses [ScheduledExecutorService] to fire
 * at the requested instant.
 *
 * **v1 limitation:** in-process only. If the JVM exits, pending alarms are lost. A future
 * v1.1 follow-up will layer this on `cmp-worker-desktop-daemon` (Windows Task Scheduler /
 * launchctl / systemd) once that module's daemon API gains execute-capability.
 */
@OptIn(ExperimentalTime::class)
actual class ExactAlarmScheduler actual constructor(
    private val fallback: WorkScheduler,
) {
    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "worker-kmp-desktop-exact-alarm").also { it.isDaemon = true }
        }

    actual fun scheduleExact(instant: Instant, mode: WorkMode, payload: WorkData): WorkHandle {
        val delayMs = (instant.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
            .coerceAtLeast(0)
        val uniqueName = "exact-sync-${instant.toEpochMilliseconds()}"
        executor.schedule(
            { fallback.enqueueDataSync(mode, payload) },
            delayMs,
            TimeUnit.MILLISECONDS,
        )
        return WorkHandle(uniqueName)
    }

    /** Shuts down the underlying executor. Call from application teardown. */
    fun shutdown() {
        executor.shutdown()
    }
}
