# Scheduler API (cmp-worker-scheduler)

High-level Koin-injectable `WorkScheduler` façade. Schedule daily syncs, periodic syncs, one-time-at-instant syncs, and exact-time syncs from any commonMain module without touching `WorkManager` directly.

Available since **3.1.0**. Bundled into `cmp-worker-compose-all`, so any consumer already depending on the umbrella gets the scheduler "for free."

## 1. Library responsibility = scheduling. Consumer responsibility = the work itself.

`cmp-worker-scheduler` schedules **sync** work — when it runs (daily 9am, every 6 hours, at an exact instant). It does **not** render notifications, update UI, or perform any work-specific action; those are the consumer's job inside their own `CoroutineWorker.doWork()`.

| Concern | Owner | How |
|---|---|---|
| When sync runs | **Library** | `WorkScheduler.scheduleDailyDataSync(LocalTime(9, 0))` |
| Sync implementation | **Consumer** | Extend `AbstractDataSyncWorker` + list `Syncable` repos in constructor |
| When a notification fires | **Consumer** | Build `oneTimeWorkRequest<MyNotificationWorker>` + `workManager.enqueue(...)` |
| Notification rendering | **Consumer** | `NotificationCompat.Builder(...)` (Android) / `UNUserNotificationCenter` (iOS) / etc. inside `MyNotificationWorker.doWork()` |
| Any custom domain worker | **Consumer** | Extend `CoroutineWorker`; schedule via raw `WorkManager.enqueue(oneTimeWorkRequest<YourWorker> { ... })` |

The library bundles `cmp-worker-kmp.WorkManager` via `api()`, so consumers get both the high-level `WorkScheduler` and the low-level `WorkManager` from one dep.

## 2. Add the dependency

If you already use `cmp-worker-compose-all`, you're done — scheduler is re-exported via `api()`. Otherwise:

```kotlin
// build.gradle.kts (commonMain)
implementation("io.github.mobilebytelabs:worker-scheduler:3.1.0")
```

## 3. Five-minute setup

```kotlin
// 1. Mark your repository Syncable
class CurrencyRepository(private val api: CurrencyApi, private val dao: CurrencyDao) : Syncable {
    override suspend fun syncWith(synchronizer: Synchronizer): Boolean = synchronizer.snapshotSync(
        name = "currency-rates",
        fetcher = { dao.upsertAll(api.fetchCurrencies()) },
    )
}

// 2. Subclass AbstractDataSyncWorker — 3 lines
class DataSyncWorker(
    ctx: WorkerContext,
    currencyRepository: CurrencyRepository,
    persister: SyncStatePersister,
) : AbstractDataSyncWorker(ctx, syncables = listOf(currencyRepository), persister = persister)

// 3. Wire Koin (commonMain)
val SyncModule = module {
    single { SyncStatePersister() }
    single<WorkScheduler> { DefaultWorkScheduler(workManager = get(), persister = get()) }
    factory { (ctx: WorkerContext) ->
        DataSyncWorker(ctx, currencyRepository = get(), persister = get())
    }
}
WorkerRegistry.register<DataSyncWorker> { ctx -> get { parametersOf(ctx) } }

// 4. Schedule from anywhere in commonMain
val scheduler: WorkScheduler by KoinPlatform.getKoin().inject()
scheduler.scheduleDailyDataSync(timeOfDay = LocalTime(9, 0))
```

## 4. The 7 WorkScheduler methods

| Method | Use case | Backed by |
|---|---|---|
| `enqueueDataSync(mode, payload)` | One-shot, immediate (or expedited if Foreground) | OneTimeWorkRequest |
| `scheduleDailyDataSync(timeOfDay, tz, payload)` | Sync once a day at HH:MM | PeriodicWorkRequest 24h + setInitialDelay(nextOccurrence) + KEEP |
| `schedulePeriodicDataSync(interval, initialDelay, payload)` | Sync every N (≥15min) | PeriodicWorkRequest + KEEP |
| `scheduleDataSyncAt(instant, mode, payload)` | One-shot at specific instant (flex window ~5min) | OneTimeWorkRequest + setInitialDelay |
| `scheduleDataSyncAtExact(instant, mode, payload)` | One-shot at exact instant (battery-impacting) | AlarmManager.setExactAndAllowWhileIdle on Android; BGProcessingTaskRequest on iOS |
| `observeWork(name)` | `Flow<WorkStatus>` for live progress | WorkManager.getWorkInfosByTag flow |
| `cancelWork(name)` | Cancel all work tagged with `name` | WorkManager.cancelAllWorkByTag |

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

## 6. Synchronizer / Syncable

Library-side contracts (NiA verbatim port) — see `io.github.mobilebytelabs.worker.scheduler.sync.*`.

```kotlin
interface Synchronizer {
    suspend fun getChangeListVersions(): ChangeListVersions
    suspend fun updateChangeListVersions(update: ChangeListVersions.() -> ChangeListVersions)
}

interface Syncable {
    suspend fun syncWith(synchronizer: Synchronizer): Boolean
    // 2-arg overload for payload routing (D23) — defaults to ignoring payload
    suspend fun syncWith(synchronizer: Synchronizer, payload: WorkData): Boolean =
        syncWith(synchronizer)
}

// Use changeListSync for id-list diff APIs (most server APIs)
// Use snapshotSync for whole-payload-replace APIs (single-shot snapshots)
suspend fun <Model> Synchronizer.changeListSync(...): Boolean
suspend fun Synchronizer.snapshotSync(name: String, fetcher: suspend () -> Unit): Boolean
```

`AbstractDataSyncWorker` does the fan-out: each `Syncable` in the constructor list is synced in parallel via `awaitAll`; success = all true; any false → retry; throwable → failure.

```kotlin
class DataSyncWorker(
    ctx: WorkerContext,
    currencyRepository: CurrencyRepository,
    macroIndicatorsRepository: MacroIndicatorsRepository,
    persister: SyncStatePersister,
) : AbstractDataSyncWorker(
    ctx,
    syncables = listOf(currencyRepository, macroIndicatorsRepository),
    persister = persister,
)
```

## 7. Scheduling non-sync workers (notifications, etc.)

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

## 8. Migrating from sample-side sync

If your codebase used the `samples/kmp-project-template/sync/` types (`org.mifos.sync.*` or `org.mifos.core.data.infra.*`):

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

The `samples/kmp-project-template/` reference app demonstrates the final shape: `DataSyncWorker` is 3 lines (just constructor + `: AbstractDataSyncWorker(...)`), `NotificationWorker` + `renderNotification` actuals live in the sample's own `sync/` module, and `LoanReminderUseCase` uses `WorkScheduler` for sync + raw `WorkManager` for notifications.
