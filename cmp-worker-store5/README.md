# cmp-worker-store5

> Store5 bridge for worker-kmp. Schedules [Mobile Native Foundation Store5](https://github.com/MobileNativeFoundation/Store)-backed refreshes via worker-kmp's WorkManager.

Added in worker-kmp **v3.0.0-alpha02** (Phase 2 of the v3.0.0 epic).

## Coordinates

`io.github.mobilebytelabs:worker-store5:3.0.0-alpha02` (Maven Central, once `worker.version` is bumped at release time).

## Quick start

```kotlin
// commonMain — Phase 0 zero-init Koin setup:
startKoin {
    modules(
        workKoinModule(
            config = WorkerConfig(),
            workers = workerRegistry {
                register<UserProfileSyncWorker> { ctx -> UserProfileSyncWorker(ctx, get()) }
            },
        ),
        workStore5KoinModule,
        appModule,
    )
}

// Schedule a periodic refresh:
val scheduler: StoreRefreshScheduler = get()
scheduler.schedulePeriodicRefresh<UserProfileSyncWorker>(
    interval = 6.hours,
    uniqueWorkName = "user-profile-sync",
    constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build(),
)
```

## Public types

| Type | Purpose |
|------|---------|
| `StoreBackedWorker<K, Output>` | Base class — extend to bind a worker to a `Store.stream(StoreReadRequest.fresh(key))` |
| `StoreRefreshScheduler` | Scheduler — `schedulePeriodicRefresh<T>(...)`, `cancelRefresh(name)`, `observeRefreshes(name)` |
| `workStore5KoinModule` | Koin module — provides `StoreRefreshScheduler` from the upstream `WorkManager` binding |

## Notes

- The `isRetryable()` heuristic on `StoreBackedWorker` defaults to `false` — every error is treated as fatal. Override per-platform to recognise transient errors (`java.io.IOException` on JVM, `NSURLErrorTimedOut` on iOS, `TypeError("Failed to fetch")` on JS).
- The scheduler tags every request with `store5-refresh:<uniqueWorkName>` (exposed as `StoreRefreshScheduler.TAG_PREFIX`) so consumers can use `workManager.getWorkInfosByTag(...)` outside the scheduler API.
- Store5 5.1.0-alpha06 is the version we pin (full KMP target matrix coverage including wasmJs).

## Deferred features (v3.0.0-alpha02.X follow-ups)

- `MutableStoreSyncWorker` — write-side outbox flush via `MutableStore.write()`
- `StoreFreshnessWorker` — skip-fetch when `Validator` says the cached value is still fresh
- Per-platform retry heuristics (provided actuals for JVM / iOS / JS / WasmJs)
- Real integration tests against a `FakeStore` harness

## See also

- Master plan: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/PLAN.md`
- Phase 2 sub-plan: `plan-layer/.../02-store5-bridge.md`
