# Scheduler API (cmp-worker-scheduler)

High-level Koin-injectable `WorkScheduler` façade. Schedule daily syncs, periodic syncs, one-time-at-instant syncs, and exact-time syncs from any commonMain module without touching `WorkManager` directly.

Available since **3.1.1**. Bundled into `cmp-worker-compose-all`, so any consumer already depending on the umbrella gets the scheduler "for free."

## 1. Library responsibility = scheduling. Consumer responsibility = the work itself.

`cmp-worker-scheduler` schedules **sync** work — when it runs (daily 9am, every 6 hours, at an exact instant). It does **not** render notifications, update UI, or perform any work-specific action; those are the consumer's job inside their own `CoroutineWorker.doWork()`.

| Concern | Owner | How |
|---|---|---|
| When sync runs | **Library** | `scheduler.scheduleDailyDataSync<AppSyncWorker>(LocalTime(9, 0))` |
| Sync implementation | **Consumer** | Extend `AbstractDataSyncWorker` + list `Syncable` adopters in constructor |
| Sync source variety | **Consumer chooses** | Implement `Syncable` on a repo · or wrap a function with `FetcherSyncable` · or wrap a Store5 `Store` with `StoreSyncable` |
| When a notification fires | **Consumer** | Build `oneTimeWorkRequest<MyNotificationWorker>` + `workManager.enqueue(...)` |
| Notification rendering | **Consumer** | `NotificationCompat.Builder(...)` (Android) / `UNUserNotificationCenter` (iOS) / etc. inside `MyNotificationWorker.doWork()` |
| Any custom domain worker | **Consumer** | Extend `CoroutineWorker`; schedule via raw `WorkManager.enqueue(oneTimeWorkRequest<YourWorker> { ... })` |

The library bundles `cmp-worker-kmp.WorkManager` via `api()`, so consumers get both the high-level `WorkScheduler` and the low-level `WorkManager` from one dep.

## 2. Add the dependency

If you already use `cmp-worker-compose-all`, you're done — scheduler + Store5 adapter are re-exported via `api()`. Otherwise:

```kotlin
// build.gradle.kts (commonMain)
implementation("io.github.mobilebytelabs:worker-scheduler:3.1.1")
// Optional, only if you sync from Store5 instances:
implementation("io.github.mobilebytelabs:worker-scheduler-store5:3.1.1")
```

## 3. Five-minute setup

```kotlin
// 1. Mark your repository Syncable — implement on a repo class
class CurrencyRepository(private val api: CurrencyApi, private val dao: CurrencyDao) : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean = synchronizer.snapshotSync(
        name = "currency-rates",
        fetcher = { dao.upsertAll(api.fetchCurrencies()) },
    )
}

// 2. Subclass AbstractDataSyncWorker — 3 lines
class AppSyncWorker(
    ctx: WorkerContext,
    currencyRepository: CurrencyRepository,
    persister: SyncStatePersister,
) : AbstractDataSyncWorker(ctx, syncables = listOf(currencyRepository), persister = persister)

// 3. Wire Koin (commonMain) — single<WorkScheduler> stays stateless across all worker types
val SyncModule = module {
    single { SyncStatePersister() }
    single<WorkScheduler> { DefaultWorkScheduler(workManager = get(), persister = get()) }
    factory { (ctx: WorkerContext) ->
        AppSyncWorker(ctx, currencyRepository = get(), persister = get())
    }
}
WorkerRegistry.register<AppSyncWorker> { ctx -> get { parametersOf(ctx) } }

// 4. Schedule from anywhere in commonMain — pass YOUR worker type at the call site
val scheduler: WorkScheduler by KoinPlatform.getKoin().inject()
scheduler.scheduleDailyDataSync<AppSyncWorker>(timeOfDay = LocalTime(9, 0))
```

The reified `<AppSyncWorker>` is how the scheduler knows which class WorkManager should instantiate. Same DSL shape as `oneTimeWorkRequest<W> { ... }` from `cmp-worker-kmp`.

## 4. The 7 WorkScheduler methods

All 5 schedule methods are reified — the worker class is supplied at the call site, so one `WorkScheduler` instance serves any number of distinct worker classes.

