# cmp-worker-storeflow

> Worker-anchored Store-flow + submit-outbox bridge for worker-kmp. Adapts patterns from `kmp-project-template/core-base/store` into a WorkManager-scheduled library.

Added in worker-kmp **v3.0.0-alpha03** (Phase 3 of the v3.0.0 epic).

## Coordinates

`io.github.mobilebytelabs:worker-storeflow:3.0.0-alpha03` (Maven Central, once `worker.version` is bumped at release time).

## What ships in alpha03 (scaffold)

| Type | Purpose |
|------|---------|
| `FetchPolicy` | Enum — `CACHE_THEN_NETWORK` / `NETWORK_ONLY` / `CACHE_ONLY`. Lifted verbatim from upstream. |
| `SubmitOutbox<P>` | Interface — offline-resilient mutation queue. |
| `InMemorySubmitOutbox<P>` | Default in-memory implementation; loses state on process death. |
| `OutboxEntry<P>` + `OutboxState` | Outbox row + state enum (`PENDING` / `RETRYING` / `SUBMITTED` / `FAILED`). |
| `WorkScheduledOfflineSubmitSyncer<P, R>` | Periodic + connectivity-gated retry loop, delegated to `WorkManager.enqueueUniquePeriodicWork`. |
| `SyncerWorker` | Internal periodic worker (no-op in alpha03 — wiring lands in alpha03.X). |
| `workStoreFlowKoinModule` | Koin module — provides default `SubmitOutbox<Any>` binding. |

## Quick start

```kotlin
// commonMain — Phase 0 zero-init Koin setup:
startKoin {
    modules(
        workKoinModule(
            config = WorkerConfig(),
            workers = workerRegistry {
                register<SyncerWorker> { ctx -> SyncerWorker(ctx) }
            },
        ),
        workStore5KoinModule,
        workStoreFlowKoinModule,
        appModule,
    )
}

// Enqueue an offline-resilient submission:
val outbox: SubmitOutbox<MyPayload> = get()
val id = outbox.enqueue(MyPayload(...))

// Schedule periodic retries on connectivity restoration:
val syncer = workScheduledOfflineSubmitSyncer(
    workManager = get(),
    outbox = outbox,
    submitBlock = { payload -> api.submitLoanApplication(payload) },
)
syncer.start(interval = 15.minutes, requireNetwork = true)
```

## Deferred features (v3.0.0-alpha03.X follow-ups)

- **Per-platform persistent `SubmitOutbox`** — Room (Android/JVM), SQLDelight (iOS / JS / Wasm).
- **`SyncerWorker.doWork()` wiring** — Koin-resolution-from-CoroutineWorker pattern so the worker can pull the right `SubmitOutbox<P>` + `submitBlock` at runtime.
- **Paging integration** — `StorePagingSource` + `LoadPageRefreshSemantics` mirror of upstream.
- **Compose helpers** — `combineScreenStates` + `produceScreenState` + screen-state mutation extensions.
- **`Validator` + `DecisionEngine`** — freshness-aware refresh skipping.
- **Per-platform retry heuristics** — transient error classifiers (mirror of `StoreBackedWorker.isRetryable`).
- **Real integration tests** — fake outbox + fake WorkManager harness.

## See also

- Master plan: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/PLAN.md`
- Phase 3 sub-plan: `plan-layer/.../03-storeflow-plug.md`
- Upstream source: `kmp-project-template/core-base/store/` (attribution preserved in source headers).
