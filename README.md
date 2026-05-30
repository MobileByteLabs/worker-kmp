# worker-kmp

> **WorkManager for Kotlin Multiplatform + Compose Multiplatform.** Write workers once in `commonMain`. Out-of-box support for Android, iOS, Desktop, and Web. One Koin module call to wire it up.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/worker-kmp.svg)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/worker-kmp)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7f52ff.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285f4.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

## Out-of-box platform support

| Platform | Mechanism | True background? |
|---|---|---|
| **Android** (API 21+) | `androidx.work.WorkManager` + `JobScheduler` | ✓ |
| **iOS** (13+) | `BGTaskScheduler` (+ ContinuedProcessing on iOS 17+) | ✓ |
| **Desktop** (JVM 11+) | OS-scheduler daemon (Windows / macOS / Linux) | ✓ |
| **Web** (browsers) | Service Worker + Web Push (Chrome/Firefox/Safari 16.4+/Edge) | ✓ |

No per-platform consumer init code. Same `WorkManager` API everywhere.

## Quick start

### 1. Add the dependency (commonMain)

**Building a Compose Multiplatform app (Android + Desktop + iOS + Web)?** Use the all-in-one bundle — **one dep** brings in core + UI + Koin + Store5 + all 4 platform factories + launchers:

```kotlin
// gradle/libs.versions.toml
worker-kmp = "3.0.0"

// commonMain build.gradle.kts
dependencies {
    implementation("io.github.mobilebytelabs:worker-compose-all:$workerVersion")
    implementation(libs.koin.compose)                  // Koin's Compose helpers
}
```

See [`cmp-worker-compose-all/README.md`](cmp-worker-compose-all/README.md) for the full per-platform launcher pattern (3-5 lines each).

**Want zero per-platform Kotlin files?** Use the [`worker-kmp-app` Gradle plugin](cmp-worker-app-plugin/README.md):

```kotlin
plugins {
    id("io.github.mobilebytelabs.worker-app") version "$workerVersion"
}
```

Then annotate two commonMain functions:

```kotlin
@WorkerKmpApp(title = "My App", iosBundleId = "com.example.myapp")
fun appKoinModules(factory: WorkManagerFactory) = ...

@WorkerKmpAppContent
@Composable fun AppContent() = ...
```

Plugin codegens every per-platform launcher (Android Application/Activity/manifest,
JVM `fun main()`, iOS `MainViewController` + xcodegen project, wasmJs
`fun main()` + `index.html`) at build time. Consumer source tree: zero per-platform
Kotlin files.

**Only need a subset** (e.g. pure-Android no-Compose)? The individual `worker-*` artifacts remain available:

```kotlin
dependencies {
    api(libs.worker.kmp)
    api(libs.worker.koin)
    implementation(libs.worker.compose)  // Compose Multiplatform UI (optional)
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
        return runCatching { api.sync(endpoint) }.fold(
            onSuccess = { WorkResult.success() },
            onFailure = { WorkResult.retry(it.message) },
        )
    }
}
```

### 3. Wire up Koin — one call, every platform (commonMain)

```kotlin
startKoin {
    modules(
        workKoinModule(
            config = WorkerConfig(logLevel = LogLevel.INFO),
            workers = workerRegistry {
                register<DataSyncWorker> { ctx -> DataSyncWorker(ctx, get()) }
            },
            factory = androidWorkManagerFactory(this@Application),
            //      = iosWorkManagerFactory()
            //      = desktopWorkManagerFactory()
            //      = webWorkManagerFactory()
        ),
        appModule,
    )
}
```

### 4. Schedule + observe (commonMain)

```kotlin
val workManager: WorkManager = get()
workManager.enqueue(oneTimeWorkRequest<DataSyncWorker> {
    setConstraints(Constraints { setRequiredNetworkType(NetworkType.CONNECTED) })
    setInputData(workDataOf("endpoint" to "/api/sync"))
})
workManager.getWorkInfosByTag("sync").collect { infos -> /* update UI */ }
```

## Compose Multiplatform UI — out-of-box

```kotlin
@Composable
fun WorkDashboard() {
    WorkSchedulerScreen(onWorkScheduled = { /* … */ })
    WorkMonitorScreen(tag = "sync")
}
```

## Modules

| Module | Purpose |
|---|---|
| `cmp-worker-kmp` | Core API — `WorkManager`, `CoroutineWorker`, `Constraints`, `WorkResult` |
| `cmp-worker-koin` | Koin DI — single `workKoinModule(...)` |
| `cmp-worker-compose` | Compose Multiplatform UI |
| `cmp-worker-test` | `TestWorkManager` + helpers |
| `cmp-worker-android` · `-ios` · `-desktop` · `-web` | Platform actuals (auto-wired) |
| `cmp-worker-store5` · `-storeflow` | Optional: Store5 bridge + offline-first patterns |
| `cmp-worker-desktop-daemon` | Optional: Desktop OS-scheduler daemon for true background |
| `cmp-worker-web-push` | Optional: Web Push universal background |

All published under `io.github.mobilebytelabs` on Maven Central.

## Samples

| Sample | Description |
|---|---|
| [`samples/cmp-worker-sample-compose-store/`](samples/cmp-worker-sample-compose-store/) | Reference Compose Multiplatform app showing all worker-kmp patterns in a single-module setup. |
| [`samples/kmp-project-template/`](samples/kmp-project-template/) | Production-shape integration of worker-kmp into [openMF/kmp-project-template](https://github.com/openMF/kmp-project-template) — clones the template, adds a `sync/` module mirroring Now in Android's architecture using `worker-compose-all` + Koin. PR-ready for upstream. See [`samples/kmp-project-template/sync/README.md`](samples/kmp-project-template/sync/README.md). |

## Documentation

Full docs live in [`docs/`](docs/Home.md) and mirror the [GitHub Wiki](https://github.com/MobileByteLabs/worker-kmp/wiki):

- 📚 [Installation](docs/getting-started/installation.md) · [Quick Start](docs/getting-started/quick-start.md) · [Convention Plugin (build-logic)](docs/getting-started/convention-plugin.md) · [Migrating from v2](docs/getting-started/migrating-from-v2.md)
- 📱 Platform setup: [Android](docs/platform-support/android.md) · [iOS](docs/platform-support/ios.md) · [Desktop](docs/platform-support/desktop.md) · [Web](docs/platform-support/web.md)
- 🛠️ Features: [Foreground Tasks](docs/features/foreground-tasks.md) · [Observers / Telemetry](docs/features/observers.md) · [Web Push Server](docs/features/web-push-server.md)
- 🔒 Operations: [Security](docs/operations/security.md) · [Performance](docs/operations/performance.md)
- 📦 [Release process](docs/release/release-process.md)

## License

Apache 2.0. © MobileByteLabs.
