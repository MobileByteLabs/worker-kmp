# worker-kmp

A Kotlin Multiplatform background task scheduler — the `WorkManager` API you know from Android, available on every platform.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/worker-kmp)](https://central.sonatype.com/search?q=io.github.mobilebytelabs)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-Apache--2.0-green)](LICENSE)

## Platform Support

| Platform | Module | Min Version |
|---|---|---|
| Android | `worker-android` | API 21 |
| iOS | `worker-ios` | iOS 14.0 |
| Desktop (JVM) | `worker-desktop` | JDK 11 |
| Web (JS) | `worker-web` | Chrome/Node |
| Compose Multiplatform | `worker-compose` | — |
| All (common API) | `worker-kmp` | — |

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

// iOS
initIosWorkManager(workerFactory)

// Desktop (JVM)
initializeWorkerDesktop()

// Web (JS)
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
    .setRetryConfig(
        RetryConfig(
            maxAttempts = 3,
            initialDelay = 5.seconds,
            backoffPolicy = BackoffPolicy.EXPONENTIAL,
            multiplier = 2.0,
            maxDelay = 60.seconds,
        )
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
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresCharging(true)
    .setRequiresDeviceIdle(true)
    .setRequiresBatteryNotLow(true)
    .setRequiresStorageNotLow(true)
    .build()

val request = OneTimeWorkRequestBuilder<BackupWorker>("BackupWorker")
    .setConstraints(constraints)
    .build()
```

`NetworkType` options: `NOT_REQUIRED`, `CONNECTED`, `UNMETERED`, `NOT_ROAMING`, `METERED`.

## Retry and Backoff

```kotlin
val retryConfig = RetryConfig(
    maxAttempts    = 5,
    initialDelay   = 2.seconds,
    maxDelay       = 5.minutes,
    backoffPolicy  = BackoffPolicy.EXPONENTIAL,
    multiplier     = 2.0,
)

val request = OneTimeWorkRequestBuilder<NetworkWorker>("NetworkWorker")
    .setRetryConfig(retryConfig)
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

```kotlin
// In your Kotlin iOS module
fun initApp() {
    initIosWorkManager(
        workerFactory = object : IosWorkerFactory {
            override fun create(workerClassName: String, context: WorkerContext): CoroutineWorker? =
                when (workerClassName) {
                    "SyncWorker" -> SyncWorker(context)
                    else         -> null
                }
        }
    )
}
```

### Desktop (JVM)

```kotlin
fun main() {
    initializeWorkerDesktop()
    val wm = PlatformWorkManager.instance
    // enqueue work ...
}
```

### Web (JS)

```kotlin
fun main() {
    initWebWorkManager(
        workerFactory = object : WebWorkerFactory {
            override fun create(workerClassName: String, context: WorkerContext): CoroutineWorker? =
                when (workerClassName) {
                    "SyncWorker" -> SyncWorker(context)
                    else         -> null
                }
        }
    )
    val wm = PlatformWorkManager.instance
}
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
| `worker-compose` | Compose Multiplatform integration |
| `worker-test` | Test utilities (`TestWorkManager`) |

## License

```
Copyright 2024 Mobile Byte Labs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0
```
