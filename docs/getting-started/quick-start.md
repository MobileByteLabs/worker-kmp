---
title: "Quick start"
description: "Smallest end-to-end worker-kmp example — define a CoroutineWorker, wire DI, enqueue."
---

# Quick Start — first worker in 60 seconds

This walk-through gets a background sync worker running on **all four platforms** (Android, iOS, Desktop, Web) with the same commonMain code. We assume you already have a Kotlin Multiplatform project skeleton — if not, see [Installation](installation.md) first.

> **Goal:** by the end of this page you'll have a `DataSyncWorker` defined in `commonMain`, wired through Koin, scheduled via `WorkManager.enqueue(...)`, and emitting state changes you can observe from any platform target.

---

## Step 1 — Define a worker in `commonMain`

The same worker class will execute on every platform you target. No `expect` / `actual` needed for the worker logic itself.

```kotlin
// commonMain/kotlin/com/example/work/DataSyncWorker.kt
package com.example.work

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext

class DataSyncWorker(
    context: WorkerContext,
    private val api: ApiClient,
) : CoroutineWorker(context) {

    override suspend fun doWork(): WorkResult {
        val endpoint = inputData.getString("endpoint")
            ?: return WorkResult.failure("missing endpoint")

        return runCatching { api.sync(endpoint) }.fold(
            onSuccess = { WorkResult.success(workDataOf("synced" to it.count)) },
            onFailure = { WorkResult.retry(it.message) },
        )
    }
}
```

> Screenshot placeholder: `docs/_images/quick-start/step1-worker.png`

Key points:
- `CoroutineWorker` is suspendable — call any suspending API from `doWork()`.
- Return `WorkResult.success` / `failure` / `retry` — semantics are identical on every platform.
- `inputData` is a typed key-value bag — see [WorkData docs in Home](../Home.md).

---

## Step 2 — Wire up Koin in `commonMain`

worker-kmp ships a single Koin DI module. Call it once at app start with your platform factory. **Everything else — work scheduling, persistence, OS APIs — is handled internally by the library.**

```kotlin
// commonMain/kotlin/com/example/di/AppModule.kt
import io.github.mobilebytelabs.worker.koin.workKoinModule
import io.github.mobilebytelabs.worker.koin.workerRegistry
import io.github.mobilebytelabs.worker.WorkerConfig

fun appKoinModule(platformFactory: WorkManagerFactory) = module {
    single { ApiClient() }
}

fun startWorkerKoin(platformFactory: WorkManagerFactory) {
    startKoin {
        modules(
            workKoinModule(
                config = WorkerConfig(logLevel = LogLevel.INFO),
                workers = workerRegistry {
                    register<DataSyncWorker> { ctx -> DataSyncWorker(ctx, get()) }
                },
                factory = platformFactory,
            ),
            appKoinModule(platformFactory),
        )
    }
}
```

> Screenshot placeholder: `docs/_images/quick-start/step2-koin.png`

Each platform's entry-point passes the platform factory:

```kotlin
// Android (Application.onCreate)
startWorkerKoin(androidWorkManagerFactory(this))

// iOS (commonMain main viewModel or AppDelegate Kotlin bridge)
startWorkerKoin(iosWorkManagerFactory())

// Desktop (main.kt)
startWorkerKoin(desktopWorkManagerFactory())

// Web (commonMain main + JS / Wasm entry)
startWorkerKoin(webWorkManagerFactory())
```

The platform factory is the **only** per-platform code you write. Everything else — including foreground-service registration, `BGTaskScheduler` task IDs, daemon installation, and Service Worker registration — is handled by the library.

---

## Step 3 — Schedule work

From any commonMain composable, ViewModel, or coroutine:

```kotlin
class SyncViewModel(private val workManager: WorkManager) {
    fun startSync() {
        workManager.enqueue(oneTimeWorkRequest<DataSyncWorker> {
            setInputData(workDataOf("endpoint" to "/api/sync"))
            setConstraints(Constraints {
                setRequiredNetworkType(NetworkType.CONNECTED)
                setRequiresBatteryNotLow(true)
            })
            setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30.seconds)
            addTag("sync")
        })
    }
}
```

> Screenshot placeholder: `docs/_images/quick-start/step3-enqueue.png`

The `Constraints { … }` DSL produces a builder whose semantics are identical on every platform. On Android they map to `WorkManager.Constraints`; on iOS to `BGTaskScheduler` predicates; on Desktop to in-process reachability + battery checks; on Web to `navigator.connection` + Service Worker `sync` events.

---

## Step 4 — Observe state changes

The `Flow<List<WorkInfo>>` API works the same on every platform:

```kotlin
class SyncScreen(workManager: WorkManager) {
    val syncStatus: Flow<WorkInfoState> =
        workManager.getWorkInfosByTag("sync")
            .map { infos -> infos.firstOrNull()?.state ?: WorkInfoState.UNKNOWN }
}
```

Or use the Compose Multiplatform UI components shipped in `cmp-worker-compose`:

```kotlin
@Composable
fun SyncDashboard() {
    Column {
        WorkSchedulerScreen(onWorkScheduled = { id -> /* navigate to detail */ })
        WorkMonitorScreen(tag = "sync")
    }
}
```

> Screenshot placeholder: `docs/_images/quick-start/step4-monitor.png`

`WorkMonitorScreen` shows real-time state transitions, retry attempts, and progress events for every worker matching the tag. Drop it anywhere — it's the same composable on Android, iOS, Desktop, and Web.

---

## What's next

- **Platform-specific setup** (Manifest / Info.plist / installer entries): [Android](../platform-support/android.md) · [iOS](../platform-support/ios.md) · [Desktop](../platform-support/desktop.md) · [Web](../platform-support/web.md)
- **Long-running tasks** that must keep the OS awake: [Foreground Tasks](../features/foreground-tasks.md)
- **Telemetry** — observe every worker lifecycle event from your OTel / Sentry / Firebase Perf SDK: [Observers](../features/observers.md)
- **Web Push** server setup for true-background Web workers: [Web Push Server](../features/web-push-server.md)
- **Migrating from v2.x**: [migrating-from-v2.md](migrating-from-v2.md)
