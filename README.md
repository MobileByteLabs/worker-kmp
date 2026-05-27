# worker-kmp

A Kotlin Multiplatform background task scheduler — the `WorkManager` API you know from Android, available on every platform.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/worker-kmp)](https://central.sonatype.com/search?q=io.github.mobilebytelabs)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-Apache--2.0-green)](LICENSE)

## Platform Support

| Platform | Module | Foreground | Background | Min Version |
|---|---|:---:|:---:|---|
| Android | `worker-android` | ✅ | ✅ via `androidx.work` | API 21 |
| Desktop (JVM) | `worker-desktop` | ✅ | ✅ in-process | JDK 11 |
| iOS | `worker-ios` | ✅ | ⚠️ opt-in via BGTaskScheduler¹ | iOS 13.0 |
| Web (JS/WasmJs) | `worker-web` | ✅ | ⚠️ opt-in via Background Sync¹ | Chrome/Node |
| Compose Multiplatform | `worker-compose` | ✅ | — | — |
| All (common API) | `worker-kmp` | ✅ | — | — |

> ¹ **iOS** and **Web** are marked `@ExperimentalWorkerApi`. Both support opt-in background
> scheduling: iOS via [BGTaskScheduler](#ios-bgtaskscheduler), Web via
> [Browser Background Sync](#browser-background-sync). Opt in with `@OptIn(ExperimentalWorkerApi::class)`.

## Setup

> **Latest version**: check the Maven Central badge above or visit
> [central.sonatype.com/search?q=io.github.mobilebytelabs](https://central.sonatype.com/search?q=io.github.mobilebytelabs)

```kotlin
// build.gradle.kts (libs.versions.toml recommended)

// gradle/libs.versions.toml
// [versions]
// worker = "<latest>"   ← replace with the version from Maven Central badge above

kotlin {
    sourceSets {
        // Common API — always required
        commonMain.dependencies {
            implementation(libs.worker.kmp)
        }

        // Platform modules — pick the ones you need
        androidMain.dependencies {
            implementation(libs.worker.android)
        }
        iosMain.dependencies {
            implementation(libs.worker.ios)
        }
        jvmMain.dependencies {
            implementation(libs.worker.desktop)
        }
        jsMain.dependencies {
            implementation(libs.worker.web)
        }

        // Testing utilities
        commonTest.dependencies {
            implementation(libs.worker.test)
        }
    }
}
```

Version catalog entries (`gradle/libs.versions.toml`):

```toml
[versions]
worker = "<latest>"   # see Maven Central badge at the top of this README

[libraries]
worker-kmp     = { module = "io.github.mobilebytelabs:worker-kmp",     version.ref = "worker" }
worker-android = { module = "io.github.mobilebytelabs:worker-android", version.ref = "worker" }
worker-ios     = { module = "io.github.mobilebytelabs:worker-ios",     version.ref = "worker" }
worker-desktop = { module = "io.github.mobilebytelabs:worker-desktop", version.ref = "worker" }
worker-web     = { module = "io.github.mobilebytelabs:worker-web",     version.ref = "worker" }
worker-test    = { module = "io.github.mobilebytelabs:worker-test",    version.ref = "worker" }
```

## Quick Start

### 1. Define a worker

```kotlin
class SyncWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val userId = inputData.getString("user_id") ?: return WorkResult.failure("missing user_id")
        // ... do work ...
        return WorkResult.success(workDataOf("synced_count" to 42))
    }
}
```

### 2. Initialize the platform (once, at app start)

```kotlin
// Android
initializeWorkerAndroid(context, workerFactory)

// iOS  (@ExperimentalWorkerApi — foreground only)
@OptIn(ExperimentalWorkerApi::class)
initIosWorkManager(workerFactory)

// Desktop (JVM)
initializeWorkerDesktop()

// Web (JS/WasmJs  (@ExperimentalWorkerApi — foreground only)
@OptIn(ExperimentalWorkerApi::class)
initWebWorkManager(workerFactory)
```

### 3. Enqueue work

```kotlin
val request = OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker")
    .setInputData(workDataOf("user_id" to "u123"))
    .addTag("sync")
    .build()

val id = workManager.enqueue(request)
```

## Core Concepts

### CoroutineWorker

All workers extend `CoroutineWorker`. The `doWork()` suspension function runs on the appropriate dispatcher for each platform.

```kotlin
class UploadWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val fileUri = inputData.getString("file_uri")
            ?: return WorkResult.failure("no file_uri in input")

        for (chunk in 0..9) {
            setProgress(WorkProgress(chunk * 10))
            uploadChunk(fileUri, chunk)
        }

        return WorkResult.success(workDataOf("bytes_uploaded" to 1_048_576L))
    }
}
```

### WorkResult

```kotlin
sealed class WorkResult {
    data class Success(val outputData: WorkData) : WorkResult()
    data class Failure(val message: String?, val outputData: WorkData) : WorkResult()
    data class Retry(val reason: String?) : WorkResult()
}

// Convenience constructors
WorkResult.success()
WorkResult.success(workDataOf("key" to "value"))
WorkResult.failure("something went wrong")
WorkResult.retry("server returned 503")
```

### WorkData

Typed key-value pairs for input/output.

```kotlin
val data = workDataOf(
    "name"   to "Alice",
    "age"    to 30,
    "score"  to 99.5f,
    "active" to true,
    "tags"   to arrayOf("admin", "user"),
)

data.getString("name")       // "Alice"
data.getInt("age")           // 30
data.getFloat("score")       // 99.5f
data.getBoolean("active")    // true
data.getStringArray("tags")  // ["admin", "user"]
```

## One-Time Work

```kotlin
val request = OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker")
    .setInputData(workDataOf("since" to lastSyncTimestamp))
    .addTag("sync")
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        RetryConfig(
            maxAttempts = 3,
            initialDelay = 5.seconds,
            maxDelay = 60.seconds,
            multiplier = 2.0,
        ),
    )
    .build()

val id = workManager.enqueue(request)
```

### DSL shorthand

```kotlin
val request = oneTimeWorkRequest<SyncWorker>("SyncWorker") {
    inputData = workDataOf("since" to lastSyncTimestamp)
    addTag("sync")
}
```

## Periodic Work

```kotlin
val request = PeriodicWorkRequestBuilder<CacheCleanupWorker>("CacheCleanupWorker",
    repeatInterval = 6.hours
).build()

workManager.enqueueUniquePeriodicWork(
    uniqueWorkName = "cache-cleanup",
    existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
    request = request,
)
```

`ExistingPeriodicWorkPolicy` options:

| Policy | Behaviour |
|---|---|
| `KEEP` | Do nothing if a job with this name is already enqueued or running |
| `REPLACE` | Cancel the existing job and enqueue the new one |
| `UPDATE` | Update constraints/interval without interrupting a running execution |

## Constraints

```kotlin
val constraints = Constraints {
    setRequiredNetworkType(NetworkType.CONNECTED)
    setRequiresCharging(true)
    setRequiresDeviceIdle(true)
    setRequiresBatteryNotLow(true)
    setRequiresStorageNotLow(true)
}

val request = OneTimeWorkRequestBuilder<BackupWorker>("BackupWorker")
    .setConstraints(constraints)
    .build()
```

`NetworkType` options: `NOT_REQUIRED`, `CONNECTED`, `UNMETERED`, `NOT_ROAMING`, `METERED`.

### Content URI Triggers (Android only, API 24+)

Trigger work automatically when a content-provider URI changes:

```kotlin
val constraints = Constraints {
    addContentUriTrigger(
        uriString = "content://com.example.provider/items",
        triggerForDescendants = true,   // also fires for content://…/items/42 etc.
    )
}
```

## Retry and Backoff

```kotlin
val retryConfig = RetryConfig(
    maxAttempts   = 5,
    initialDelay  = 2.seconds,
    maxDelay      = 5.minutes,
    backoffPolicy = BackoffPolicy.EXPONENTIAL,
    multiplier    = 2.0,
)

val request = OneTimeWorkRequestBuilder<NetworkWorker>("NetworkWorker")
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, retryConfig)
    .build()
```

Return `WorkResult.retry()` from `doWork()` to trigger a retry attempt:

```kotlin
override suspend fun doWork(): WorkResult {
    return try {
        api.sync()
        WorkResult.success()
    } catch (e: IOException) {
        WorkResult.retry("network failure: ${e.message}")
    }
}
```

## Progress Reporting

```kotlin
class TranscodeWorker(context: WorkerContext) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val frames = 300
        repeat(frames) { frame ->
            transcode(frame)
            setProgress(WorkProgress(progress = (frame + 1) * 100 / frames))
        }
        return WorkResult.success()
    }
}
```

Observe progress on the caller side:

```kotlin
workManager.getWorkInfosByTag("transcode").collect { infos ->
    infos.forEach { info ->
        println("${info.id}: ${info.state} — ${info.progress?.progress}%")
    }
}
```

## Monitoring Work

```kotlin
// Single work item (suspend, current value)
val info: WorkInfo? = workManager.getWorkInfoById(id)
println(info?.state)       // ENQUEUED / RUNNING / SUCCEEDED / FAILED / CANCELLED / BLOCKED
println(info?.outputData)  // WorkData with results

// Flow of all work with a given tag
workManager.getWorkInfosByTag("sync").collect { infos ->
    infos.forEach { println("${it.id}: ${it.state}") }
}
```

`WorkInfo.State`:

| State | Terminal? |
|---|---|
| `ENQUEUED` | no |
| `RUNNING` | no |
| `SUCCEEDED` | yes |
| `FAILED` | yes |
| `CANCELLED` | yes |
| `BLOCKED` | no |

## Work Chaining

Chain multiple workers sequentially. The output data of each step flows to the input of the next.

```kotlin
val download  = OneTimeWorkRequestBuilder<DownloadWorker>("DownloadWorker").build()
val transcode = OneTimeWorkRequestBuilder<TranscodeWorker>("TranscodeWorker").build()
val upload    = OneTimeWorkRequestBuilder<UploadWorker>("UploadWorker").build()

workManager
    .beginWith(download)
    .then(transcode)
    .then(upload)
    .enqueue()
```

## Conditional Execution

`ConditionalWorker` gates `doWork()` behind a runtime `condition()` check. When `condition()` returns `false` the worker reports `WorkResult.failure` without invoking the actual work body, so the chain halts cleanly without retrying.

Typical use-cases: feature flags, auth state, required resources.

```kotlin
class FeatureSyncWorker(context: WorkerContext) : ConditionalWorker(context) {

    override suspend fun condition(): Boolean =
        FeatureFlags.isEnabled("premium_sync") && AuthManager.isSignedIn()

    override suspend fun doConditionalWork(): WorkResult {
        return if (networkService.isReachable()) {
            val result = apiService.sync()
            WorkResult.success(workDataOf("synced" to result.count))
        } else {
            WorkResult.retry("network unavailable")
        }
    }
}
```

## Cancellation

```kotlin
// Cancel a specific work item
workManager.cancelWorkById(id)

// Cancel all work with a given tag
workManager.cancelAllWorkByTag("sync")
```

Cancellation is a no-op when the work is already in a terminal state (`SUCCEEDED`, `FAILED`, `CANCELLED`).

## Platform Initialization

### Android

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeWorkerAndroid(
            context = this,
            workerFactory = object : AndroidWorkerFactory {
                override fun create(workerClassName: String, context: WorkerContext): CoroutineWorker? =
                    when (workerClassName) {
                        "SyncWorker"   -> SyncWorker(context)
                        "BackupWorker" -> BackupWorker(context)
                        else           -> null
                    }
            }
        )
    }
}
```

### iOS (Swift / Kotlin)

> **Note:** iOS uses `@ExperimentalWorkerApi`. Work runs in the foreground by default; opt-in
> [BGTaskScheduler integration](#ios-bgtaskscheduler) enables background wake-ups.

```kotlin
// In your Kotlin iOS module — call from Swift AppDelegate before applicationDidFinishLaunching returns
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
            enablePersistence       = true,         // persist work across app restarts (default)
            enableBackgroundTasks   = false,         // set true to enable BGTaskScheduler
            bgProcessingTaskIdentifier = "",         // set when enableBackgroundTasks = true
        ),
    )
}
```

### Desktop (JVM)

```kotlin
fun main() {
    initializeWorkerDesktop(
        config = DesktopWorkManagerConfig(
            persistenceEnabled = true,           // persist work state across restarts (default)
            persistencePath = File("~/.my-app/worker-kmp"),  // where .properties files are stored
            maxConcurrentWorkers = 4,            // parallelism cap (default)
            constraintCheckIntervalMs = 5_000,   // constraint poll interval in ms (default)
        ),
        workerFactory = object : DesktopWorkerFactory {
            override fun create(workerClass: String, context: WorkerContext): CoroutineWorker =
                when (workerClass) {
                    "SyncWorker" -> SyncWorker(context)
                    else         -> error("Unknown worker: $workerClass")
                }
        },
    )
    val wm = PlatformWorkManager.instance
    // enqueue work ...
}
```

Work state persists across JVM restarts by default. Pending/enqueued items are restored when
`initializeWorkerDesktop()` is called again; RUNNING items are reset to ENQUEUED (they were
interrupted by the previous shutdown).

Set `persistenceEnabled = false` (or use `DesktopWorkManagerConfig.IN_MEMORY`) to disable
persistence entirely (useful in tests or CLI tools where state should not outlive the process).

### Web (JS/WasmJs)

> **Note:** Web uses `@ExperimentalWorkerApi` — work runs only while the page is open.

```kotlin
@OptIn(ExperimentalWorkerApi::class)
fun main() {
    // Optional: check support before init (always true in browsers/Node.js)
    if (!isWebWorkManagerSupported()) return

    initWebWorkManager(
        workerFactory = object : WebWorkerFactory {
            override fun create(workerClassName: String, context: WorkerContext): CoroutineWorker =
                when (workerClassName) {
                    "SyncWorker" -> SyncWorker(context)
                    else         -> error("Unknown worker: $workerClassName")
                }
        },
        config = WebWorkManagerConfig(
            constraintCheckIntervalMs = 5_000,        // how often to re-check unsatisfied constraints
            enablePersistence = true,                  // persist work state in IndexedDB
            persistenceDbName = "my-app-worker",       // override when sharing an origin with other apps
            enableBackgroundSync = false,              // set true to enable Browser Background Sync
            serviceWorkerScript = "/worker-kmp-sw.js", // path to bundled SW (when enableBackgroundSync)
        ),
    )
    val wm = PlatformWorkManager()
}
```

See [## Web Platform](#web-platform) for persistence, constraint, and progressive enhancement details.

## Compose Multiplatform UI

Add the Compose module to your dependencies:

```kotlin
commonMain.dependencies {
    implementation(libs.worker.compose)
}
```

Version catalog entry: `worker-compose = { module = "io.github.mobilebytelabs:worker-compose", version.ref = "worker" }`

### Inject WorkManager

```kotlin
CompositionLocalProvider(LocalWorkManager provides workManager) {
    MyApp()
}