| Method | Use case | Backed by |
|---|---|---|
| `enqueueDataSync<W>(mode, payload)` | One-shot, immediate (or expedited if Foreground) | OneTimeWorkRequest |
| `scheduleDailyDataSync<W>(timeOfDay, tz, payload)` | Sync once a day at HH:MM | PeriodicWorkRequest 24h + setInitialDelay(nextOccurrence) + KEEP |
| `schedulePeriodicDataSync<W>(interval, initialDelay, payload)` | Sync every N (≥15min) | PeriodicWorkRequest + KEEP |
| `scheduleDataSyncAt<W>(instant, mode, payload)` | One-shot at specific instant (flex window ~5min) | OneTimeWorkRequest + setInitialDelay |
| `scheduleDataSyncAtExact<W>(instant, mode, payload)` | One-shot at exact instant (battery-impacting) | AlarmManager.setExactAndAllowWhileIdle on Android; BGProcessingTaskRequest on iOS |
| `observeWork(name)` | `Flow<WorkStatus>` for live progress | WorkManager.getWorkInfosByTag flow |
| `cancelWork(name)` | Cancel all work tagged with `name` | WorkManager.cancelAllWorkByTag |

`W` is constrained to `W : AbstractDataSyncWorker`. For non-sync workers, use raw `WorkManager.enqueue(oneTimeWorkRequest<W> { ... })` directly (see §8).

### Multi-worker example

```kotlin
// One scheduler serves multiple worker classes — no extra DI binding needed
scheduler.scheduleDailyDataSync<UserSyncWorker>(timeOfDay = LocalTime(7, 0))     // morning user refresh
scheduler.scheduleDailyDataSync<AnalyticsRollupWorker>(timeOfDay = LocalTime(23, 0)) // nightly rollup
scheduler.schedulePeriodicDataSync<MetricsExportWorker>(interval = 1.hours)      // hourly metrics
scheduler.enqueueDataSync<UserSyncWorker>(payload = workDataOf("trigger" to "manual"))
```

## 5. Per-platform exact-time notes

### Android — AlarmManager + WorkManager

- `scheduleDataSyncAtExact` requires the **`SCHEDULE_EXACT_ALARM`** permission in `AndroidManifest.xml` (Android 12+, API 31+; user-grantable from Settings).
- When denied, falls back automatically to flex-window `scheduleDataSyncAt` with a `Log.w("ExactAlarmScheduler", "...")` log.
- Doze respects `setExactAndAllowWhileIdle` (wakes device); consecutive exact alarms within ~9-min are throttled.

### iOS — BGTaskScheduler

- `scheduleDataSyncAtExact` submits a `BGProcessingTaskRequest` with `earliestBeginDate = instant.toNSDate()`. iOS does **not** guarantee exact timing — the OS decides actual run time based on battery / connectivity / app-usage heuristics. For user-visible exact-time triggers, fire your own `UNUserNotificationCenter` notification from inside your `CoroutineWorker.doWork()` (consumer responsibility).
- Consumer-app `Info.plist` must register the identifier `io.github.mobilebytelabs.worker.scheduler.exact_sync` under `BGTaskSchedulerPermittedIdentifiers`.

### Desktop (JVM) — ScheduledExecutorService

- `scheduleDataSyncAtExact` uses `ScheduledExecutorService.schedule(...)` — in-process only, **lost on JVM restart**. v1.1 will layer on `cmp-worker-desktop-daemon` when its alpha gains execute-capability.

### Web (wasmJs + js) — setTimeout

- `scheduleDataSyncAtExact` uses `setTimeout(handler, delayMs)` — **in-tab only**. Background scheduling requires Service Worker `periodicSync` (Chrome-only, requires bundler config + `sw.js` scaffold; ships as a separate epic in 3.2.0).

## 6. Synchronizer / Syncable — multi-source pattern

Sync contracts live in `io.github.mobilebytelabs.worker.scheduler.sync.*`. Three ergonomic ways to author a `Syncable`, depending on where your data lives:

```kotlin
interface Synchronizer {
    suspend fun getChangeListVersions(): ChangeListVersions
    suspend fun updateChangeListVersions(update: ChangeListVersions.() -> ChangeListVersions)
}

interface Syncable {
    suspend fun syncWith(synchronizer: Synchronizer): Boolean
    suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean = syncWith(synchronizer)
}
```

### 6.1 Repository pattern (preferred when your repo already exists)

Implement `Syncable` directly on the repository class. Two extension functions cover the two common server shapes:

