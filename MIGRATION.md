# Migration Guide — worker-kmp

This guide covers migrating to the unified **worker-kmp** API from platform-specific
background-task implementations on Android, iOS, and Web.

---

## From AndroidX WorkManager

AndroidX WorkManager is the closest API match. Most concepts map 1-to-1.

### Dependency

```kotlin
// Before
implementation("androidx.work:work-runtime-ktx:2.x.x")

// After
implementation("io.github.mobilebytelabs:worker-kmp:<version>")  // commonMain
implementation("io.github.mobilebytelabs:worker-android:<version>")  // androidMain
```

### Worker class

```kotlin
// Before — androidx
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val endpoint = inputData.getString("endpoint") ?: return Result.failure()
        sync(endpoint)
        return Result.success(workDataOf("done" to true))
    }
}

// After — worker-kmp
class SyncWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val endpoint = inputData.getString("endpoint") ?: return WorkResult.failure()
        sync(endpoint)
        return WorkResult.success(workDataOf("done" to true))
    }
}
```

### Initialization

```kotlin
// Before — no explicit init needed; WorkManager auto-initialises via Initializer API.
// Custom factory (Hilt):
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted params: WorkerParameters,
    val repo: SyncRepository,
) : CoroutineWorker(context, params) { ... }

// After — provide a factory at app start:
initializeWorkerAndroid(
    context = applicationContext,
    workerFactory = object : AndroidWorkerFactory {
        override fun create(workerClassName: String, context: WorkerContext): CoroutineWorker? =
            when (workerClassName) {
                "SyncWorker" -> SyncWorker(context, syncRepository)
                else         -> null
            }
    },
)
```

### Enqueue one-time work

```kotlin
// Before
val request = OneTimeWorkRequestBuilder<SyncWorker>()
    .setInputData(workDataOf("endpoint" to "/api/sync"))
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(1))
    .addTag("sync")
    .build()
WorkManager.getInstance(context).enqueue(request)

// After
val request = OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker")
    .setInputData(workDataOf("endpoint" to "/api/sync"))
    .setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RetryConfig(initialDelay = 1.minutes))
    .addTag("sync")
    .build()
workManager.enqueue(request)
```

### Enqueue periodic work

```kotlin
// Before
val request = PeriodicWorkRequestBuilder<CacheWorker>(6, TimeUnit.HOURS)
    .build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "cache-cleanup", ExistingPeriodicWorkPolicy.KEEP, request
)

// After
val request = PeriodicWorkRequestBuilder<CacheWorker>("CacheWorker",
    repeatInterval = 6.hours,
).build()
workManager.enqueueUniquePeriodicWork(
    "cache-cleanup", ExistingPeriodicWorkPolicy.KEEP, request
)
```

### Observe work status

```kotlin
// Before
WorkManager.getInstance(context)
    .getWorkInfosByTagLiveData("sync")
    .observe(lifecycleOwner) { infos -> ... }

// After — Flow-based (no LiveData dependency)
workManager.getWorkInfosByTag("sync")
    .collect { infos -> ... }
```

### WorkResult mapping

| AndroidX | worker-kmp |
|---|---|
| `Result.success()` | `WorkResult.success()` |
| `Result.success(outputData)` | `WorkResult.success(workDataOf(...))` |
| `Result.failure()` | `WorkResult.failure()` |
| `Result.retry()` | `WorkResult.retry()` |

### Progress reporting

```kotlin
// Before
setProgressAsync(workDataOf("progress" to 50))

// After — suspend, no async wrapper needed
setProgress(WorkProgress(progress = 50))
```

---

## From iOS BGTaskScheduler

BGTaskScheduler has a different model — tasks register callbacks via `Info.plist` identifiers.
worker-kmp replaces this with the same `CoroutineWorker` API you use on Android.

### Dependency (Kotlin side)

```kotlin
// In iosMain
implementation("io.github.mobilebytelabs:worker-ios:<version>")
```

### Info.plist — still required

iOS still requires task identifiers in `Info.plist`. Register one identifier per unique worker tag:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER).sync</string>
</array>
```

### Worker registration

```kotlin
// Before — Swift (AppDelegate)
BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.example.sync", using: nil) { task in
    self.handleSyncTask(task as! BGProcessingTask)
}

// After — Kotlin (shared module, called from Swift AppDelegate before didFinishLaunching returns)
@OptIn(ExperimentalWorkerApi::class)
fun initApp() {
    initIosWorkManager(
        workerFactory = object : IosWorkerFactory {
            override fun create(workerClassName: String, context: WorkerContext): CoroutineWorker? =
                when (workerClassName) {
                    "SyncWorker" -> SyncWorker(context)
                    else         -> null
                }
        },
        config = IosWorkManagerConfig(
            enableBackgroundTasks      = true,
            bgProcessingTaskIdentifier = "com.example.sync",  // must match Info.plist
        ),
    )
}
```

### Scheduling

```kotlin
// Before — Swift
let request = BGProcessingTaskRequest(identifier: "com.example.sync")
request.requiresNetworkConnectivity = true
try? BGTaskScheduler.shared.submit(request)