// anywhere inside the tree
val wm = LocalWorkManager.current
```

### Ready-made components

#### WorkStatusChip

Coloured chip showing the current `WorkInfo.State`:

```kotlin
WorkStatusChip(state = info.state)
```

#### WorkProgressIndicator

Linear progress bar bound to a `WorkProgress` value; indeterminate while the progress is 0:

```kotlin
WorkProgressIndicator(
    progress = info.progress ?: WorkProgress.NONE,
    statusMessage = "Uploading…",
)
```

#### WorkInfoCard

Full work card with ID, status chip, progress bar, output data, and Cancel/Retry buttons:

```kotlin
WorkInfoCard(
    info = info,
    onCancel = { workManager.cancelWorkById(info.id) },
    onRetry  = { workManager.enqueue(originalRequest) },
)
```

#### WorkMonitorScreen

Full-screen list of active work items for a tag — observes the `WorkManager` flow automatically:

```kotlin
WorkMonitorScreen(
    tag      = "sync",
    onCancel = { info -> workManager.cancelWorkById(info.id) },
    onRetry  = { info -> workManager.enqueue(retryRequestFor(info)) },
)
```

#### WorkSchedulerScreen

Developer/admin form for scheduling work at runtime:

```kotlin
WorkSchedulerScreen(
    onSchedule = { request -> workManager.enqueue(request) },
)
```

## Testing

Add the test artifact to your test source set (use the same version as your other worker artifacts):

```kotlin
// gradle/libs.versions.toml — add to the [libraries] block shown in Setup
// worker-test = { module = "io.github.mobilebytelabs:worker-test", version.ref = "worker" }

