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


Key points:
- `CoroutineWorker` is suspendable — call any suspending API from `doWork()`.
- Return `WorkResult.success` / `failure` / `retry` — semantics are identical on every platform.
- `inputData` is a typed key-value bag — see [WorkData docs in Home](../Home.md).

---

## Step 2 — Declare your workers + wire Koin in `commonMain`

worker-kmp v4 is a **single-API commonMain** setup. You (1) declare your workers with one
annotation, and (2) call the codegen-emitted `WorkerKmpAuto.install()` from your shared init.
The per-platform factory selection, worker registry, and Koin wiring are all generated for you —
**you write zero per-platform worker code.** (Applying the `io.github.mobilebytelabs.worker-app`
Gradle plugin — see [Installation](installation.md) / [Convention Plugin](convention-plugin.md) —
is what runs the codegen.)

```kotlin
// commonMain — declare the workers once; the annotation drives codegen for every platform.
import io.github.mobilebytelabs.worker.app.WorkerKmpWorkers

@WorkerKmpWorkers(workers = [DataSyncWorker::class])
fun workerDeclarations() = Unit
```

```kotlin
// commonMain — your app's shared init (the ONE function every platform entry point calls).
import cmp.shared.generated.WorkerKmpAuto   // codegen'd from @WorkerKmpWorkers into your module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initApp(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)                 // Android binds androidContext(this@App) here
        modules(module { single { ApiClient() } })
    }
    WorkerKmpAuto.install()                   // ONE line — wires workers on Android/iOS/Desktop/Web
}
```

Each platform entry point just calls `initApp()` — **no per-platform worker code:**

```kotlin
// androidMain — the app class only supplies the Koin context:
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initApp { androidContext(this@MyApp) }
    }
}
// desktopMain:        fun main() { initApp(); /* compose window */ }
// wasmJsMain / jsMain: fun main() { initApp(); /* compose viewport */ }
// iosMain:            ViewController { initApp(); /* compose UIViewController */ }
```

> **Placement matters:** put `WorkerKmpAuto.install()` in the shared commonMain init (as above),
> **not** in a single platform's app class — otherwise the other platforms compile and run but
> schedule no workers. See the [Single-API Guide](../wiki/single-api-guide.md) for the full
> multiplatform-placement rationale.

Foreground-service registration, `BGTaskScheduler` task IDs, daemon installation, and Service
Worker registration are all handled by the library — you don't write any of it.

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


`WorkMonitorScreen` shows real-time state transitions, retry attempts, and progress events for every worker matching the tag. Drop it anywhere — it's the same composable on Android, iOS, Desktop, and Web.

---

## What's next

- **Platform-specific setup** (Manifest / Info.plist / installer entries): [Android](../platform-support/android.md) · [iOS](../platform-support/ios.md) · [Desktop](../platform-support/desktop.md) · [Web](../platform-support/web.md)
- **Long-running tasks** that must keep the OS awake: [Foreground Tasks](../features/foreground-tasks.md)
- **Telemetry** — observe every worker lifecycle event from your OTel / Sentry / Firebase Perf SDK: [Observers](../features/observers.md)
- **Web Push** server setup for true-background Web workers: [Web Push Server](../features/web-push-server.md)