// After — Kotlin (shared module)
@OptIn(ExperimentalWorkerApi::class)
workManager.enqueue(
    OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker")
        .setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
        .addTag("sync")
        .build()
)
```

### Constraints mapping

| BGTaskRequest property | worker-kmp Constraints |
|---|---|
| `requiresNetworkConnectivity = true` | `setRequiredNetworkType(NetworkType.CONNECTED)` |
| `requiresExternalPower = true` | `setRequiresCharging(true)` |
| *(no equivalent)* | `setRequiresBatteryNotLow(true)` |
| *(no equivalent)* | `setRequiresDeviceIdle(true)` |

> **Note:** iOS background execution is limited by the OS to ~30 seconds for processing tasks.
> For long-running work, split into multiple chained workers and handle `BGTaskExpiredHandler`
> by saving checkpoint state to the work output data.

---

## Desktop — Initialization

The `initializeWorkerDesktop()` function now accepts an optional `workerFactory`. Previously only
`ReflectionWorkerFactory` (class-name based) was supported; now you can supply a DI-aware factory.

```kotlin
// Before (still compiles — no change needed for reflection-based factories)
fun main() {
    initializeWorkerDesktop()
}

// After — supply a factory for DI injection or class-splitting across modules
fun main() {
    initializeWorkerDesktop(
        config = DesktopWorkManagerConfig(
            persistenceEnabled = true,            // default — work survives JVM restarts
            persistencePath = File("~/.my-app"),  // default: ~/.worker-kmp
        ),
        workerFactory = object : DesktopWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
                when (workerClass) {
                    "SyncWorker" -> SyncWorker(context, syncRepository)
                    else         -> error("Unknown worker: $workerClass")
                }
        },
    )
}
```

### Persistence mapping

| Before | After |
|---|---|
| Work state lost on JVM exit | Work state written to `~/.worker-kmp/*.properties` |
| No restart recovery | ENQUEUED/RUNNING work restored on next `initializeWorkerDesktop()` |
| — | RUNNING → ENQUEUED on restore (interrupted mid-execution) |
| — | Terminal states (SUCCEEDED/FAILED/CANCELLED) deleted from disk |

---

## From Web (custom polling / setInterval)

The web platform is new in v2.0.0. If you previously used `setInterval`, `setTimeout`, or
`requestIdleCallback` for background-like scheduling, worker-kmp gives you retries, constraints,
and persistence with the same API as the other platforms.

### Dependency

```kotlin
// In jsMain or wasmJsMain
implementation("io.github.mobilebytelabs:worker-web:<version>")
```

### Initialization

```kotlin
// Before — custom polling
val intervalId = window.setInterval({
    if (navigator.onLine) runSync()
}, 30_000)

// After
@OptIn(ExperimentalWorkerApi::class)
fun main() {
    if (!isWebWorkManagerSupported()) return
    initWebWorkManager(
        workerFactory = object : WebWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
                when (workerClass) {
                    "SyncWorker" -> SyncWorker(context)
                    else         -> error("Unknown worker: $workerClass")
                }
        },
        config = WebWorkManagerConfig(enablePersistence = true),
    )
    val wm = PlatformWorkManager()

    // Enqueue once — will retry on failure, wait for CONNECTED constraint, survive page reload
    wm.enqueue(
        OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker")
            .setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RetryConfig(maxAttempts = 5))
            .build()
    )
}
```

### Constraint wake-up

Unlike `setInterval`, the web constraint loop wires `window.addEventListener("online", …)` so
network-constrained work wakes up **immediately** when connectivity is restored rather than
waiting for the next poll cycle.

---

## API quick-reference

| Concept | AndroidX | worker-kmp |
|---|---|---|
| Worker base class | `CoroutineWorker(ctx, params)` | `CoroutineWorker(context)` |
| Success | `Result.success()` | `WorkResult.success()` |
| Failure | `Result.failure()` | `WorkResult.failure()` |
| Retry | `Result.retry()` | `WorkResult.retry()` |
| Input data | `inputData.getString(key)` | `inputData.getString(key)` |
| Output data | `Result.success(outputData)` | `WorkResult.success(workDataOf(...))` |
| Progress | `setProgressAsync(workDataOf(...))` | `setProgress(WorkProgress(n))` |
| Observe | `.getWorkInfosByTagLiveData(tag)` | `.getWorkInfosByTag(tag): Flow<…>` |
| Cancel by id | `.cancelWorkById(id)` | `.cancelWorkById(id)` |
| Cancel by tag | `.cancelAllWorkByTag(tag)` | `.cancelAllWorkByTag(tag)` |
| Chain | `.beginWith(w).then(w2).enqueue()` | `.beginWith(w).then(w2).enqueue()` |
