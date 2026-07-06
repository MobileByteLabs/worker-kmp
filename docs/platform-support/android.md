---
title: "Android"
description: "worker-kmp on Android is a thin adapter over androidx.work.WorkManager + JobScheduler — OS-scheduled, reboot-persistent, survives process death."
---

# Android

worker-kmp on Android is a thin adapter over `androidx.work.WorkManager` (+ `JobScheduler`).
**OS-scheduled, persistent across reboots, and survives app process death** — the
WorkManager you already know, exposed through the commonMain API.

## Requirements

- `minSdk` ≥ 21 (Android 5.0 Lollipop)
- AndroidX WorkManager 2.9+ (transitively included by `cmp-worker-android`)
- AndroidX Startup (transitively included; used for zero-config initialization)

## Dependency

```kotlin
// commonMain
api(libs.worker.kmp)
api(libs.worker.koin)

// androidMain
api(libs.worker.android)
```

## Manifest additions

For most workers, no Manifest changes are required — `cmp-worker-android` declares the
necessary `<provider>` and `<receiver>` entries via manifest-merger.

For **foreground service workers** (long-running tasks), add the following:

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- API 34+: declare service type -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<!-- … add other types your foreground workers use -->

<application>
    <!-- The library's foreground service is auto-declared via manifest-merger.
         Consumers may override foregroundServiceType per build variant: -->
    <service
        android:name="androidx.work.impl.foreground.SystemForegroundService"
        tools:replace="android:foregroundServiceType"
        android:foregroundServiceType="dataSync" />
</application>
```

See [Foreground Tasks](../features/foreground-tasks.md) for the full per-type matrix.

## Android setup (v4 single-API)

The worker wiring lives in your **commonMain** shared init via `WorkerKmpAuto.install()` (see the
[Quick start](../getting-started/quick-start.md) / [Single-API Guide](../wiki/single-api-guide.md)).
The Android app class's only worker-related job is to bind the Koin `androidContext` — the
generated Android installer reads the `Application` from that binding and selects
`androidWorkManagerFactory` automatically:

```kotlin
// androidMain — no worker code; just supply the Koin Android context.
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initApp { androidContext(this@App) }   // commonMain initApp calls WorkerKmpAuto.install()
    }
}
```

Under the hood the generated installer uses `androidWorkManagerFactory(context)`, which handles:
- `WorkManager.getInstance(context)` lookup
- Mapping commonMain `Constraints` → `androidx.work.Constraints`
- Registry-based worker instantiation from the `@WorkerKmpWorkers` codegen
- Wiring `WorkObserver` SAMs through `WorkManager.getWorkInfosByTagFlow(...)`

## Permissions you may also need

- `POST_NOTIFICATIONS` (API 33+) if your foreground worker shows a notification
- `INTERNET` (almost always, for network-constrained workers)
- `ACCESS_NETWORK_STATE` for `NetworkType.CONNECTED` / `UNMETERED` constraints

## See also

- [Platform API Matrix](platform-api-matrix.md) — full per-API support
- [True Background Matrix](true-background-matrix.md) — Android's guarantees vs other platforms
- [Foreground Tasks](../features/foreground-tasks.md) — per-`foregroundServiceType` setup
