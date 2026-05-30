# Scheduler API (cmp-worker-scheduler)

High-level Koin-injectable `WorkScheduler` façade. Schedule daily syncs, periodic syncs, one-time-at-instant syncs, exact-time syncs, and notifications from any commonMain module without touching `WorkManager` directly.

Available since **3.1.0**. Bundled into `cmp-worker-compose-all`, so any consumer already depending on the umbrella gets the scheduler "for free."

## 1. Why a scheduler façade

`cmp-worker-kmp`'s `WorkManager` is the low-level engine — sufficient, but verbose for common patterns (daily-at-9am sync, periodic-every-6h sync, exact-time reminder). `WorkScheduler` provides:

- **9 idiomatic methods** covering the 95% of use cases — `enqueueDataSync`, `scheduleNotification`, `scheduleDailyDataSync`, `schedulePeriodicDataSync`, `scheduleDataSyncAt`, `scheduleDataSyncAtExact`, `scheduleNotificationAt`, `observeWork`, `cancelWork`.
- **Koin-injectable** — declare `single<WorkScheduler> { DefaultWorkScheduler(workManager = get(), persister = get()) }` once; every consumer module just injects `WorkScheduler` in its constructor.
- **Cross-platform actuals** — Android (AlarmManager exact tier), iOS (BGTaskScheduler + UNUserNotificationCenter), Desktop JVM (ScheduledExecutorService + SystemTray), wasmJs (setTimeout + Notification API).
- **Synchronizer/Syncable contracts** — NiA-shaped: `changeListSync` (id-list diff) + `snapshotSync` (epoch-bumped full snapshot). Repos extend `Syncable`; `AbstractDataSyncWorker` fans out via `awaitAll`.

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
        versionReader = { it.currency },
        modelUpdater = { dao.upsertAll(api.fetchCurrencies()) },
        versionUpdater = { copy(currency = currency + 1) },
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

## 4. The 9 WorkScheduler methods

| Method | Use case | Backed by |
|---|---|---|
| `enqueueDataSync(mode, payload)` | One-shot, immediate (or expedited if Foreground) | OneTimeWorkRequest |
| `scheduleNotification(content, delay, mode)` | Fire-and-forget notification after delay | OneTimeWorkRequest + setInitialDelay |
| `scheduleDailyDataSync(timeOfDay, tz, payload)` | Sync once a day at HH:MM | PeriodicWorkRequest 24h + setInitialDelay(nextOccurrence) + KEEP |
| `schedulePeriodicDataSync(interval, initialDelay, payload)` | Sync every N (≥15min) | PeriodicWorkRequest + KEEP |
| `scheduleDataSyncAt(instant, mode, payload)` | One-shot at specific instant (flex window ~5min) | OneTimeWorkRequest + setInitialDelay |
| `scheduleDataSyncAtExact(instant, mode, payload)` | One-shot at exact instant (battery-impacting) | AlarmManager.setExactAndAllowWhileIdle on Android; BGProcessingTaskRequest on iOS |
| `scheduleNotificationAt(content, instant, mode)` | User-visible notification at exact instant | NotificationWorker + setInitialDelay |
| `observeWork(handle)` | `Flow<WorkStatus>` for live progress | WorkManager.getWorkInfoByIdLiveData / WorkInfo flow |
| `cancelWork(handle)` | Cancel by handle | WorkManager.cancelWorkById |

## 5. Per-platform notes

### Android — AlarmManager + WorkManager

- `scheduleDataSyncAtExact` requires the **`SCHEDULE_EXACT_ALARM`** permission in `AndroidManifest.xml` (Android 12+, API 31+; user-grantable from Settings).
- When denied, falls back automatically to flex-window `scheduleDataSyncAt` with a `Napier.w("SCHEDULE_EXACT_ALARM permission not granted; falling back…")` log.
- Doze respects `setExactAndAllowWhileIdle` (wakes device); consecutive exact alarms within ~9-min are throttled.
- Notifications use `NotificationManagerCompat` + idempotent `createNotificationChannel`. Caller is responsible for the `POST_NOTIFICATIONS` runtime permission on Android 13+.

