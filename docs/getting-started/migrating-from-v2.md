# Migrating from worker-kmp 2.x to 3.0.0

This document covers the source-incompatible API changes between worker-kmp **2.1.0** and
**3.0.0-alpha00.X** (Phase 0 deep refactor, clean-break release — no v2 BC story).

> **Important**: worker-kmp 3.x is a clean-break release. The legacy `PlatformWorkManager`
> global slot and `initializeWorkerXxx(...)` side-effecting init functions were removed
> outright in v3.0.0-alpha00.X — there is no `@Deprecated` grace period. v2.x call sites
> need a one-time edit per platform.

If you only follow one section: see [§ 2. New consumer pattern](#2-new-consumer-pattern).

---

## 1. `workKoinModule` val → function (with factory)

**What changed**: the previously `val`-shaped Koin module is now a `fun` with three
parameters — [`WorkerConfig`](cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/config/WorkerConfig.kt),
[`WorkerRegistry`](cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/registry/WorkerRegistry.kt),
and a mandatory [`WorkManagerFactory`](cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/PlatformWorkManager.kt)
supplied by your chosen per-platform module.

**Why**: the v3 API surface treats configuration + worker registration + platform-backend
selection as commonMain-discoverable inputs, not platform-init side effects. A single
`startKoin { modules(workKoinModule(...)) }` call replaces the v2 two-step dance.

### Before — v2.1.0

```kotlin
// 1. Pre-startKoin platform init (separate per platform)
initializeWorkerAndroid(context, workerFactory)

// 2. Then Koin
startKoin {
    modules(workKoinModule, appModule)
}
```

### After — v3.0.0-alpha00.X

```kotlin
// Single startKoin call wires everything
startKoin {
    androidContext(this@App)
    modules(
        workKoinModule(
            config = WorkerConfig(/* logLevel, observers, androidConfig, ... */),
            workers = workerRegistry {
                register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
            },
            factory = androidWorkManagerFactory(this@App),
        ),
        appModule,
    )
}
```

---

## 2. New consumer pattern

The factory you pass to `workKoinModule(...)` is the per-platform backend selector.
Pick the one matching the platform module on your classpath:

| Platform module | Factory function | Source set |
| --- | --- | --- |
| `cmp-worker-android` | `androidWorkManagerFactory(context)` | androidMain |
| `cmp-worker-ios` | `iosWorkManagerFactory()` (requires `@OptIn(ExperimentalWorkerApi::class)`) | iosMain |
| `cmp-worker-desktop` | `desktopWorkManagerFactory()` | jvmMain |
| `cmp-worker-web` | `webWorkManagerFactory()` (requires `@OptIn(ExperimentalWorkerApi::class)`) | jsMain + wasmJsMain |

### Android (`androidMain`)

```kotlin
import io.github.mobilebytelabs.worker.android.androidWorkManagerFactory
import io.github.mobilebytelabs.worker.config.AndroidWorkerConfig
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.koin.workKoinModule
import io.github.mobilebytelabs.worker.registry.workerRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(
                workKoinModule(
                    config = WorkerConfig(
                        androidConfig = AndroidWorkerConfig(useReflectionFactory = true),
                    ),
                    workers = workerRegistry {
                        register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
                    },
                    factory = androidWorkManagerFactory(this@MyApp),
                ),
                appModule,
            )
        }
    }
}
```

### iOS (`iosMain`)

```kotlin
@OptIn(ExperimentalWorkerApi::class)
fun startApp() {
    startKoin {
        modules(
            workKoinModule(
                config = WorkerConfig(
                    iosConfig = IosWorkerConfig(
                        enableBackgroundTasks = true,
                        bgProcessingTaskIdentifier = "com.example.sync",
                    ),
                ),
                workers = workerRegistry {
                    register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
                },
                factory = iosWorkManagerFactory(),
            ),
        )
    }
}
```

### Desktop (`jvmMain`)

```kotlin
fun main() {
    startKoin {
        modules(
            workKoinModule(
                config = WorkerConfig(
                    desktopConfig = DesktopWorkerConfig(persistenceEnabled = false),
                ),
                workers = workerRegistry {
                    register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
                },
                factory = desktopWorkManagerFactory(),
            ),
        )
    }
    // Resolve from Koin:
    val wm = GlobalContext.get().get<WorkManager>()
}
```

### Web (`jsMain` / `wasmJsMain`)

```kotlin
@OptIn(ExperimentalWorkerApi::class)
fun main() {
    if (!isWebWorkManagerSupported()) return
    startKoin {
        modules(
            workKoinModule(
                config = WorkerConfig(
                    webConfig = WebWorkerConfig(
                        enablePersistence = false,
                        enableBackgroundSync = true,
                        serviceWorkerScript = "/sw.js",
                    ),
                ),
                workers = workerRegistry {
                    register<SyncWorker> { ctx -> SyncWorker(ctx) }
                },
                factory = webWorkManagerFactory(),
            ),
        )
    }
}
```

---

## 3. What was removed

The following v2.x APIs were **deleted outright** in v3.0.0-alpha00.X. No grace period, no
`@Deprecated` wrappers — call sites must migrate to the new factory pattern above.

| Removed API | Replacement |
| --- | --- |
| `PlatformWorkManager` (expect object) | `WorkManagerFactory` (fun interface) — wired via `workKoinModule(factory = ...)` |
| `PlatformWorkManager.configure(impl)` | `workKoinModule(factory = xxxWorkManagerFactory())` |
| `PlatformWorkManager()` (invoke) | `koin.get<WorkManager>()` |
| `initializeWorkerAndroid(context, factory)` | `androidWorkManagerFactory(context)` |
| `initIosWorkManager(factory, config)` | `iosWorkManagerFactory()` |
| `initializeWorkerDesktop(config, factory)` | `desktopWorkManagerFactory()` |
| `initWebWorkManager(factory, config)` | `webWorkManagerFactory()` |
| `WorkManagerProvider(workManager = PlatformWorkManager())` default | `WorkManagerProvider(workManager = koinInject<WorkManager>())` — explicit `WorkManager` arg required |

---

## 4. New commonMain types

Five new commonMain types ship in v3.0.0-alpha00.X. All five enable the factory pattern
without forcing consumers to import Android/iOS/Desktop/Web types into shared code.

### `WorkerConfig` (extended)

```kotlin
public data class WorkerConfig(
    public val logLevel: LogLevel = LogLevel.WARN,
    public val defaultRetryConfig: RetryConfig = RetryConfig.DEFAULT,
    public val observers: List<WorkObserver> = emptyList(),
    public val androidConfig: AndroidWorkerConfig = AndroidWorkerConfig(),
    public val iosConfig: IosWorkerConfig = IosWorkerConfig(),
    public val desktopConfig: DesktopWorkerConfig = DesktopWorkerConfig(),
    public val webConfig: WebWorkerConfig = WebWorkerConfig(),
)
```

Unified configuration object — replaces ad-hoc per-platform config classes at the
commonMain layer. Per-platform sub-configs (`androidConfig`, `iosConfig`, etc.) carry the
platform-specific knobs that v2.x exposed via separate `IosWorkManagerConfig` /
`DesktopWorkManagerConfig` / `WebWorkManagerConfig` types. Those legacy types remain in
their respective platform modules for direct use (the iOS/Desktop/Web `WorkManager`
constructors still accept them), but commonMain consumers should now reach for the
unified `WorkerConfig`.

### `WorkManagerFactory` (fun interface)

```kotlin
public fun interface WorkManagerFactory {
    public fun create(config: WorkerConfig, workers: WorkerRegistry): WorkManager
}
```

The factory contract that per-platform modules implement. Consumers don't write factories
directly — they pass the result of `xxxWorkManagerFactory(...)` (from the platform module)
to `workKoinModule(factory = ...)`.

### `PlatformContext` (unchanged shape, role refined)

```kotlin
public expect class PlatformContext
```

`PlatformContext` remains a commonMain `expect class` with empty `actual` declarations on
JVM/iOS/JS/WasmJs. It is no longer used to carry Android `Context` across module
boundaries — that's now the responsibility of `androidWorkManagerFactory(context)`, which
takes the Android `Context` as a direct platform-typed parameter inside `androidMain`.

**Note on the originally-planned Android typealias**: the v3.0.0-alpha00 plan called for
`PlatformContext` to become an `actual typealias` to `android.content.Context` once
`cmp-worker-android` migrated. That plan didn't survive contact with Kotlin Multiplatform's
`expect`/`actual` module rules — `expect` and `actual` declarations must live in the same
gradle module, and `cmp-worker-android` is a separate module from `cmp-worker-kmp` (the
latter has no Android target). The factory pattern above achieves the same goal (no Android
imports in commonMain consumer code) without forcing a same-module `expect`/`actual` pair.

### `AndroidWorkerConfig`, `IosWorkerConfig`, `DesktopWorkerConfig`, `WebWorkerConfig`

Per-platform config sub-objects living in `io.github.mobilebytelabs.worker.config`. Each
mirrors the legacy platform-module config type (`IosWorkManagerConfig` etc.) field-by-field
but lives in commonMain so consumers declare it from shared code.

```kotlin
// commonMain — author once, applies to every platform
val config = WorkerConfig(
    logLevel = LogLevel.DEBUG,
    androidConfig = AndroidWorkerConfig(useReflectionFactory = false),
    iosConfig = IosWorkerConfig(enableBackgroundTasks = true, bgProcessingTaskIdentifier = "..."),
    desktopConfig = DesktopWorkerConfig(persistenceEnabled = true),
    webConfig = WebWorkerConfig(enableBackgroundSync = true),
)
```

### `WorkerRegistry` (unchanged from v3.0.0-alpha00)

```kotlin
public class WorkerRegistry internal constructor() {
    public inline fun <reified T : CoroutineWorker> register(
        noinline factory: (WorkerContext) -> T,
    )
    public fun register(className: String, factory: (WorkerContext) -> CoroutineWorker)
}

public fun workerRegistry(block: WorkerRegistry.() -> Unit): WorkerRegistry
```

Type-safe registry of worker factories keyed by simple class name. Locked after Koin loads
it — late registrations throw `WorkerRegistryAlreadyLoadedException` (defends against T23
per [security.md](../operations/security.md)). Each platform factory threads the registry through a per-platform
adapter (`WorkerRegistryAdapter` / `WorkerRegistryIosAdapter` / `ChainedDesktopWorkerFactory`
/ `WorkerRegistryWebAdapter`) so the consumer's commonMain registration becomes the single
source of worker instances.

On Android the registry is consulted FIRST with a reflection fallback when
`WorkerConfig.androidConfig.useReflectionFactory = true` (default) — this preserves the v2
reflection path during incremental migration. Set to `false` for strict registry mode.

On Desktop the registry is consulted FIRST with a reflection (`Class.forName`) fallback
unconditionally. iOS and Web have no reflection available and throw on unregistered
workers — consumers MUST register every iOS/Web worker in the `workerRegistry { ... }` block.

---

## 5. Compose integration

The `WorkManagerProvider` default parameter (`workManager = PlatformWorkManager()`) was
removed. Pass the [`WorkManager`](cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/WorkManager.kt)
explicitly — typically from your Koin graph:

### Before — v2.1.0

```kotlin
WorkManagerProvider {
    MyScreen()
}
```

### After — v3.0.0-alpha00.X

```kotlin
// commonMain composable (uses Koin Compose)
WorkManagerProvider(workManager = koinInject<WorkManager>()) {
    MyScreen()
}

// or Android-specific
WorkManagerProvider(workManager = remember { KoinJavaComponent.getKoin().get() }) {
    MyScreen()
}
```

---

## 6. Gradle plugin migrator (Phase 12, alpha08)

A future `cmp-worker-migrate` Gradle plugin (slated for v3.0.0-alpha08) will automate the
call-site rewrites covered above plus the per-actual init removals. Until then, the
changes are 1-line-per-platform manual edits.

---

## Quick checklist

- [ ] Replace `initializeWorkerAndroid(ctx, factory)` + `modules(workKoinModule, ...)`
      with `modules(workKoinModule(config, workers, factory = androidWorkManagerFactory(ctx)))`
- [ ] Replace `initIosWorkManager(factory, config)` + `modules(workKoinModule, ...)`
      with `modules(workKoinModule(config, workers, factory = iosWorkManagerFactory()))`
- [ ] Replace `initializeWorkerDesktop(config, factory)` + `modules(workKoinModule, ...)`
      with `modules(workKoinModule(config, workers, factory = desktopWorkManagerFactory()))`
- [ ] Replace `initWebWorkManager(factory, config)` + `modules(workKoinModule, ...)`
      with `modules(workKoinModule(config, workers, factory = webWorkManagerFactory()))`
- [ ] Replace any `PlatformWorkManager()` lookup with `koin.get<WorkManager>()`
- [ ] Replace any `KmpAndroidWorkerFactory` / `IosWorkerFactory` / `DesktopWorkerFactory` /
      `WebWorkerFactory` consumer impl with a `workerRegistry { register<T> { ... } }` block
- [ ] Update `WorkManagerProvider { ... }` → `WorkManagerProvider(workManager = ...) { ... }`
- [ ] Move per-platform config (`IosWorkManagerConfig`, etc.) from the platform module's
      direct `WorkManager` constructor → into `WorkerConfig.{ios|desktop|web|android}Config`
      so commonMain declares it once
