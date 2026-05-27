# Migrating from worker-kmp 2.x to 3.0.0

This document covers the source-incompatible API changes between worker-kmp **2.1.0** and
**3.0.0-alpha00**, plus the staged migration plan for the larger zero-init refactor that
lands across the v3.0.0-alpha00.X follow-up alphas.

If you only follow one section: see [§ 1. `workKoinModule` val → function](#1-workkoinmodule-val--function).

---

## 1. `workKoinModule` val → function

**What changed**: the previously `val`-shaped Koin module is now a `fun` with two optional
parameters carrying the new [`WorkerConfig`](cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/config/WorkerConfig.kt)
and [`WorkerRegistry`](cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/registry/WorkerRegistry.kt).

**Why**: the v3 API surface treats configuration + worker registration as
commonMain-discoverable inputs, not platform-init side effects. The function form lets us
accept the new types without breaking the singleton invariant.

**Migration**: append `()` parens at every call site. Existing v2.x callers receive
identical behaviour because both parameters have safe defaults (`WorkerConfig()` and an
empty `workerRegistry { }`).

### Before — v2.1.0

```kotlin
startKoin {
    modules(workKoinModule, appModule)
}
```

### After — v3.0.0-alpha00

```kotlin
startKoin {
    modules(workKoinModule(), appModule)
}
```

### After — v3.0.0-alpha00 with new config + registry

```kotlin
import io.github.mobilebytelabs.worker.config.WorkerConfig
import io.github.mobilebytelabs.worker.registry.workerRegistry

startKoin {
    modules(
        workKoinModule(
            config = WorkerConfig(logLevel = LogLevel.DEBUG),
            workers = workerRegistry {
                register<SyncWorker> { ctx -> SyncWorker(ctx, get()) }
            },
        ),
        appModule,
    )
}
```

---

## 2. Per-platform init still required in v3.0.0-alpha00

In v3.0.0-alpha00, the per-platform `initializeWorkerXxx(...)` calls remain mandatory —
the new `workKoinModule(config, workers)` signature is the carrier for future zero-init
wiring, but the actuals continue to populate the existing `PlatformWorkManager` slot.

Concrete impact: **no code change required** for existing per-platform init blocks.

```kotlin
// androidMain — UNCHANGED
initializeWorkerAndroid(context, workerFactory)
startKoin { modules(workKoinModule(), appModule) } // <-- only this line changed (parens added)

// iosMain — UNCHANGED
initIosWorkManager(workerFactory, config)
startKoin { modules(workKoinModule(), appModule) }
```

The full zero-init refactor lands per-actual across the v3.0.0-alpha00.X follow-ups:

| Follow-up | Actual replaced | Result |
| --- | --- | --- |
| `v3.0.0-alpha00.1` | `AndroidWorkManager` via `androidx.startup` auto-init | `initializeWorkerAndroid` becomes a no-op stub |
| `v3.0.0-alpha00.2` | `IosWorkManager` consumes `WorkerConfig.iosConfig` | `initIosWorkManager` becomes a no-op stub |
| `v3.0.0-alpha00.3` | `DesktopWorkManager` consumes `WorkerConfig.desktopConfig` | `initializeWorkerDesktop` becomes a no-op stub |
| `v3.0.0-alpha00.4` | `WebWorkManager` consumes `WorkerConfig.webConfig` | `initWebWorkManager` becomes a no-op stub |

Once all four actuals migrate, `WorkerConfig` becomes the sole configuration surface
and per-platform init is fully eliminated.

---

## 3. New types in commonMain

Three new commonMain types ship in v3.0.0-alpha00. None of them break existing v2.x code —
they're additions that future zero-init wiring depends on.

### `WorkerConfig`

```kotlin
public data class WorkerConfig(
    public val logLevel: LogLevel = LogLevel.WARN,
    public val defaultRetryConfig: RetryConfig = RetryConfig.DEFAULT,
    public val observers: List<WorkObserver> = emptyList(),
)
```

Unified configuration object — replaces ad-hoc per-platform config classes at the
commonMain layer. Per-platform sub-configs (`androidConfig`, `iosConfig`, etc.) join in
the v3.0.0-alpha00.X follow-ups.

### `WorkerRegistry`

```kotlin
public class WorkerRegistry internal constructor() {
    public inline fun <reified T : CoroutineWorker> register(
        noinline factory: (WorkerContext) -> T,
    )
    public fun register(className: String, factory: (WorkerContext) -> CoroutineWorker)
}

public fun workerRegistry(block: WorkerRegistry.() -> Unit): WorkerRegistry
```

Type-safe registry of worker factories keyed by simple class name (matching the
`OneTimeWorkRequestBuilder` / `PeriodicWorkRequestBuilder` convention) — replaces
the per-platform `WorkerFactory` interfaces with a single commonMain registry.
Uses `KClass::simpleName` for Kotlin/JS portability (Kotlin/JS doesn't support
`qualifiedName` at runtime). **Locked** after Koin loads it — late registrations throw
[`WorkerRegistryAlreadyLoadedException`](cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/registry/WorkerRegistry.kt)
(defends against T23 per `SECURITY.md`).

### `PlatformContext` (expect class)

```kotlin
public expect class PlatformContext
```

Future bridge for `android.content.Context` and equivalents — currently a sentinel
class on JVM / iOS / JS / WasmJs. Becomes an `actual typealias` to
`android.content.Context` once cmp-worker-android is refactored
(v3.0.0-alpha00.1 follow-up).

---

## 4. Gradle plugin migrator (Phase 12, alpha08)

A future `cmp-worker-migrate` Gradle plugin (slated for v3.0.0-alpha08) will automate
the call-site rewrites covered above plus the per-actual init removals. Until then,
the changes above are 1-line manual edits.

---

## 5. Backward-compatibility test gate

The BC test module (`cmp-worker-bc-test`) verifies that legacy v2.1.0 behaviour
continues to work via the BCV public API surface — see [`BACKWARD_COMPATIBILITY.md`](BACKWARD_COMPATIBILITY.md).
CI fails on divergence. The deprecation grace window for v2.x APIs runs through v3.x.

---

## Quick checklist

- [ ] Update `modules(workKoinModule, ...)` → `modules(workKoinModule(), ...)` everywhere
- [ ] (Optional) Adopt `WorkerConfig` for log-level / retry / observers
- [ ] (Optional) Adopt `workerRegistry { register<T> { ... } }` block — opt-in until the
      per-actual refactors land
- [ ] Keep your existing `initializeWorkerXxx(...)` calls — they are still required in
      v3.0.0-alpha00
- [ ] Watch v3.0.0-alpha00.X follow-up alpha release notes for per-actual zero-init wiring