```kotlin
// Snapshot API — server returns the full canonical payload (Frankfurter, WorldBank)
class CurrencyRepository(...) : Syncable {
    override suspend fun syncWith(sync: Synchronizer): Boolean = sync.snapshotSync("currency-rates") {
        dao.upsertAll(api.fetchLatest("USD"))
    }
}

// Change-list API — server returns deltas since version N (NiA topics, GitHub events)
class TopicsRepository(...) : Syncable {
    override suspend fun syncWith(sync: Synchronizer): Boolean = sync.changeListSync(
        versionReader     = { it.versions["topics"]?.toInt() ?: 0 },
        changeListFetcher = { since -> api.fetchTopicChanges(since) },
        versionUpdater    = { latest -> copy(versions = versions + ("topics" to latest.toLong())) },
        modelDeleter      = { ids -> dao.deleteByIds(ids) },
        modelUpdater      = { ids -> dao.upsertByIds(api.fetchByIds(ids)) },
    )
}
```

### 6.2 Function pattern via `FetcherSyncable` (for one-off lambdas)

Wraps a `suspend (WorkData) -> Unit` block without defining a class. Same staleness semantics as `snapshotSync`:

```kotlin
val currencySyncable = FetcherSyncable("currency-rates") { payload ->
    val base = payload.getString("currency.base") ?: "USD"
    dao.upsertAll(api.fetchLatest(base))
}

class AppSyncWorker(
    ctx: WorkerContext,
    persister: SyncStatePersister,
) : AbstractDataSyncWorker(ctx, syncables = listOf(currencySyncable), persister = persister)
```

### 6.3 Store5 pattern via `StoreSyncable` (requires `cmp-worker-scheduler-store5`)

Adapter module `cmp-worker-scheduler-store5` provides `Syncable` wrappers for Mobile Native Foundation Store5 instances:

```kotlin
import io.github.mobilebytelabs.worker.scheduler.store5.StoreSyncable
import io.github.mobilebytelabs.worker.scheduler.store5.MutableStoreSyncable
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.MutableStore

// Read-path sync — forces store.fresh(key) which triggers a network fetch
val currencyStoreSyncable = StoreSyncable(
    name  = "currency-rates",
    key   = "USD",
    store = appStoreRegistry.currencyStore,  // Store<String, FrankfurterResponse>
)

// Write-path sync — pushes a pending mutation via mutableStore.write(...)
val profileEditSyncable = MutableStoreSyncable(
    name         = "profile-edit-${edits.id}",
    key          = edits.id,
    value        = edits,
    mutableStore = appStoreRegistry.profileMutableStore,  // MutableStore<UserId, UserProfile>
)

class AppSyncWorker(
    ctx: WorkerContext,
    persister: SyncStatePersister,
) : AbstractDataSyncWorker(
    ctx,
    syncables = listOf(currencyStoreSyncable, profileEditSyncable),  // both fan out in parallel
    persister = persister,
)
```

For payload routing (e.g. picking the Store key from `WorkData`), use the lower-level extension functions inside a custom `Syncable`:

```kotlin
class CurrencyStoreSyncable(
    private val store: Store<String, FrankfurterResponse>,
) : Syncable {
    override suspend fun syncWith(sync: Synchronizer, payload: WorkData): Boolean {
        val base = payload.getString("currency.base") ?: "USD"
        return sync.storeSync(name = "currency-rates-$base", key = base, store = store)
    }
    override suspend fun syncWith(sync: Synchronizer) = syncWith(sync, workDataOf("currency.base" to "USD"))
}
```

### 6.4 Sequencing via `CompositeSyncable` (rare — only when one sync depends on another)

```kotlin
val onboarding = CompositeSyncable(
    settingsRepo,      // sync 1st
    userProfileRepo,   // sync 2nd (depends on settings being current)
    userContentRepo,   // sync 3rd (depends on profile being current)
)
```

For independent syncables, list them directly in `AbstractDataSyncWorker.syncables` — those run in parallel (`awaitAll`).

### Fan-out semantics

`AbstractDataSyncWorker.doWork()` iterates the `syncables` list inside `coroutineScope { … async { it.syncWith(this@AbstractDataSyncWorker, inputData) } … awaitAll() }`. All true → success. Any false → retry. Any throw → failure.

```kotlin
class AppSyncWorker(
    ctx: WorkerContext,
    currencyRepository: CurrencyRepository,
    macroIndicatorsRepository: MacroIndicatorsRepository,
    currencyStoreSyncable: StoreSyncable<String, FrankfurterResponse>,
    persister: SyncStatePersister,
) : AbstractDataSyncWorker(
    ctx,
    syncables = listOf(currencyRepository, macroIndicatorsRepository, currencyStoreSyncable),
    persister = persister,
)
```

## 7. Module structure

