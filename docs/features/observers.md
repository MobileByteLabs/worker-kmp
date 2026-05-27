# Observers

> Production telemetry hooks for worker-kmp. v2.2.0 introduces the `WorkObserver`
> SAM interface + 4-event sealed class + a ship-in-core `LoggingWorkObserver`.
> Consumers wire OpenTelemetry / Sentry / Firebase Performance bridges by
> implementing `WorkObserver` themselves.

## The SPI

```kotlin
public fun interface WorkObserver {
    public suspend fun onEvent(event: WorkEvent)
}

public sealed class WorkEvent {
    data class Enqueued(id: Uuid, tag: String?, inputData: WorkData) : WorkEvent()
    data class Started(id: Uuid, attemptCount: Int) : WorkEvent()
    data class Progress(id: Uuid, progress: WorkProgress) : WorkEvent()
    data class Resulted(id: Uuid, result: WorkResult, durationMs: Long) : WorkEvent()
}
```

## Out-of-box: LoggingWorkObserver

Ships in `cmp-worker-kmp` core. Uses Kermit (cross-platform KMP logger) for structured single-line logs.

```kotlin
// Register at platform init (per-platform actual wiring lands in Phases 1/7/8/9)
val observer = LoggingWorkObserver(tag = "MyApp.worker")
// observer registration lands as platform-actual init param: observers = listOf(observer)
```

Log levels:

| Event | Level | Single-line shape |
|---|---|---|
| Enqueued | INFO | `worker-kmp enqueued id=<uuid> tag=<tag\|<none>>` |
| Started | INFO | `worker-kmp started  id=<uuid> attempt=<n>` |
| Progress | DEBUG | `worker-kmp progress id=<uuid> pct=<0-100>` |
| Resulted (Success) | INFO | `worker-kmp succeeded id=<uuid> durationMs=<n>` |
| Resulted (Failure) | WARN | `worker-kmp failed    id=<uuid> durationMs=<n> reason=<msg\|<none>>` |
| Resulted (Retry) | WARN | `worker-kmp retry    id=<uuid> durationMs=<n>` |

## Bridge patterns

### OpenTelemetry

```kotlin
class OtelWorkObserver(private val tracer: Tracer) : WorkObserver {
    private val activeSpans = mutableMapOf<Uuid, Span>()
    override suspend fun onEvent(event: WorkEvent) = when (event) {
        is WorkEvent.Enqueued ->
            activeSpans[event.id] = tracer.spanBuilder("worker.enqueue")
                .setAttribute("worker.id", event.id.toString())
                .setAttribute("worker.tag", event.tag ?: "none")
                .startSpan()
        is WorkEvent.Started ->
            activeSpans[event.id]?.addEvent("started")
                ?.setAttribute("worker.attempt", event.attemptCount.toLong())
        is WorkEvent.Progress ->
            activeSpans[event.id]?.addEvent("progress")
                ?.setAttribute("worker.progress.pct", event.progress.progress.toLong())
        is WorkEvent.Resulted -> {
            activeSpans[event.id]?.setStatus(
                if (event.result is WorkResult.Success) StatusCode.OK else StatusCode.ERROR
            )?.end()
            activeSpans.remove(event.id)
            Unit
        }
    }
}
```

### Sentry

```kotlin
class SentryWorkObserver : WorkObserver {
    override suspend fun onEvent(event: WorkEvent) = when (event) {
        is WorkEvent.Resulted -> when (event.result) {
            is WorkResult.Failure -> Sentry.captureMessage(
                "worker-kmp failed id=${event.id} reason=${(event.result as WorkResult.Failure).message}",
                SentryLevel.WARNING,
            )
            else -> Unit
        }
        else -> Unit // Sentry only cares about failures
    }
}
```

### Firebase Performance

Similar pattern — start a Performance trace on `Started`, stop on `Resulted`. See Phase 16 (`cmp-worker-firebase-perf` bridge artifact) — deferred to v3.1+ backlog.

## Per-platform actual wiring

In v2.2.0, `WorkObserver` is a pure SPI interface — no platform actual emits events yet. Each platform's actual gains observer wiring as its surfaces land:

- Android (`cmp-worker-android`): observers emit from `KmpAndroidWorker.doWork()` lifecycle — Phase 1
- iOS (`cmp-worker-ios`): from `IosWorkManager` state-store transitions — Phase 1
- Desktop (`cmp-worker-desktop`): from `DesktopWorkManager.executeWork()` — Phase 1
- Web (`cmp-worker-web`): from `WebWorkManager.runOne()` (main + SW) — Phase 1 + Phase 9
- Desktop daemon (`cmp-worker-desktop-daemon`): from daemon RotatingLogger handoff — Phase 8

Until those land, the SPI is callable but no events fire from production paths. `TestWorkManager` records events synchronously for test assertions.

## Testing your observer

```kotlin
val recorded = mutableListOf<WorkEvent>()
val tester = TestWorkManager() // already records to observedEvents internally too
val observer = WorkObserver { recorded += it }

// emit synthetic events via TestWorkManager.simulateSuccess(...) etc.
tester.simulateSuccess(id, workDataOf("out" to "ok"))
assertEquals(2, recorded.size) // Started + Resulted
```

## Cross-references

- Phase 4 sub-plan: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/04-capability-matrix-and-telemetry.md`
- Master plan: same dir, `PLAN.md`
- Future bridges (cmp-worker-otel, cmp-worker-sentry, cmp-worker-firebase-perf): v3.1+ backlog
