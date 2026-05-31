package io.github.mobilebytelabs.worker.scheduler

import io.github.mobilebytelabs.worker.WorkData
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * wasmJs actual for [ExactAlarmScheduler].
 *
 * **v1 limitation — in-tab only.** Uses `setTimeout(handler, delayMs)` which fires only
 * while the tab is active. Navigating away / closing the tab / device suspension cancels
 * pending timers. Not persisted across page reloads.
 *
 * **v1.1 roadmap:** Service Worker `periodicSync` API (Chrome 80+) for background scheduling
 * — deferred from v1 due to bundler-config (sw.js registration) coupling.
 */
@OptIn(ExperimentalTime::class)
actual class ExactAlarmScheduler actual constructor(private val fallback: WorkScheduler) {
    actual fun scheduleExact(instant: Instant, mode: WorkMode, payload: WorkData): WorkHandle {
        val delayMs = (instant.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
            .coerceAtLeast(0)
            .toInt()
        val uniqueName = "exact-sync-${instant.toEpochMilliseconds()}"
        jsSetTimeout(handler = { fallback.enqueueDataSync(mode, payload) }, delayMs = delayMs)
        return WorkHandle(uniqueName)
    }
}

@JsFun("(handler, delayMs) => setTimeout(handler, delayMs)")
private external fun jsSetTimeout(handler: () -> Unit, delayMs: Int)