| Module | Coords | Purpose |
|---|---|---|
| `cmp-worker-scheduler` | `io.github.mobilebytelabs:worker-scheduler` | `WorkScheduler` + `AbstractDataSyncWorker` + `Synchronizer` + `Syncable` + `FetcherSyncable` + `CompositeSyncable` |
| `cmp-worker-scheduler-store5` | `io.github.mobilebytelabs:worker-scheduler-store5` | `StoreSyncable` + `MutableStoreSyncable` + `Synchronizer.storeSync` + `Synchronizer.mutableStoreSync` |
| `cmp-worker-compose-all` | `io.github.mobilebytelabs:worker-compose-all` | Umbrella re-exporting both above + cmp-worker-kmp + cmp-worker-koin + per-platform engines |

Standalone usage: the `io.github.mobilebytelabs.worker.scheduler.sync.*` types (`Synchronizer`, `Syncable`, `changeListSync`, `snapshotSync`) have no WorkManager dependency at the contract level — you can use them as a NiA-shaped pattern in any KMP project without engaging the scheduler.

## 8. Scheduling non-sync workers (notifications, etc.)

Use raw `WorkManager.enqueue(...)` for any worker class that isn't `AbstractDataSyncWorker`:

```kotlin
class MyNotificationWorker(ctx: WorkerContext) : CoroutineWorker(ctx) {
    override suspend fun doWork(): WorkResult {
        val title = inputData.getString("title") ?: return WorkResult.failure("missing title")
        // Consumer-owned rendering — NotificationCompat (Android),
        // UNUserNotificationCenter (iOS), SystemTray (Desktop), etc.
        renderNotification(title)
        return WorkResult.success()
    }
}

// Scheduling — use raw WorkManager, NOT WorkScheduler:
val request = oneTimeWorkRequest<MyNotificationWorker> {
    setInputData(workDataOf("title" to "Loan due", "body" to "Payment due today"))
    setInitialDelay(15.minutes)
}
workManager.enqueue(request)
```

`cmp-worker-scheduler` bundles `cmp-worker-kmp` via `api()`, so injecting `WorkManager` in the same module that uses `WorkScheduler` requires no extra dep.

## 9. Migrating from sample-side sync (3.1.0 → 3.1.1)

### From the old (3.1.0) typed-less API

| Before (3.1.0) | After (3.1.1) |
|---|---|
| `scheduler.enqueueDataSync(...)` | `scheduler.enqueueDataSync<AppSyncWorker>(...)` |
| `scheduler.scheduleDailyDataSync(...)` | `scheduler.scheduleDailyDataSync<AppSyncWorker>(...)` |
| `scheduler.schedulePeriodicDataSync(...)` | `scheduler.schedulePeriodicDataSync<AppSyncWorker>(...)` |
| `scheduler.scheduleDataSyncAt(...)` | `scheduler.scheduleDataSyncAt<AppSyncWorker>(...)` |
| `scheduler.scheduleDataSyncAtExact(...)` | `scheduler.scheduleDataSyncAtExact<AppSyncWorker>(...)` |

### From samples/.../sync (any version)

| Old package | New package |
|---|---|
| `org.mifos.sync.WorkScheduler` | `io.github.mobilebytelabs.worker.scheduler.WorkScheduler` |
| `org.mifos.sync.DefaultWorkScheduler` | `io.github.mobilebytelabs.worker.scheduler.DefaultWorkScheduler` |
| `org.mifos.sync.WorkMode` / `WorkHandle` / `WorkStatus` | `io.github.mobilebytelabs.worker.scheduler.*` |
| `org.mifos.core.data.infra.Synchronizer` / `Syncable` / `ChangeListVersions` / `NetworkChange` | `io.github.mobilebytelabs.worker.scheduler.sync.*` |
| `org.mifos.core.data.util.SyncManager` | `io.github.mobilebytelabs.worker.scheduler.sync.SyncManager` |
| `org.mifos.core.datastore.SyncStatePersister` | `io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister` |
| Hand-rolled `DataSyncWorker` with fan-out logic | Extend `AbstractDataSyncWorker(ctx, syncables = listOf(...), persister)` |
| `org.mifos.sync.NotificationWorker` + `NotificationContent` + `renderNotification` | **Stay in consumer code** — library doesn't ship these |
| `WorkScheduler.scheduleNotification(...)` / `scheduleNotificationAt(...)` | **Removed from library** — use `workManager.enqueue(oneTimeWorkRequest<MyNotificationWorker> { setInputData(...); setInitialDelay(...) })` |
