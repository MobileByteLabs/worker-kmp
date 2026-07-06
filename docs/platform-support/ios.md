---
title: "iOS"
description: "worker-kmp on iOS via BGTaskScheduler + URLSession — what works, what doesn't, foreground vs background time constraints."
---

# iOS

worker-kmp on iOS is built on `BGTaskScheduler` (iOS 13+) — the OS-managed background
task framework. Three task types are supported:

- **Processing** (`BGProcessingTask`) — long-running, opaque OS-cadence (typically minutes to hours)
- **App Refresh** (`BGAppRefreshTask`) — short refresh tasks (~30 sec wall-clock)
- **Continued Processing** (`BGContinuedProcessingTask`, iOS 17+) — UI-promoted long tasks

## Requirements

- iOS 13.0+
- Xcode 15+ (for iOS 17 `BGContinuedProcessingTask` types)

## Dependency

```kotlin
// commonMain
api(libs.worker.kmp)
api(libs.worker.koin)

// iosMain
api(libs.worker.ios)
```

## Info.plist additions

Two keys MUST be set in your iOS app's `Info.plist`:

```xml
<!-- Info.plist -->
<key>UIBackgroundModes</key>
<array>
    <string>processing</string>
    <string>fetch</string>
</array>

<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>com.example.app.sync</string>
    <string>com.example.app.fetch</string>
    <!-- Add one entry per task identifier you'll use -->
</array>
```

The task identifiers in `BGTaskSchedulerPermittedIdentifiers` MUST match the
`taskIdentifier` you pass through `IosWorkManagerConfig.taskIdentifiers`. Mismatches
cause the OS to reject submissions silently.

## iOS setup (v4 single-API)

Worker wiring lives in your **commonMain** `initApp()` via `WorkerKmpAuto.install()` (see the
[Quick start](../getting-started/quick-start.md)); the generated iOS installer selects
`iosWorkManagerFactory()` for you. The iOS entry point (the `ViewController` Kotlin bridge, or an
`AppDelegate`-invoked function) just calls `initApp()`:

```kotlin
// iosMain — e.g. the ComposeUIViewController bridge
fun MainViewController() = ComposeUIViewController {
    initApp()               // commonMain initApp calls WorkerKmpAuto.install()
    App()
}
```

iOS-specific `WorkerConfig` — notably the `BGTaskScheduler` task identifiers (which MUST also be
listed in `Info.plist` under `BGTaskSchedulerPermittedIdentifiers`) — is supplied via a Koin
binding, see [Configuration](../wiki/single-api-guide.md#configuration):

```kotlin
single {
    WorkerConfig(
        logLevel = LogLevel.INFO,
        iosConfig = IosWorkManagerConfig(
            taskIdentifiers = mapOf(
                "DataSyncWorker" to "com.example.app.sync",
                "FetchWorker" to "com.example.app.fetch",
            ),
            defaultTaskType = IosTaskType.PROCESSING,
        ),
    )
}
```

## When the OS will run your task

`BGTaskScheduler` makes **no wall-clock guarantee**. Tasks run when the system decides:
device idle, plugged in, on Wi-Fi, low system load. Typical observed cadence:

- Processing — minutes to several hours after submission
- AppRefresh — within ~30 minutes of submission while app is in memory
- ContinuedProcessing (iOS 17+) — promoted to UI while running; longest runtime

If your task is time-sensitive, use foreground execution via
[Foreground Tasks](../features/foreground-tasks.md) instead.

## Persistence

iOS uses `NSUserDefaults` for the work queue. State survives app launches but is wiped
on uninstall. See `IosWorkManagerConfig.persistenceSuiteName` to override the suite.

## See also

- [Platform API Matrix](platform-api-matrix.md) — per-iOS-version API coverage
- [True Background Matrix](true-background-matrix.md) — iOS guarantees vs other platforms
- [Foreground Tasks](../features/foreground-tasks.md) — when not to use BGTaskScheduler