commonTest.dependencies {
    implementation(libs.worker.test)
}
```

`TestWorkManager` is an in-memory implementation. Work is **never executed automatically** — you drive state transitions manually, so tests are deterministic and instant.

```kotlin
class SyncFeatureTest {
    private val workManager = TestWorkManager()

    @Test
    fun sync_enqueuesOneTimeRequest() = runTest {
        val request = OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker")
            .addTag("sync")
            .build()

        val id = workManager.enqueue(request)

        assertEquals(1, workManager.enqueuedRequests.size)
        assertEquals(WorkInfo.State.ENQUEUED, workManager.getWorkInfoById(id)?.state)
        assertTrue(workManager.hasWorkWithTag("sync"))
    }

    @Test
    fun sync_reportsSuccessOutput() = runTest {
        val id = workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker").build()
        )

        workManager.simulateWorkRunning(id)
        workManager.simulateWorkSuccess(id, workDataOf("synced_count" to 42))

        val info = workManager.getWorkInfoById(id)
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
        assertEquals(42, info?.outputData?.getInt("synced_count"))
    }

    @Test
    fun sync_retries_onTransientFailure() = runTest {
        val id = workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncWorker>("SyncWorker").build()
        )

        workManager.simulateWorkRetry(id)
        workManager.simulateWorkRetry(id)

        assertEquals(2, workManager.getWorkInfoById(id)?.runAttemptCount)
    }

    @AfterTest
    fun tearDown() {
        workManager.reset()
    }
}
```

### TestWorkManager API

| Method | Description |
|---|---|
| `simulateWorkRunning(id)` | Transitions work to `RUNNING` |
| `simulateWorkSuccess(id, outputData?)` | Transitions work to `SUCCEEDED` with optional output |
| `simulateWorkFailure(id)` | Transitions work to `FAILED` |
| `simulateWorkProgress(id, progress)` | Updates progress without changing state |
| `simulateWorkRetry(id)` | Transitions back to `ENQUEUED`, increments `runAttemptCount` |
| `reset()` | Clears all state — call in `@BeforeTest` / `@AfterTest` |
| `enqueuedRequests` | Ordered list of all enqueued requests |
| `uniqueWorkNames` | Names registered via `enqueueUniquePeriodicWork` |
| `lastEnqueuedRequest` | The most recently enqueued request |
| `hasWorkWithTag(tag)` | Returns true if any work with this tag was enqueued |
| `workCountWithTag(tag)` | Count of enqueued work items with this tag |

## Desktop Persistence

Work state is persisted to disk by default (`persistenceEnabled = true`) using `.properties` files under `~/.worker-kmp`. This means:

- **Restart resilience** — pending work survives JVM process restarts; it is restored and re-executed automatically when `initializeWorkerDesktop()` is called again.
- **RUNNING → ENQUEUED** — work that was RUNNING when the previous JVM exited is reset to ENQUEUED (it was interrupted mid-execution).
- **Terminal state cleanup** — SUCCEEDED, FAILED, and CANCELLED items are immediately deleted from disk; the persistence directory only contains pending/active work.

### Configuration — `DesktopWorkManagerConfig`

| Field | Default | Description |
|---|---|---|
| `persistenceEnabled` | `true` | When `true`, work state is written to `persistencePath`. Set `false` (or use `IN_MEMORY` preset) to disable all file I/O. |
| `persistencePath` | `~/.worker-kmp` | Directory for `.properties` files. Override per-app to avoid collisions between multiple JVM processes. |
| `maxConcurrentWorkers` | `4` | Maximum number of workers running concurrently. |
| `constraintCheckIntervalMs` | `5000` | Polling interval (ms) when waiting for constraints to be satisfied. |

Use `DesktopWorkManagerConfig.IN_MEMORY` as a convenience preset for tests or CLI tools.

## iOS BGTaskScheduler

The `worker-ios` module targets `iosArm64` and `iosSimulatorArm64`. Work runs while the app is in the foreground by default. Opt-in BGTaskScheduler integration (iOS 13+) allows constrained work to be woken up by the OS in the background.

### Configuration — `IosWorkManagerConfig`

| Field | Default | Description |
|---|---|---|
| `enableBackgroundTasks` | `false` | When `true`, registers a `BGProcessingTask` handler and schedules background wake-ups for constrained work. |
| `bgProcessingTaskIdentifier` | `""` | The task identifier to register. Must match `Info.plist → BGTaskSchedulerPermittedIdentifiers`. |
| `enablePersistence` | `true` | When `true`, work state is written to `NSUserDefaults` so pending/running work survives app restarts. |
| `persistenceKey` | `"worker-kmp-ios"` | NSUserDefaults key. Override when sharing a suite with other extensions to avoid collisions. |

### Setup — BGTaskScheduler

**Step 1 — add the identifier to `Info.plist`:**

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER).worker-kmp</string>
</array>
```

