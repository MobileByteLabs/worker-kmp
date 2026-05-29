# cmp-worker-compose-all

All-in-one Compose Multiplatform worker library — **one dep gets you everything**.

For consumer apps targeting Android + Desktop (JVM) + iOS + Web (wasmJs / js)
with Compose Multiplatform, this module replaces the 5+ individual `worker-*`
dependencies with a single Maven artifact: `io.github.mobilebytelabs:worker-compose-all`.

```kotlin
// build.gradle.kts (commonMain)
implementation("io.github.mobilebytelabs:worker-compose-all:$workerKmpVersion")

// You also need Koin's Compose helpers separately:
implementation("io.insert-koin:koin-compose:$koinVersion")
```

That single dep transitively brings in:

| Brought in | What you get |
|---|---|
| `cmp-worker-kmp` | Core `WorkManager` API, `CoroutineWorker`, `WorkInfo`, `WorkRequest`, `Constraints`, periodic + one-time work, tags, retry policies, progress reporting |
| `cmp-worker-compose` | `WorkManagerProvider`, `LocalWorkManager`, `WorkInfoCard`, `WorkMonitorScreen`, `WorkSchedulerScreen`, `BackgroundCapabilitiesBanner`, `WorkCountBadge` |
| `cmp-worker-koin` | `workKoinModule(...)` — wires the WorkManager backend + your `WorkerRegistry` into the DI graph |
| `cmp-worker-store5` | `StoreBackedWorker<K, V>` — drives a Store5 store from a background worker (fetch + write-back + observable cache) |
| `cmp-worker-android` (androidMain) | `androidWorkManagerFactory(context)` + `WorkerKmpComposeActivity` + `WorkerKmpStarterApplication` |
| `cmp-worker-desktop` (desktopMain / jvmMain) | `desktopWorkManagerFactory()` + `launchDesktopWorkerApp(...)` |
| `cmp-worker-ios` (iosMain) | `iosWorkManagerFactory()` + `workerKmpMainViewController(...)` |
| `cmp-worker-web` (jsMain + wasmJsMain) | `webWorkManagerFactory()` + `launchWebWorkerApp(...)` |

## When to use this vs. the individual modules

- ✅ **You're building a Compose Multiplatform app** targeting most of {Android, Desktop, iOS, Web}: use this bundle. One dep, simpler `build.gradle.kts`.
- ❌ **You only need one platform** (e.g. pure-Android app with traditional View XML — no Compose): use `io.github.mobilebytelabs:worker-android` directly. Don't pull in Compose deps you won't use.
- ❌ **You need an opt-in extra** like `cmp-worker-desktop-daemon`, `cmp-worker-web-push`, or `cmp-worker-storeflow`: add those as additional explicit deps alongside (this bundle covers the core surface but not the optional extras).

## Per-platform launcher pattern

After you add the bundle dep, your per-platform launcher files are still ≤10 lines each (the OS bootstrap floor):

### Android (`androidMain`)

```kotlin
// AndroidManifest.xml: <application android:name=".App"> + <activity android:name=".MainActivity"/>

class App : WorkerKmpStarterApplication() {
    override fun koinModules() = appKoinModules(androidWorkManagerFactory(this))
}

class MainActivity : WorkerKmpComposeActivity({ AppContent() })
```

### Desktop (`jvmMain`)

```kotlin
fun main() = launchDesktopWorkerApp(
    title = "My App",
    koinModules = { appKoinModules(desktopWorkManagerFactory()) },
) { AppContent() }
```

### iOS (`iosMain` — exposed to Swift via Kotlin/Native interop)

```kotlin
fun MainViewController(): UIViewController = workerKmpMainViewController(
    koinModules = { appKoinModules(iosWorkManagerFactory()) },
) { AppContent() }
```

### Web (`wasmJsMain` — paired with `wasmJsMain/resources/index.html` `<div id="composeCanvas">`)

```kotlin
fun main() = launchWebWorkerApp(
    canvasElementId = "composeCanvas",
    koinModules = { appKoinModules(webWorkManagerFactory()) },
) { AppContent() }
```

## Eliminating these launchers entirely

If you want **zero per-platform Kotlin code** in your consumer app, see the planned `worker-kmp-app-plugin` (Gradle + KSP) at `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-app-plugin/` — generates all launcher files at build time from a single `@WorkerKmpApp` annotation in commonMain. That epic builds on top of this bundle.

## See also

- `samples/cmp-worker-sample-compose-store/` — canonical CMP demo. Uses this bundle as its single worker-kmp dep; runs on all 5 platforms.
- Root [`README.md`](../README.md) — overall library overview.
