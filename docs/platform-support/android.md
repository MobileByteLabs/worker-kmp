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

## Platform factory

```kotlin
// Android Application.onCreate
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(
                workKoinModule(
                    config = WorkerConfig(logLevel = LogLevel.INFO),
                    workers = workerRegistry { register<MyWorker> { ctx -> MyWorker(ctx, get()) } },
                    factory = androidWorkManagerFactory(this@App),
                ),
            )
        }
    }
}
```

`androidWorkManagerFactory(context)` handles:
- `WorkManager.getInstance(context)` lookup
- Mapping commonMain `Constraints` → `androidx.work.Constraints`
- Reflection or registry-based worker instantiation (configurable via
  `WorkerConfig.androidConfig.useReflectionFactory`; defaults to `true` for v2 compat)
- Wiring `WorkObserver` SAMs through `WorkManager.getWorkInfosByTagFlow(...)`

## Permissions you may also need

- `POST_NOTIFICATIONS` (API 33+) if your foreground worker shows a notification
- `INTERNET` (almost always, for network-constrained workers)
- `ACCESS_NETWORK_STATE` for `NetworkType.CONNECTED` / `UNMETERED` constraints

## See also

- [Platform API Matrix](platform-api-matrix.md) — full per-API support
- [True Background Matrix](true-background-matrix.md) — Android's guarantees vs other platforms
- [Foreground Tasks](../features/foreground-tasks.md) — per-`foregroundServiceType` setup