**Step 2 — enable background tasks in `initIosWorkManager` (Kotlin, called from Swift AppDelegate):**

```kotlin
@OptIn(ExperimentalWorkerApi::class)
fun initApp() {
    initIosWorkManager(
        workerFactory = myWorkerFactory,
        config = IosWorkManagerConfig(
            enableBackgroundTasks      = true,
            bgProcessingTaskIdentifier = "${bundleId}.worker-kmp",
        ),
    )
}
```

> **Important:** `initIosWorkManager` must be called before `applicationDidFinishLaunching` returns — the same timing requirement as `BGTaskScheduler.register(...)` in Swift.

### How it works

1. When `IosWorkManager` has constrained work waiting (e.g. `NetworkType.CONNECTED`), it submits a `BGProcessingTaskRequest` with matching `requiresNetworkConnectivity` / `requiresExternalPower` flags.
2. iOS wakes the app in the background when the constraints are met.
3. The registered `BGProcessingTask` handler calls `runPendingWork()` which re-enqueues any orphaned work items.
4. A polling loop inside `awaitConstraintsSatisfied` runs as a foreground fallback — no BGTask means work still executes when the app comes to the foreground.

### NSUserDefaults persistence

When `enablePersistence = true` (the default), `IosWorkManager` automatically:

- **Saves** each `WorkInfo` to NSUserDefaults on every state transition.
- **Restores** persisted work on the next app launch — `RUNNING` items are restored as `ENQUEUED` (they were interrupted mid-flight by an app kill).
- **Deletes** terminal items (`SUCCEEDED`, `FAILED`, `CANCELLED`) to keep NSUserDefaults clean.

