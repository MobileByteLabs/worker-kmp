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
| `MutableStoreSyncWorker<K, V>` | Base class — push pending mutations via `MutableStore.write(...)` (alpha02.X) |
| `StoreFreshnessWorker<K, Output>` | Base class — skip fetch when `Validator` says cache is still fresh (alpha02.X) |
| `StoreRefreshScheduler` | Scheduler — `schedulePeriodicRefresh<T>(...)`, `cancelRefresh(name)`, `observeRefreshes(name)` |
| `workStore5KoinModule` | Koin module — provides `StoreRefreshScheduler` from the upstream `WorkManager` binding |

## Advanced workers (alpha02.X)

### MutableStoreSyncWorker — push mutations on connectivity

```kotlin
class ProfileEditSyncWorker(
    context: WorkerContext,
    store: MutableStore<UserId, UserProfile>,
    userId: UserId,
    edits: UserProfile,
) : MutableStoreSyncWorker<UserId, UserProfile>(context, store, userId, edits)
```

Invokes `mutableStore.write(StoreWriteRequest.of(key, value))` which triggers the
configured `Updater` + `Bookkeeper`. Override `mapWriteResponseToWorkData(response)`
to project the typed/untyped `Success` payload into worker output. `MutableStore` is
annotated `@ExperimentalStoreApi` upstream — opt-in propagates via the class-level
`@OptIn`.

### StoreFreshnessWorker — skip fetch when cache is fresh

```kotlin
class UserProfileFreshnessWorker(
    context: WorkerContext,
    store: Store<UserId, UserProfile>,
    userId: UserId,
    validator: Validator<UserProfile> = Validator.by { it.fetchedAt > now - 6.hours },
) : StoreFreshnessWorker<UserId, UserProfile>(context, store, userId, validator)
```

Reads via `StoreReadRequest.cached(key, refresh = false)` then runs `Validator.isValid(...)`.
If valid: returns `WorkResult.success(workDataOf("skipped" to "fresh"))` — observers can
key off `StoreFreshnessWorker.KEY_SKIPPED` to count bandwidth-saved invocations. Otherwise:
same `store.stream(StoreReadRequest.fresh(key))` path as `StoreBackedWorker`.

## Notes

- The `isRetryable()` heuristic on `StoreBackedWorker` defaults to `false` — every error is treated as fatal. Override per-platform to recognise transient errors (`java.io.IOException` on JVM, `NSURLErrorTimedOut` on iOS, `TypeError("Failed to fetch")` on JS).
- The scheduler tags every request with `store5-refresh:<uniqueWorkName>` (exposed as `StoreRefreshScheduler.TAG_PREFIX`) so consumers can use `workManager.getWorkInfosByTag(...)` outside the scheduler API.
- Store5 5.1.0-alpha06 is the version we pin (full KMP target matrix coverage including wasmJs).

## Deferred features (v3.0.0-alpha02.X.Y follow-ups)

- Per-platform retry heuristics (provided actuals for JVM / iOS / JS / WasmJs)
- Real integration tests against a `FakeStore` harness
- Multi-key `MutableStoreSyncWorker` variant (batch flush)

## See also

- Master plan: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/PLAN.md`
- Phase 2 sub-plan: `plan-layer/.../02-store5-bridge.md`
