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

## Added in alpha03.X (Phase 3 extension)

| Type | Purpose |
|------|---------|
| `DraftSubmitHandler<P, R>` | Persistent draft state machine surviving process restarts (Idle → Drafting → Submitting → Submitted/Failed). |
| `PrefetchPagingWorker` | Abstract `CoroutineWorker` for paginated cache warming — consumer extends + implements `fetchPage(pageNumber)`. |
| `SubmitStateUi` + `SubmitStateUiModel` | Compose helper enum + UI model (in `cmp-worker-compose` to avoid storeflow→Compose hard dep). |

## Advanced patterns (alpha03.X)

### DraftSubmitHandler — persistent draft state across restarts

```kotlin
val handler = DraftSubmitHandler(roomBackedOutbox) { payload -> api.submitLoan(payload) }
handler.rehydrateFromOutbox()   // On Activity/ViewModel restart

handler.draft(LoanApplication(amount = 50_000, term = 36))
val result: DraftSubmitHandler.State<LoanApplication> = handler.submit()
when (result) {
    is DraftSubmitHandler.State.Submitted -> { /* success */ }
    is DraftSubmitHandler.State.Failed    -> { /* retry */ }
    else                                  -> { /* still drafting / submitting */ }
}
```

`DraftSubmitHandler` enqueues every submit through the [SubmitOutbox] so a process death
mid-submit survives. `rehydrateFromOutbox()` restores the most-recently-enqueued PENDING/RETRYING
entry to `State.Submitting` so the UI can offer a "retry / cancel" affordance on relaunch.

### PrefetchPagingWorker — paginated cache warmer

```kotlin
class FeedPrefetchWorker(
    context: WorkerContext,
    private val feedRepo: FeedRepository,
) : PrefetchPagingWorker(context) {
    override suspend fun fetchPage(pageNumber: Int) {
        feedRepo.loadAndCache(pageNumber)
    }
}

// Schedule:
workManager.enqueue(OneTimeWorkRequestBuilder<FeedPrefetchWorker>().apply {
    setInputData(workDataOf(
        PrefetchPagingWorker.KEY_START_PAGE to currentPage + 1,
        PrefetchPagingWorker.KEY_PAGE_COUNT to 5,
    ))
}.build())
```

Page-fetch failures map to [WorkResult.retry] — the scheduler retries per
`RetryConfig`. Output WorkData carries [KEY_PAGES_FETCHED] for observability.

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