### iOS — BGTaskScheduler + UNUserNotificationCenter

- `scheduleDataSyncAtExact` submits a `BGProcessingTaskRequest` with `earliestBeginDate = instant.toNSDate()`. iOS does **not** guarantee exact timing — the OS decides actual run time based on battery / connectivity / app-usage heuristics. For user-visible exact-time triggers, prefer `scheduleNotificationAt` (UNCalendarNotificationTrigger respects the requested instant precisely).
- Consumer-app `Info.plist` must register the identifier `io.github.mobilebytelabs.worker.scheduler.exact-sync` under `BGTaskSchedulerPermittedIdentifiers`.
- Notifications request `.alert + .sound` authorization on first call; consumer should set `NSUserNotificationsUsageDescription` in `Info.plist`.

### Desktop (JVM) — ScheduledExecutorService + SystemTray

- `scheduleDataSyncAtExact` uses `ScheduledExecutorService.schedule(...)` — in-process only, **lost on JVM restart**. v1.1 will layer on `cmp-worker-desktop-daemon` when its alpha gains execute-capability.
- Notifications use `java.awt.SystemTray.displayMessage(...)`. Headless environments (`GraphicsEnvironment.isHeadless() == true`) and unsupported OSes (some Linux DEs) fall back to a log-only stub.

### wasmJs — setTimeout + (optional) Notification API

- `scheduleDataSyncAtExact` uses `setTimeout(handler, delayMs)` — **in-tab only**. Background scheduling requires Service Worker `periodicSync` (Chrome-only, requires bundler config + `sw.js` scaffold; ships as a separate epic in 3.2.0).
- Notifications delegate to the existing `cmp-worker-web`'s `showWorkerNotification(id, title, body)` browser API binding. First call requests permission; denied = silent drop.

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
suspend fun <Model> Synchronizer.snapshotSync(...): Boolean
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

## 7. Migrating from sample-side sync

If your codebase used the `samples/kmp-project-template/sync/` types (`org.mifos.sync.*` or `org.mifos.core.data.infra.*`):

| Old package | New package |
|---|---|
| `org.mifos.sync.WorkScheduler` | `io.github.mobilebytelabs.worker.scheduler.WorkScheduler` |
| `org.mifos.sync.DefaultWorkScheduler` | `io.github.mobilebytelabs.worker.scheduler.DefaultWorkScheduler` |
| `org.mifos.sync.WorkMode` / `WorkHandle` / `WorkStatus` / `NotificationContent` | `io.github.mobilebytelabs.worker.scheduler.*` |
| `org.mifos.sync.SyncInitializer` / `Sync` | `io.github.mobilebytelabs.worker.scheduler.*` |
| `org.mifos.sync.NotificationWorker` / `SyncWorkHelpers` | `io.github.mobilebytelabs.worker.scheduler.*` |
| `org.mifos.core.data.infra.Synchronizer` / `Syncable` / `ChangeListVersions` / `NetworkChange` | `io.github.mobilebytelabs.worker.scheduler.sync.*` |
| `org.mifos.core.data.util.SyncManager` | `io.github.mobilebytelabs.worker.scheduler.sync.SyncManager` |
| `org.mifos.core.datastore.SyncStatePersister` | `io.github.mobilebytelabs.worker.scheduler.sync.SyncStatePersister` |
| Hand-rolled `DataSyncWorker` with fan-out logic | Extend `AbstractDataSyncWorker(ctx, syncables = listOf(...), persister)` |

The `samples/kmp-project-template/` reference app demonstrates the final shape: `DataSyncWorker` is now 3 lines (just the constructor + `: AbstractDataSyncWorker(...)`), and the entire sync infrastructure is library-side.
