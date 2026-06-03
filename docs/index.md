---
title: "worker-kmp"
description: "WorkManager for Kotlin Multiplatform — one commonMain API across Android, iOS, Desktop, and Web. Maven Central, Apache 2.0."
---

# worker-kmp

> **WorkManager for Kotlin Multiplatform.** One commonMain API. Out-of-box support for Android, iOS, Desktop, and Web.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/worker-kmp.svg)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/worker-kmp)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7f52ff.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285f4.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

## Why worker-kmp

| Without worker-kmp | With worker-kmp |
|---|---|
| Write 4 different scheduling implementations (WorkManager, BGTaskScheduler, JVM coroutines, Web Workers) | Write **one** `CoroutineWorker` subclass in commonMain |
| 4 different per-platform init calls before `startKoin` | One `workKoinModule(config, workers, factory)` — done |
| Different retry/constraint/observability semantics per platform | One `Constraints` builder, one `RetryConfig`, one `WorkObserver` SAM — works everywhere |
| Per-platform UI for work monitoring | `WorkSchedulerScreen` + `WorkMonitorScreen` in Compose Multiplatform |

## Out-of-box platform support

| Platform | Mechanism | True background? | Foreground? |
|---|---|:---:|:---:|
| **Android** (API 21+) | `androidx.work.WorkManager` + `JobScheduler` · `ForegroundService` for foreground | ✓ OS-scheduled, persistent | ✓ |
| **iOS** (13+) | `BGTaskScheduler` (Processing + AppRefresh + 17+: ContinuedProcessing · ≤16: BGProcessing + UNNotification shim) | ✓ OS-managed, opaque cadence | ✓ |
| **Desktop** (JVM 11+) | OS-scheduler daemon (Windows Task Scheduler · macOS launchd · Linux systemd-user) · in-process tray for foreground | ✓ Survives app close + reboot | ✓ |
| **Web** (browsers) | Service Worker + Web Push (Chrome/Firefox/Safari 16.4+/Edge) | ✓ Server-cron-driven | ⚠ SW `showNotification` analog |

See [True Background Matrix](platform-support/true-background-matrix.md) for full per-platform-variant detail. See [Cross-platform parity audit](operations/cross-platform-parity-audit.md) for the file:line evidence ledger behind every cell.

## Quick start

### 1. Add the dependency

> 📦 **Latest version:** see the [![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/worker-kmp.svg?label=worker-kmp)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/worker-kmp) badge above. Replace `LATEST` in the snippets below with that string.

```kotlin
// gradle/libs.versions.toml
worker-kmp = "LATEST"  // ← put the current published version here

// commonMain build.gradle.kts
dependencies {
    api(libs.worker.kmp)
    api(libs.worker.koin)
    // Optional add-ons:
    implementation(libs.worker.compose)    // Compose Multiplatform UI
    implementation(libs.worker.store5)     // Store5 bridge
    implementation(libs.worker.storeflow)  // Offline-first patterns
}
```

### 2. Define a worker (commonMain)

```kotlin
class DataSyncWorker(
    context: WorkerContext,
    private val api: ApiClient,
) : CoroutineWorker(context) {
    override suspend fun doWork(): WorkResult {
        val endpoint = inputData.getString("endpoint") ?: return WorkResult.failure()
        return runCatching { api.sync(endpoint) }
            .fold(
                onSuccess = { WorkResult.success() },
                onFailure = { WorkResult.retry(it.message) },
            )
    }
}
```

### 3. Wire up Koin (commonMain — one call, every platform)

```kotlin
startKoin {
    modules(
        workKoinModule(
            config = WorkerConfig(logLevel = LogLevel.INFO),
            workers = workerRegistry {
                register<DataSyncWorker> { ctx -> DataSyncWorker(ctx, koin.get()) }
            },
            factory = androidWorkManagerFactory(this@Application),  // or iosWorkManagerFactory() / desktopWorkManagerFactory() / webWorkManagerFactory()
        ),
        appModule,
    )
}
```

### 4. Schedule + observe (commonMain)

```kotlin
val workManager: WorkManager = get()
val id = workManager.enqueue(oneTimeWorkRequest<DataSyncWorker> {
    setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
    setInputData(workDataOf("endpoint" to "/api/sync"))
})
workManager.getWorkInfosByTag("sync").collect { infos ->
    infos.forEach { println("${it.id}: ${it.state}") }
}
```

That's the entire setup. Same code shape on Android, iOS, Desktop, Web.

## Compose Multiplatform UI components

```kotlin
@Composable
fun WorkDashboard() {
    WorkSchedulerScreen(onWorkScheduled = { id -> /* … */ })
    WorkMonitorScreen(tag = "sync")
}
```

[Full Compose API →](features/observers.md)

## Library modules

All modules ship under the same version. Use the [![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/worker-kmp.svg?label=latest)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/worker-kmp) version in your `libs.versions.toml`.

| Module | Coordinates | Purpose |
|---|---|---|
| `cmp-worker-kmp` | `io.github.mobilebytelabs:worker-kmp` | Core API — `WorkManager`, `CoroutineWorker`, types |
| `cmp-worker-koin` | `io.github.mobilebytelabs:worker-koin` | Koin DI module — `workKoinModule(...)` |
| `cmp-worker-compose` | `io.github.mobilebytelabs:worker-compose` | Compose Multiplatform UI |
| `cmp-worker-test` | `io.github.mobilebytelabs:worker-test` | Test utilities — `TestWorkManager` |
| `cmp-worker-android` | `io.github.mobilebytelabs:worker-android` | Android actual (auto-wired) |
| `cmp-worker-ios` | `io.github.mobilebytelabs:worker-ios` | iOS actual (auto-wired) |
| `cmp-worker-desktop` | `io.github.mobilebytelabs:worker-desktop` | Desktop actual (auto-wired) |
| `cmp-worker-web` | `io.github.mobilebytelabs:worker-web` | Web actual (auto-wired) |
| `cmp-worker-store5` | `io.github.mobilebytelabs:worker-store5` | Store5 bridge (optional) |
| `cmp-worker-storeflow` | `io.github.mobilebytelabs:worker-storeflow` | Offline-first patterns (optional) |
| `cmp-worker-desktop-daemon` | `io.github.mobilebytelabs:worker-desktop-daemon` | Desktop OS-scheduler daemon (optional) |
| `cmp-worker-web-push` | `io.github.mobilebytelabs:worker-web-push` | Web Push universal background (optional) |

## Where next

- 📚 [Installation](getting-started/installation.md) — Gradle + Maven setup with Compose Multiplatform
- 🚀 [Quick Start](getting-started/quick-start.md) — first worker in 60 seconds
- 🧩 [Convention Plugin (build-logic)](getting-started/convention-plugin.md) — copy-and-adopt Kotlin source for projects that share worker-kmp wiring across modules via a build-logic convention plugin (worker-kmp does not ship one — see the page for why)
- 📱 Platform setup: [Android](platform-support/android.md) · [iOS](platform-support/ios.md) · [Desktop](platform-support/desktop.md) · [Web](platform-support/web.md)
- 🛠️ Features: [Foreground Tasks](features/foreground-tasks.md) · [Telemetry / Observers](features/observers.md) · [Web Push Server](features/web-push-server.md)
- 🔒 [Security](operations/security.md) · [Performance](operations/performance.md) · [Cross-platform parity audit](operations/cross-platform-parity-audit.md) — per-feature evidence ledger, refreshed nightly
- 📦 [Release process](release/release-process.md)

## License

Apache 2.0. © MobileByteLabs.