## Web Platform

The `worker-web` module targets Kotlin/JS (`jsMain`) and Kotlin/Wasm (`wasmJsMain`). Work runs in the current page or Node.js process. Opt-in [Browser Background Sync](#browser-background-sync) allows constrained work to survive tab focus changes on supported browsers.

### Progressive enhancement

```kotlin
import io.github.mobilebytelabs.worker.web.isWebWorkManagerSupported

if (isWebWorkManagerSupported()) {
    initWebWorkManager(workerFactory)
} else {
    // Very old browsers or SSR environments — skip background work.
}
```

`isWebWorkManagerSupported()` always returns `true` in a real browser or Node.js runtime and `false` on the JVM target (which is used only for unit tests).

### Configuration — `WebWorkManagerConfig`

| Field | Default | Description |
|---|---|---|
| `constraintCheckIntervalMs` | `5_000` | How often to re-evaluate unsatisfied constraints (ms). Lower values are more responsive but burn more CPU on battery-constrained devices. |
| `enablePersistence` | `true` | When `true`, work state is written to IndexedDB so pending and running work survives page reloads. |
| `persistenceDbName` | `"worker-kmp"` | IndexedDB database name. Override when multiple apps share the same origin to avoid key collisions. |
| `enableBackgroundSync` | `false` | When `true`, registers a Background Sync tag so constrained work can be woken by the browser even across tab focus changes. Requires a Service Worker at `serviceWorkerScript`. |
| `serviceWorkerScript` | `"/worker-kmp-sw.js"` | Path to the worker-kmp Service Worker file served by your host. Only used when `enableBackgroundSync = true`. |

```kotlin
WebWorkManagerConfig(
    constraintCheckIntervalMs = 2_000,
    enablePersistence         = true,
    persistenceDbName         = "my-app",
    enableBackgroundSync      = true,         // opt-in
    serviceWorkerScript       = "/sw.js",     // default: "/worker-kmp-sw.js"
)
```

### IndexedDB persistence

When `enablePersistence = true` (the default), `WebWorkManager` automatically:

- **Saves** each `WorkInfo` to IndexedDB on every state transition.
- **Restores** persisted work on the next page load — `RUNNING` items are restored as `ENQUEUED` (they were interrupted mid-flight by the reload).
- **Deletes** terminal items (`SUCCEEDED`, `FAILED`, `CANCELLED`) to keep the database clean.

Set `enablePersistence = false` for SSR, Node.js samples, or any environment where IndexedDB is unavailable.

### Constraint system

The JS actual evaluates constraints using browser APIs:

| Constraint | Browser API |
|---|---|
| `setRequiredNetworkType(CONNECTED)` | `navigator.onLine` |
| `setRequiresBatteryNotLow(true)` | `navigator.getBattery()` — passes when `level > 0.20` |
| `setRequiresCharging(true)` | `navigator.getBattery()` — passes when `charging == true` |
| `setRequiresStorageNotLow(true)` | `navigator.storage.estimate()` — passes when free quota > 5 MB |

Conservative fallback: when a browser API is unavailable (e.g. Battery Status API in Firefox), the constraint **passes** rather than blocking work indefinitely.

### Online/offline event-driven re-evaluation

The constraint loop does not just poll every `constraintCheckIntervalMs`. It also wires `window.addEventListener("online", …)` and `window.addEventListener("offline", …)` so that a transition from offline → online wakes up constrained work **immediately** instead of waiting up to 5 seconds.

### Browser Background Sync

Enable opt-in Background Sync so the browser can wake constrained work even when your tab loses focus:

```kotlin
initWebWorkManager(
    workerFactory = ...,
    config = WebWorkManagerConfig(
        enableBackgroundSync = true,
        serviceWorkerScript  = "/worker-kmp-sw.js",
    ),
)
```

**Setup — serve the Service Worker file**

Copy the bundled template to your web server root:

```
cmp-worker-web/src/jsMain/resources/worker-kmp-sw.js  →  public/worker-kmp-sw.js
```

Or generate it programmatically (e.g. from a Kotlin/JS `main()` that writes to disk during build):

```kotlin
import io.github.mobilebytelabs.worker.web.backgroundSyncServiceWorkerScript

val swContent: String = backgroundSyncServiceWorkerScript()
// write to file or serve dynamically
```

**How it works**

1. When `WebWorkManager` encounters constrained work (e.g. `NetworkType.CONNECTED`) it registers a
   sync tag `worker-kmp-{uuid}` via `ServiceWorkerRegistration.sync.register(tag)`.
2. The browser fires a `sync` event in the Service Worker when connectivity is restored.
3. The Service Worker posts `{ type: 'WORKER_KMP_SYNC', tag }` to all open window clients.
4. `backgroundSyncFlow` receives the message and signals `awaitConstraintsSatisfied` to re-evaluate.

The polling fallback and online/offline watcher remain active — Background Sync is an additional
wake-up source, not a replacement.

**Browser support for Background Sync**

| Browser | Background Sync | Notes |
|---|---|---|
| Chrome / Edge 49+ | ✅ | Full SyncManager support |
| Firefox | ❌ | Falls back to polling + online watcher |
| Safari | ❌ | Falls back to polling + online watcher |
| Node.js / WasmJs | ❌ | No SyncManager — fallback always active |

`isBackgroundSyncSupported()` returns `false` when `SyncManager` is unavailable; worker-kmp
silently falls back to polling + online-event wake-up in that case.

### Unique periodic work policies

All three `ExistingPeriodicWorkPolicy` values are supported:

| Policy | Web behavior |
|---|---|
| `KEEP` | If active work with the same unique name exists, returns its existing ID — no second job is created. |
| `REPLACE` | Cancels the existing job and enqueues the new request. |
| `UPDATE` | Cancels the existing job and enqueues the new request (same semantics as `REPLACE` for the coroutine-based web scheduler). |

### Browser compatibility

| Browser | Minimum version | Notes |
|---|---|---|
| Chrome / Edge | 66+ | Full support — all constraint APIs available |
| Firefox | 64+ | Battery Status API unavailable — constraint passes conservatively |
| Safari | 15.4+ | Battery Status API unavailable — constraint passes conservatively |
| Node.js | 18+ | IndexedDB unavailable — set `enablePersistence = false` |

### Sample

See `cmp-worker-sample/src/jsMain/kotlin/…/WebSampleMain.kt` for a runnable Node.js demo covering all scenarios. Run with:

```bash
./gradlew :cmp-worker-sample:jsNodeRun
```

## Artifacts

All modules are published together under `io.github.mobilebytelabs` on Maven Central with a shared version.
Check the badge at the top of this README for the latest release.

| Artifact | Use |
|---|---|
| `worker-kmp` | Common API — include in `commonMain` |
| `worker-android` | Android platform implementation |
| `worker-ios` | iOS platform implementation (iosArm64, iosSimulatorArm64) |
| `worker-desktop` | JVM desktop implementation |
| `worker-web` | JS/browser + Node.js implementation |
| `worker-compose` | Compose Multiplatform integration — `LocalWorkManager`, `WorkStatusChip`, `WorkProgressIndicator`, `WorkInfoCard`, `WorkMonitorScreen`, `WorkSchedulerScreen` |
| `worker-test` | Test utilities (`TestWorkManager`) |

## Migration

Coming from AndroidX WorkManager, iOS BGTaskScheduler, or a custom web polling loop?
See **[MIGRATION.md](MIGRATION.md)** for step-by-step migration instructions and an API quick-reference.

## License

```
Copyright 2024 Mobile Byte Labs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0
```
