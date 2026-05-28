package io.github.mobilebytelabs.worker.observer

import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.worker.WorkEvent
import io.github.mobilebytelabs.worker.WorkObserver
import io.github.mobilebytelabs.worker.WorkResult

/**
 * Out-of-box [WorkObserver] that logs every lifecycle event via [Kermit Logger][co.touchlab.kermit.Logger].
 *
 * Structured log format (single-line per event):
 *   `worker-kmp <event> id=<uuid> <event-specific-fields>`
 *
 * Levels:
 * - INFO  — Enqueued, Started, Resulted (success)
 * - WARN  — Resulted (failure / retry-exhausted)
 * - DEBUG — Progress (high-volume; sample if needed in production)
 *
 * Consumers can swap [Logger] for OpenTelemetry / Sentry / Firebase Performance by
 * writing their own [WorkObserver] — see docs/features/observers.md for bridge patterns.
 *
 * @property tag Kermit logger tag; defaults to `worker-kmp`.
 *
 * Added in v2.2.0.
 */
public class LoggingWorkObserver(private val tag: String = "worker-kmp") : WorkObserver {
    override suspend fun onEvent(event: WorkEvent) {
        val logger = Logger.withTag(tag)
        when (event) {
            is WorkEvent.Enqueued -> logger.i { "enqueued id=${event.id} tag=${event.tag ?: "<none>"}" }

            is WorkEvent.Started -> logger.i { "started  id=${event.id} attempt=${event.attemptCount}" }

            is WorkEvent.Progress -> logger.d { "progress id=${event.id} pct=${event.progress.progress}" }

            is WorkEvent.Resulted -> {
                when (val result = event.result) {
                    is WorkResult.Success -> logger.i { "succeeded id=${event.id} durationMs=${event.durationMs}" }

                    is WorkResult.Failure -> logger.w {
                        val reason = result.message.ifBlank { "<none>" }
                        "failed   id=${event.id} durationMs=${event.durationMs} reason=$reason"
                    }

                    is WorkResult.Retry -> logger.w { "retry    id=${event.id} durationMs=${event.durationMs}" }
                }
            }
        }
    }
}
