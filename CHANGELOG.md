# Changelog

All notable changes to worker-kmp will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] - 2026-05-31 (WIP)

### Added — NEW `cmp-worker-scheduler` module

High-level `WorkScheduler` Koin façade for scheduling background sync work. Library
responsibility = **when work runs** (cadence: daily/periodic/at-instant/exact). Consumer
responsibility = **what the work does** (extend `AbstractDataSyncWorker` for sync; use raw
`WorkManager.enqueue(oneTimeWorkRequest<YourWorker> { ... })` for custom workers).
Consumers depending on `cmp-worker-compose-all` get the scheduling APIs for free with no extra dep.

- **`WorkScheduler` interface** (7 methods, all sync-scheduling): `enqueueDataSync`,
  `scheduleDailyDataSync(timeOfDay)`, `schedulePeriodicDataSync(interval)`,
  `scheduleDataSyncAt(instant)`, `scheduleDataSyncAtExact(instant)`,
  `observeWork(name)`, `cancelWork(name)`. Every scheduling method accepts
  `payload: WorkData = workDataOf()`.
- **`DefaultWorkScheduler`** — cross-platform impl using PeriodicWorkRequest +
  setInitialDelay + enqueueUniquePeriodicWork(KEEP) for periodic + OneTimeWorkRequest for at-time.
- **Android exact-tier**: `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` actual.
  Requires host app to declare `SCHEDULE_EXACT_ALARM` permission in AndroidManifest.xml
  (Android 12+). Runtime check `AlarmManager.canScheduleExactAlarms()`; on denial, falls
  back to WorkManager flex-window with `android.util.Log.w` log.
- **iOS exact-tier**: `BGProcessingTaskRequest` with `earliestBeginDate = instant.toNSDate()`
  submitted to `BGTaskScheduler.sharedScheduler`. Best-effort per Apple's policy (OS may
  defer); host app `Info.plist` must register identifier in `BGTaskSchedulerPermittedIdentifiers`.
- **Desktop**: in-process via `ScheduledExecutorService.schedule(...)`; lost on JVM restart.
  v1.1 follow-up will layer on cmp-worker-desktop-daemon when daemon is execute-capable.
- **Web (wasmJs + js)**: `setTimeout(handler, delayMs)` actual (in-tab only). v1.1 follow-up
  adds Service Worker periodicSync (Chrome-only, requires bundler config).
- **NiA-shaped sync contracts**: `Synchronizer` + `Syncable` (with 2-arg
  `syncWith(synchronizer, payload: WorkData)` overload) + `NetworkChange` + `changeListSync`
  extension (delta APIs) + `snapshotSync` sibling extension (snapshot APIs like Frankfurter
  / WorldBank) — all in `io.github.mobilebytelabs.worker.scheduler.sync`. `ChangeListVersions`
  (Long-typed for snapshotSync epochSeconds), `SyncManager` observer interface,
  `SyncStatePersister` (in-memory MutableStateFlow-backed default impl).
- **`AbstractDataSyncWorker(ctx, syncables: List<Syncable>, persister: SyncStatePersister)`**
  base class — consumer's `DataSyncWorker` becomes a 3-line subclass listing its specific
  Syncable repos. doWork iterates
  `coroutineScope { syncables.map { async { it.syncWith(...) } }.awaitAll() }`.

### NOT in the library (consumer responsibility)

Notification rendering, foreground notification UI, custom-worker classes — the library
schedules; the consumer owns what runs. To schedule a non-sync worker, use raw WorkManager:

```kotlin
val request = oneTimeWorkRequest<MyNotificationWorker> {
    setInputData(workDataOf("title" to "Hi", "body" to "Hello"))
    setInitialDelay(15.minutes)
}
workManager.enqueue(request)
```

### Sample-side impact

`samples/kmp-project-template/sync/` keeps these consumer-owned pieces: adopter-specific
`OfflineFirstCurrencyRepository` + `OfflineFirstMacroIndicatorsRepository` + 3-line
`DataSyncWorker` subclass + `SyncModule` (Koin wiring) + sample's own `NotificationWorker`
+ `NotificationContent` + per-platform `renderNotification` actuals. Cross-module demo
`feature/loans/LoanReminderUseCase` uses `WorkScheduler` for daily sync + `WorkManager`
directly for the reminder notification.

PR: [MobileByteLabs/worker-kmp#27](https://github.com/MobileByteLabs/worker-kmp/pull/27).

### Backward compatibility

Pre-release library — no prior consumers of `cmp-worker-scheduler`. Existing sample-side
consumers must update imports (`org.mifos.sync.*` → `io.github.mobilebytelabs.worker.scheduler.*`
for sync types; notification types stay in `org.mifos.sync.*`).

---

## [Unreleased]

### Added

- `samples/kmp-project-template/` — production-shape integration of worker-kmp into a
  verbatim clone of [openMF/kmp-project-template](https://github.com/openMF/kmp-project-template).
  New `sync/` module mirroring Now in Android's architecture using `worker-compose-all` + Koin.
  `WorkerComposeConventionPlugin` added to the clone's `build-logic/`. PR-ready for upstream —
  see [`samples/kmp-project-template/PR_README.md`](samples/kmp-project-template/PR_README.md).

## [3.0.0] - 2026-05-29

### ⚠ Breaking Changes

- **kotlinx-datetime 0.6.2 → 0.8.0** (transitive bump via `cmp-worker-kmp`'s `api(libs.kotlinx.datetime)`). kotlinx-datetime 0.7.0 renamed `dayOfMonth → day`, `monthNumber → month`, and changed `TimeZone.UTC` identifier from `"Z"` to `"UTC"`. `Instant` + `Clock` remain available as type aliases pointing at `kotlin.time.Instant` / `kotlin.time.Clock` (0.7.1 migration aid), so existing imports continue to compile. Consumers using the renamed properties must update call sites.

### Added — `worker-kmp-app-plugin` epic: 3 new modules eliminate per-platform launcher Kotlin

Ships a Gradle plugin + KSP processor that codegens per-platform Compose Multiplatform
launcher files from a single `@WorkerKmpApp` annotation in commonMain. After plugin
adoption a consumer's source tree contains ZERO per-platform Kotlin files —
the plugin generates Android Application/Activity/manifest, JVM `fun main()`,
iOS `MainViewController`+ xcodegen `project.yml`+ Swift wrappers, and wasmJs
`fun main()` + `index.html` into the consumer's `build/generated/worker-kmp-app/`
directory at build time.

Three new Maven artifacts:

| Module | Coords | Purpose |
|---|---|---|
| `cmp-worker-app-annotations` | `io.github.mobilebytelabs:worker-app-annotations` | KMP module with `@WorkerKmpApp` + `@WorkerKmpAppContent` annotations |
| `cmp-worker-app-ksp` | `io.github.mobilebytelabs:worker-app-ksp` | JVM-only KSP `SymbolProcessor` emitting `CodegenModel` JSON |
| `cmp-worker-app-plugin` | `io.github.mobilebytelabs:worker-app-plugin` (Maven) + `io.github.mobilebytelabs.worker-app` (Gradle Plugin Portal) | Gradle plugin reading CodegenModel + running per-platform codegens |

Consumer usage:

```kotlin
plugins {
    kotlin("multiplatform")
    id("io.github.mobilebytelabs.worker-app") version "$workerVersion"
}
```

```kotlin
// commonMain
@WorkerKmpApp(title = "My App", iosBundleId = "com.example.myapp")
fun appKoinModules(factory: WorkManagerFactory): List<Module> = ...

@WorkerKmpAppContent
@Composable fun AppContent() = MyRootScreen()
```

Build → plugin generates `Generated_App.kt`, `Generated_MainActivity.kt`,
`Generated_Main.kt` (desktop + wasmJs), `Generated_MainViewController.kt`,
`AndroidManifest.xml`, `index.html`, `iosApp/project.yml`, Swift wrappers —
all into `build/generated/worker-kmp-app/...`. Consumer never edits them.

Per-platform opt-out: set `workerKmpApp { androidGenerator = false }` etc.
Plugin also auto-detects pre-existing `Application.kt` / `MainActivity.kt` /
`Main.kt` / `MainViewController.kt` files and skips that source set's codegen
with a warning — zero-config escape hatch.

The canonical sample `cmp-worker-sample-compose-store` retains its hand-authored
launchers in this PR — sample migration to the plugin is a follow-up (requires
`includeBuild` / build-logic plumbing for the same-monorepo apply path). The
plugin module's hand-authored launcher files in the sample remain a 1:1 mirror
of what the plugin's templates would generate, so the migration is mechanical.

KSP processor + Gradle plugin behaviour will be covered by TestKit fixtures
in a follow-up (kotlin-compile-testing's kctfork 0.5.0 ships
`kotlin-compiler-embeddable 2.0.0` which can't read `symbol-processing-api 2.3.x`
metadata; the processor is exercised end-to-end via plugin integration).

Spec: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-app-plugin/`
(epic master + 7 sub-plans, 23 ACs).

### Added — `cmp-worker-compose-all` all-in-one bundle module

New Maven artifact `io.github.mobilebytelabs:worker-compose-all` that re-exports
core + Compose UI + Koin + Store5 + all 4 platform modules + launcher APIs via
`api(project(...))`. CMP consumer apps drop from 5+ separate `worker-*` deps to
**one**:

```kotlin
implementation("io.github.mobilebytelabs:worker-compose-all:$workerVersion")
implementation("io.insert-koin:koin-compose:$koinVersion")
```

Non-breaking: the 4 individual platform modules (`worker-android`, `worker-desktop`,
`worker-ios`, `worker-web`) + `worker-compose` (UI only) + `worker-kmp` (core) +
`worker-koin` + `worker-store5` all continue to publish independently. Consumers
who want granular deps (e.g. pure-Android no-Compose) keep their fine-grained
options.

`samples/cmp-worker-sample-compose-store` migrated to use the bundle as its
single worker-kmp dep — proves the bundle works end-to-end on all 5 targets.

See `cmp-worker-compose-all/README.md` for the "use this for CMP apps" pattern.

### Added — Compose Multiplatform launcher helpers (worker-kmp-cmp-launchers epic)

End-to-end Compose Multiplatform support for consumer sample apps. The library now ships
platform-specific launcher helpers so consumer per-platform launcher files reduce to
≤10 source lines each, with UI living entirely in `commonMain`.

- **`cmp-worker-compose`** — added `iosArm64()` + `iosSimulatorArm64()` publication
  targets. `WorkManagerProvider`, `BackgroundCapabilitiesBanner`, `WorkMonitorScreen`,
  `LocalWorkManager` and the rest of the Compose helpers now compile + ship for iOS.
  Root `apiValidation { klib { enabled = true } }` produces a merged BCV klib snapshot
  at `cmp-worker-compose/api/cmp-worker-compose.klib.api` covering iOS + JS + wasmJs.
- **`cmp-worker-android`** — added open base classes:
  - `WorkerKmpComposeActivity(content: @Composable () -> Unit)` — `ComponentActivity`
    subclass that wires `setContent { content() }` for you.
  - `WorkerKmpStarterApplication` — `Application` subclass with abstract
    `modules(): List<Module>`; runs `startKoin { androidContext(this); modules(modules()) }`
    idempotently on `onCreate`.
- **`cmp-worker-desktop`** — added top-level `launchDesktopWorkerApp(title, koinModules, content)`
  wrapping `application { Window(...) { content() } }` after an idempotent Koin start.
- **`cmp-worker-ios`** — added `workerKmpMainViewController(koinModules, content): UIViewController`
  returning a `ComposeUIViewController { content() }` after an idempotent Koin start.
  Now ships a static `WorkerKmpIos.framework` for Swift / SwiftUI consumption.
- **`cmp-worker-web`** — added `launchWebWorkerApp(canvasElementId, koinModules, content)` for
  both wasmJs and js(IR) targets, wrapping `CanvasBasedWindow(canvasElementId) { content() }`.
- **`gradle/libs.versions.toml`** — added `koin-compose` alias (`io.insert-koin:koin-compose`).

All new public APIs include Dokka KDoc with runnable code examples. Idempotent Koin
helpers (`startWorkerKoinIfAbsent`) are exposed for test fixtures.

### Changed — `cmp-worker-sample-compose-store` is now fully Compose Multiplatform

The sample previously ran on JVM Desktop only with a hand-rolled 52-line `Main.kt`.
It now targets **all 5 KMP platforms** with per-platform launcher files reduced to
≤10 source lines each, courtesy of the launcher APIs above:

- UI moved to `commonMain/.../ui/SampleApp.kt` (no parameters — pulls `WorkManager` +
  Store via `koinInject`); `commonMain/.../ui/App.kt` deleted.
- Shared Koin module factory at `commonMain/.../di/SampleKoinSetup.kt` so each per-platform
  launcher differs only in which `WorkManagerFactory` it plugs in.
- Android: `SampleApplication` (3 lines) + `MainActivity` (1 line) + `AndroidManifest.xml`.
- Desktop: `jvmMain/Main.kt` (4 lines, down from 52).
- iOS: `iosMain/MainViewController.kt` (3 lines) + `iosApp/` Xcode wrapper using
  xcodegen for merge-friendly project generation.
- Web (wasmJs): `wasmJsMain/Main.kt` (4 lines) + `wasmJsMain/resources/index.html` shell
  with `<canvas id="composeCanvas">`.

### Internal

- `.github/workflows/pr-check.yml` gains 3 new jobs: `ios-bcv-check` (runs
  `scripts/check-ios-artifact.sh`), `sample-wasmjs` (`wasmJsBrowserDistribution`),
  `sample-ios` (Kotlin/Native framework link + xcodegen + `xcodebuild` against iOS sim).
- `scripts/check-ios-artifact.sh` — guard script asserting `cmp-worker-compose`'s
  klib BCV snapshot covers `iosArm64` + `iosSimulatorArm64`.

### Samples

- **`samples/cmp-worker-sample-compose-store/`** — new end-to-end Compose Multiplatform
  sample exercising the full integration story: `cmp-worker-kmp` + `cmp-worker-store5` +
  `cmp-worker-koin` + `cmp-worker-compose`. A `StoreBackedWorker` refreshes a Store5
  `Store<String, Article>` from a fake fetcher; the Compose UI observes both the
  `WorkInfo` flow (via `WorkInfoCard`) and the Store cached stream simultaneously, so
  the worker's effect on the cache is visible through two independent surfaces. JVM
  target only for the first cut — `App()` lives in `commonMain` so iOS / wasmJs / Android
  entry points are a small follow-up. Run: `./gradlew :samples:cmp-worker-sample-compose-store:run`.

### Repo layout

- **Samples moved to `samples/`** — `cmp-worker-sample`, `cmp-worker-sample-android`,
  `cmp-worker-sample-ios`, and `cmp-worker-sample-hilt` are now under `samples/` at
  repo root. Gradle paths shifted accordingly:
  - `:cmp-worker-sample` → `:samples:cmp-worker-sample`
  - `:cmp-worker-sample-android` → `:samples:cmp-worker-sample-android`
  Run targets become `./gradlew :samples:cmp-worker-sample:jvmRun` etc. Internal-only
  scaffold dirs (`cmp-worker-sample-ios`, `cmp-worker-sample-hilt`) are README-only
  and not Gradle projects, so their move is purely filesystem (no task path change).
  Web-push sample servers `samples/web-push-server-node` + `samples/web-push-server-ktor`
  are untouched — they were already in `samples/`.

### Core API

#### Desktop true-background daemon (Phase 8 alpha05.X)

- **Per-OS installers — real implementations:**
  - `WindowsTaskInstaller` via `schtasks /Create /XML` + Task XML (UTF-16) — minute-granular
    trigger, `InteractiveToken` principal, `LeastPrivilege` run level, 10-min execution
    limit per firing.
  - `MacosLaunchdInstaller` via `launchctl load -w` + `~/Library/LaunchAgents/{appId}.worker-kmp.plist`
    — `StartInterval`-based scheduling, `ProcessType=Background`.
  - `LinuxSystemdInstaller` via `~/.config/systemd/user/{appId}.worker-kmp.{timer,service}`
    + `systemctl --user enable --now`. Linger detection via `loginctl show-user`; warns if
    `Linger=no` (timer pauses on logout — consumer needs `loginctl enable-linger`).
  - `LinuxCronInstaller` fallback (no systemd-user) — appends marker line
    `# worker-kmp:{appId}` to user crontab via `crontab -l` / `crontab -`.
  - `LinuxInstallerRouter` selects `LinuxSystemdInstaller` when `systemctl --user --version`
    succeeds; falls back to `LinuxCronInstaller` when crontab is reachable; otherwise
    returns a `NoSchedulerInstaller` that fails every operation cleanly.
- **`createDesktopBackgroundInstaller()` factory** now dispatches by OS family — Windows /
  macOS / Linux (via Router) / Other (fail-only stub for unsupported OS). The Kt class
  `io.github.mobilebytelabs.worker.daemon.installer.DesktopInstallerFactoryKt` provides a
  stable reflective entry point so `cmp-worker-desktop` can auto-install without taking a
  hard dependency on the daemon module.
- **`LockFile`** — `FileChannel.tryLock()` + PID tracking at `{persistenceDir}/daemon.lock`;
  refuses second daemon instance (advisory on POSIX, mandatory on Windows).
- **`JarIntegrityCheck`** — SHA-256 of the currently-running daemon JAR (resolved via
  `protectionDomain.codeSource.location`) compared to `{persistenceDir}/daemon.jar.sha256`
  (written at install time by the per-OS installer). Mismatch → daemon refuses to run.
  Fail-OPEN when running from classes (dev/test) or when no hash file exists (first run /
  manual launch).
- **Daemon main loop** — real implementation: probe mode prints capability matrix; normal
  mode acquires lock, verifies integrity, reads `.properties` files from persistence dir,
  counts entries by state within `--max-runtime-seconds` budget, heals stuck-RUNNING entries
  back to ENQUEUED (mirrors `DesktopWorkStateStore.restoreFromPersistence`). Full
  PropertiesFileWorkPersistence + `WorkManager.runOne()` integration deferred to alpha05.X.Y
  pending a richer persistence schema that carries `workerClass` FQCN + `inputData`.
- **`DesktopBackgroundConfig` moves to commonMain** —
  `cmp-worker-kmp/src/commonMain/.../config/DesktopBackgroundConfig.kt`, now field of
  `DesktopWorkerConfig.background` (nullable; defaults to `null`). Consumers declare the
  full background-scheduling config from shared code. `persistenceDir` becomes nullable
  `String?` (null = resolve to `~/.worker-kmp` on the JVM side at install time).
- **Auto-install at first `DesktopWorkManager` construction** — when
  `DesktopWorkerConfig.background.installOnFirstRun == true`, `desktopWorkManagerFactory()`
  invokes the installer reflectively (no hard dep on `cmp-worker-desktop-daemon` from
  `cmp-worker-desktop`). Skips if `isInstalled(appId)` already returns true. All failures
  caught + logged at WARN so a missing daemon module never breaks normal factory construction.
- **`RotatingLogger`** scaffold — file-backed log at `{persistenceDir}/logs/daemon.log.{0..2}`
  via `java.util.logging.FileHandler` (1 MB × 3-file rotation). Best-effort install — failures
  during install (e.g., persistenceDir not writable) are silent.
- **`HmacPersistence`** scaffold — HMAC-SHA256 envelope around `.properties` files defending
  T2 (persistence-file tamper) per `docs/operations/security.md`. Ephemeral 32-byte key at
  `{persistenceDir}/.persistence-hmac.key` (POSIX 600 via NIO). Helpers shipped;
  integration into `PropertiesFileWorkPersistence` reader/writer deferred to alpha05.X.Y.
- **5 smoke tests in `DesktopDaemonTest`** — defaults sane, `--probe` flag parsed,
  `installer_probe_returnsCorrectOsForCurrentHost`, `lockFile_acquire_thenSecondAttemptFails`,
  `jarIntegrityCheck_missingHashFile_isPermissive`, `jarIntegrityCheck_emptyHashFile_isPermissive`.

Deferred to alpha05.X.Y: full `PropertiesFileWorkPersistence` read/execute integration in
the daemon main loop; HMAC-signed persistence wrapper integration (helpers shipped,
integration TBD); shadowJar fat-JAR packaging for distribution.

#### Web Push universal background (Phase 9 alpha06.X)

- **`JsWebPushSubscriber`** — real Service Worker registration via
  `navigator.serviceWorker.register(scriptUrl)` + `pushManager.subscribe({userVisibleOnly,
  applicationServerKey})`. ArrayBuffer → BASE64URL encoding for `p256dh` + `auth` keys.
  Auto-POST subscription metadata to consumer's `WebPushConfig.serverEndpoint` with optional
  async `serverEndpointAuthHeader` auth-header provider. Replaces the alpha06 log-only stub.
- **`WebPushConfig.serverEndpointAuthHeader: (suspend () -> String)?`** — new optional async
  auth header provider. Library invokes the lambda each time it submits a subscription —
  letting consumers fetch a fresh bearer token / OIDC JWT / signed-request header out-of-band.
- **`worker-kmp-sw.js`** — real implementation. Push event handler reads IndexedDB
  (`worker-kmp` db, `work` object store), filters ENQUEUED entries matching scope, marks
  RUNNING, broadcasts `'PENDING_PROCESSED'` via `BroadcastChannel('worker-kmp')` for cross-tab
  notification. `periodicsync` event handler for the `worker-kmp:` tag prefix. CSP-clean (no
  eval, no document.write, no innerHTML).
- **BroadcastChannel cross-tab wiring** — new `expect fun openWorkerKmpBroadcastChannel(...)`
  in `cmp-worker-web/commonMain`; JS `WebWorkManager` opens
  `BroadcastChannel('worker-kmp')` at construction, refreshes state-store from IndexedDB on
  `'PENDING_PROCESSED'` events. WasmJs + JVM actuals are no-ops (WasmJs pending kotlinx-browser
  bindings; JVM has no browser runtime). Closed in `WebWorkManager.shutdown()`.
- **Reference push servers** at `samples/web-push-server-{node,ktor}/`:
  - `web-push-server-node/` — Express + better-sqlite3 + node-cron + web-push npm package
    (~95 LoC). Hourly cron sends `WORKER_KMP_TRIGGER` push to every subscription. Drops
    expired (410/404) subscriptions.
  - `web-push-server-ktor/` — Standalone Gradle project (NOT in root `settings.gradle.kts`);
    Ktor 3.x + Exposed + sqlite-jdbc + nl.martijndwars:web-push + BouncyCastle (~135 LoC).
    Same endpoints + same hourly loop.
  - Both ship hardening checklists pointing at SECURITY.md T7-T15 (rate-limiting, encryption
    at rest, VAPID-vault integration, unsubscribe endpoint, annual key rotation).
- **`./gradlew :cmp-worker-web-push:generateVapidKeys`** — Gradle task under `worker-kmp`
  group that prints VAPID key-generation instructions + framework
  `/secrets push --generate vapid` integration pointer. Full BouncyCastle-based generator
  deferred to alpha06.X.Y.

Deferred to alpha06.X.Y: WasmJs real `WebPushSubscriber` (current = stub); BouncyCastle-based
VAPID key generator inside the Gradle task; end-to-end integration test against a real
browser; WasmJs `BroadcastChannel` binding via kotlinx-browser.

#### Phase 1 alpha01.X — real foreground impls (v3.0.0-alpha01.X)

Replaces the log-only `runAsForeground` stubs from alpha01 with platform-native
implementations across all 5 target platforms (Android via cmp-worker-android,
iOS, Desktop/JVM, JS, WasmJs).

- **Desktop (JVM)** — `cmp-worker-kmp/src/jvmMain/.../ForegroundWorker.jvm.kt`:
  surfaces foreground tasks via `java.awt.SystemTray`. Per-`notificationId` icon
  reuse (matches Android's notificationId semantics), tooltip updates with
  `title — message (progress%)`, automatic icon removal at `progress >= 100`.
  Graceful degradation to log-only WARN when `SystemTray.isSupported()` is false
  (headless JVM, missing `java.desktop`, JLink images).
- **iOS** — `cmp-worker-kmp/src/iosMain/.../ForegroundWorker.ios.kt`:
  schedules `BGProcessingTaskRequest` to extend the app's background lifetime
  AND posts a `UNNotification` for user visibility. Detects iOS version via
  `UIDevice.currentDevice.systemVersion`; routes iOS 17+ consumers through the
  same shim path until Kotlin/Native ships a `BGContinuedProcessingTaskRequest`
  binding (tracked for alpha01.X.1 follow-up).
- **JS** — `cmp-worker-kmp/src/jsMain/.../ForegroundWorker.js.kt`: requests
  Notification permission idempotently, posts progress-bearing tag-de-duped
  notifications. Routes through `ServiceWorker.showNotification()` when an SW
  is registered for better browser eviction resistance; otherwise uses the
  page-scope `new Notification(...)` API.
- **WasmJs** — `cmp-worker-kmp/src/wasmJsMain/.../ForegroundWorker.wasmJs.kt`:
  mirrors the JS behaviour via `@JsFun`-bound interop helpers.
- **Android** — `cmp-worker-android/.../AndroidForegroundBridge.kt` (new):
  Android-side hook that performs the actual `setForegroundAsync` call on
  `androidx.work.CoroutineWorker`. Builds a `NotificationCompat` notification
  on a worker-kmp notification channel (`worker-kmp.foreground`), wires the
  Android-14+ `foregroundServiceType` constant from `ForegroundServiceType`.
  Lifecycle: `KmpAndroidWorker` registers itself in a `ConcurrentHashMap<Uuid, KmpAndroidWorker>`
  before delegating to the user's KMP worker, unregisters in a `try/finally`.
  Reflectively dispatched from `cmp-worker-kmp`'s JVM actual when an Android
  runtime is detected — keeps cmp-worker-kmp Android-dependency-free.

**Pragmatic deviation from the alpha01.X dispatch (Option A vs B)**: the dispatch
proposed adding `android()` as a target on `cmp-worker-kmp` so the Android actual
lives in the same KMP module. We chose Option B (Android bridge in
`cmp-worker-android` + reflective dispatch from the JVM actual) because adding
`android()` to `cmp-worker-kmp` would require the
`com.android.kotlin.multiplatform.library` plugin and create overlapping
publication coordinates with `cmp-worker-android`. The chosen design keeps the
publication graph clean (one Android module = `cmp-worker-android`) at the cost
of one reflective `Class.forName` lookup per `setForeground()` call on Android —
acceptable given that foreground promotion is a low-frequency event.

#### Phase 7 alpha04.X — native API parity wiring (v3.0.0-alpha04.X)

Wires the previously-scaffolded native-API surfaces into the per-platform work managers.

- **`OneTimeWorkRequestBuilder.setExpedited(OutOfQuotaPolicy)`** (commonMain) —
  requests Android 12+ expedited execution. Maps to
  `androidx.work.OneTimeWorkRequest.Builder.setExpedited(...)` when SDK_INT ≥ 31;
  no-op (debug-logged) on Android <31 / iOS / Desktop / Web.
- **`OneTimeWorkRequestBuilder.setInitialDelay(Duration)`** +
  **`PeriodicWorkRequestBuilder.setInitialDelay(Duration)`** (commonMain) —
  delays the first attempt. Android: native `setInitialDelay(...)`.
  iOS: `BGProcessingTaskRequest.earliestBeginDate` when background tasks are
  enabled; otherwise coroutine `delay()`. Desktop + Web: coroutine `delay()`.
- **`ForegroundServiceType` → Android-14+ enforcement** (cmp-worker-android) —
  `AndroidForegroundBridge` translates `ForegroundServiceType` to
  `ServiceInfo.FOREGROUND_SERVICE_TYPE_*` constants per the active SDK level
  (API 29+ for `DATA_SYNC/MEDIA_PLAYBACK/...`, API 30+ for `CAMERA/MICROPHONE`,
  API 34+ for `HEALTH/REMOTE_MESSAGING/SHORT_SERVICE/SPECIAL_USE/SYSTEM_EXEMPTED`).
  Falls back to the no-serviceType constructor on Android <14 (which doesn't enforce).
- **`PeriodicWorkRequestBuilder.setQuickRefresh(Boolean)`** + new
  `IosWorkerConfig.appRefreshTaskIdentifier` (commonMain) — iOS-only hint: when
  `true` AND `appRefreshTaskIdentifier` is set, the iOS scheduler issues a
  `BGAppRefreshTaskRequest` (short, frequent wake-ups for keep-alive polling)
  instead of `BGProcessingTaskRequest`. No-op on Android / Desktop / Web.
- **Info.plist validation at iOS init time** (cmp-worker-ios) — new
  `InfoPlistValidator.kt`. When `iosWorkManagerFactory()` constructs an
  `IosWorkManager`, the validator inspects `NSBundle.mainBundle.infoDictionary`
  and emits actionable kermit ERROR entries when:
  - `enableBackgroundTasks=true` but `UIBackgroundModes` lacks `"processing"`
    OR `BGTaskSchedulerPermittedIdentifiers` lacks `bgProcessingTaskIdentifier`.
  - `appRefreshTaskIdentifier` is set but `UIBackgroundModes` lacks `"fetch"`
    OR `BGTaskSchedulerPermittedIdentifiers` lacks `appRefreshTaskIdentifier`.
  Log-only — does not throw — so a misconfigured Info.plist degrades gracefully.
- **Web Periodic Background Sync** (cmp-worker-web) — new `enablePeriodicBackgroundSync`
  field on `WebWorkerConfig`/`WebWorkManagerConfig`. When `true`, periodic workers
  register a tag via `ServiceWorkerRegistration.periodicSync.register(tag, { minInterval })`.
  Best-effort: browsers gate the API behind PWA-install heuristics — falls back to
  the existing polling/timer path on unsupported origins. New top-level expect:
  `registerPeriodicSyncTag(tag, minIntervalMs, swScript)` with actuals for js/wasmJs/jvm.
- **Web Notifications API** (cmp-worker-web) — new public surface in commonMain:
  - `enum class NotificationPermission { GRANTED, DENIED, DEFAULT }`
  - `suspend fun requestNotificationPermission(): NotificationPermission`
  - `fun showWorkerNotification(id, title, body, progress)` — tag-de-duped,
    SW-aware, auto-closes when `progress >= 100`.
  Used internally by the JS/WasmJs `runAsForeground` actuals; exposed publicly
  for app-level re-use.

#### Phase 1 + Phase 7 surface area summary

`cmp-worker-kmp` public additions:
- `OneTimeWorkRequest.{initialDelay, expeditedPolicy}` properties
- `PeriodicWorkRequest.{initialDelay, quickRefresh}` properties
- `OneTimeWorkRequestBuilder.{setInitialDelay, setExpedited}` builders
- `PeriodicWorkRequestBuilder.{setInitialDelay, setQuickRefresh}` builders
- `IosWorkerConfig.appRefreshTaskIdentifier` field
- `WebWorkerConfig.enablePeriodicBackgroundSync` field

`cmp-worker-android` public additions:
- `AndroidForegroundBridge` object (`promote(worker, info)` — reflectively
  invoked by cmp-worker-kmp's JVM actual)

`cmp-worker-ios` public additions:
- `IosWorkManagerConfig.appRefreshTaskIdentifier` field

`cmp-worker-web` public additions:
- `NotificationPermission` enum
- `requestNotificationPermission()` / `showWorkerNotification(...)` functions
- `WebWorkManagerConfig.enablePeriodicBackgroundSync` field

All additions retain backward compatibility with v3.0.0-alpha01..alpha06 callers
(new fields/builders have defaults). BCV dumps updated under `*/api/`.

#### Store5 advanced workers (Phase 2 alpha02.X)

- **`MutableStoreSyncWorker<K, V>`** — abstract `CoroutineWorker` that invokes
  `MutableStore.write(StoreWriteRequest.of(key, value))` to push pending mutations.
  Maps `StoreWriteResponse.Success` → `WorkResult.success` (overridable via
  `mapWriteResponseToWorkData(response)`); `StoreWriteResponse.Error.Exception` is
  routed through the `isRetryable()` override (defaults to false / fatal);
  `StoreWriteResponse.Error.Message` is always fatal. Class-level `@OptIn(ExperimentalStoreApi)`
  propagates Store5's experimental opt-in to subclasses.
- **`StoreFreshnessWorker<K, Output>`** — checks `Validator<Output>.isValid(item)` against
  the cached value (read via `StoreReadRequest.cached(key, refresh = false)`) before
  fetching. If still fresh: returns `WorkResult.success(workDataOf("skipped" to "fresh"))`
  — observers key off `KEY_SKIPPED` to count bandwidth-saved invocations. If stale: same
  `store.stream(StoreReadRequest.fresh(key))` path as `StoreBackedWorker`.
- Both added to `cmp-worker-store5/src/commonMain/kotlin/.../store5/`. BCV updated at
  orchestrator's final build pass.

#### StoreFlow advanced patterns (Phase 3 alpha03.X)

- **`DraftSubmitHandler<P, R>`** — persistent draft state machine surviving process restarts.
  State enum `Idle → Drafting → Submitting → Submitted/Failed` driven by `draft(payload)`
  + `submit()`. `rehydrateFromOutbox()` restores the most-recently-enqueued PENDING/RETRYING
  entry to `State.Submitting` on process restart so the consumer's UI can offer a
  "retry / cancel" affordance. Adapts `kmp-project-template/core-base/store/submit/DraftSubmitHandler.kt`
  to worker-kmp's `SubmitOutbox` contract.
- **`PrefetchPagingWorker`** — abstract `CoroutineWorker` for paginated cache warming.
  Consumer extends + implements `fetchPage(pageNumber)`. Input keys: `KEY_START_PAGE` (Int,
  required) + `KEY_PAGE_COUNT` (Int, optional; default 3). Output key: `KEY_PAGES_FETCHED`
  (Int). Page-fetch failure → `WorkResult.retry`; all-pages-clean → `WorkResult.success`.
- **`SubmitStateUi` enum + `SubmitStateUiModel` data class** — Compose helper (in
  `cmp-worker-compose/.../storeflow/SubmitStateScaffold.kt`) — plain Kotlin types so
  `cmp-worker-compose` doesn't take a hard dep on `cmp-worker-storeflow`. Consumer code maps
  `DraftSubmitHandler.State<P>` to one of the five `SubmitStateUi` labels. The full
  Composable `SubmitStateScaffold(model, content)` lands in alpha03.X.Y.
- Per-platform persistent `SubmitOutbox` impls (Room / NSUserDefaults / properties-file /
  IndexedDB) deferred to alpha03.X.Y — `InMemorySubmitOutbox` remains the default.

#### Samples — Wasm browser (Phase 5 alpha07)

- **`cmp-worker-sample/wasmJsBrowserMain`** — Compose-for-Wasm browser sample scaffold.
  Adds `wasmJs { browser { ... } }` target to `cmp-worker-sample/build.gradle.kts` with
  `binaries.executable()` + webpack `cssSupport` + named output `cmp-worker-sample.js`.
  Plain-DOM entry point at `src/wasmJsMain/kotlin/BrowserSampleMain.kt` demonstrating the
  worker-kmp Wasm import + an enqueue button + event log. Host page at
  `src/wasmJsMain/resources/index.html`. Run via
  `./gradlew :cmp-worker-sample:wasmJsBrowserRun`. Full Compose-for-Wasm UI
  (`CanvasBasedWindow` + `SampleApp()`) defers to alpha07.X.Y.
- iOS Xcode sample + Hilt Android sample remain README-only scaffolds; full Xcode project
  + APK defer to alpha07.X.Y based on consumer need.

### Documentation

- Reorganized source repo docs into a Wiki-friendly structure under `docs/`:
  - `docs/Home.md` — KMP-centric overview (new)
  - `docs/_Sidebar.md` — GitHub Wiki nav (new)
  - `docs/getting-started/` — installation + quick-start + migration
  - `docs/platform-support/` — per-platform setup + API matrix + true-background matrix
  - `docs/features/` — foreground tasks + observers + Web Push server
  - `docs/operations/` — security + performance
  - `docs/release/` — release process + postmortem template
- Rewrote `README.md` as tight (~100 lines) KMP-centric front page emphasizing
  Kotlin Multiplatform + Compose Multiplatform out-of-box positioning.
- All previously-top-level markdown files moved via `git mv` (history preserved).
- Updated `.github/PULL_REQUEST_TEMPLATE.md` + `.github/workflows/security-scan.yml`
  + `scripts/security-doc-coverage.sh` to reference `docs/operations/security.md`.

### Core API

#### Phase 0 deep refactor (v3.0.0-alpha00.X — per-actual clean-break)

- **Per-actual `WorkManagerFactory` pattern replaces `PlatformWorkManager` global slot** —
  **BREAKING (clean break — no v2 BC story, no `@Deprecated` grace)**. The legacy
  `expect object PlatformWorkManager` + `PlatformWorkManager.configure(impl)` global slot,
  and the four `initializeWorkerXxx(...)` side-effecting init functions have been REMOVED
  outright. Replaced by a `fun interface WorkManagerFactory` in commonMain and per-platform
  factory builders (`androidWorkManagerFactory(context)`, `iosWorkManagerFactory()`,
  `desktopWorkManagerFactory()`, `webWorkManagerFactory()`). The factory is wired through
  the new third parameter on `workKoinModule(config, workers, factory)`. Consumers now
  call `startKoin` exactly once per app — no more pre-startKoin platform-init step. See
  `MIGRATION_FROM_2_x.md`.
- **`WorkManagerFactory`** — new commonMain `fun interface` in `io.github.mobilebytelabs.worker`.
  `fun create(config: WorkerConfig, workers: WorkerRegistry): WorkManager`. Implementations
  ship in each platform module; consumers obtain them via the `xxxWorkManagerFactory(...)`
  entry-points.
- **Per-platform sub-configs in commonMain** — new data classes `AndroidWorkerConfig`,
  `IosWorkerConfig`, `DesktopWorkerConfig`, `WebWorkerConfig` in
  `io.github.mobilebytelabs.worker.config`. Each mirrors the legacy platform-module config
  field-by-field. `WorkerConfig` now nests one of each as a default-constructed property —
  consumers declare platform tuning from commonMain without importing
  `IosWorkManagerConfig` / `DesktopWorkManagerConfig` / `WebWorkManagerConfig`.
- **`PlatformContext`** — role refined: remains a commonMain `expect class` with empty
  `actual` declarations on JVM/iOS/JS/WasmJs. The originally-planned `actual typealias` to
  `android.content.Context` was **dropped** because Kotlin Multiplatform's `expect`/`actual`
  contract requires same-module actuals, and `cmp-worker-kmp` is a separate module from
  `cmp-worker-android` (the latter has no Android target). The factory pattern achieves the
  same goal (no Android imports in commonMain consumer code) without forcing the same-module
  pair. See MIGRATION_FROM_2_x.md §4.
- **`androidWorkManagerFactory(context, ...)`** — new top-level `fun` in
  `cmp-worker-android` returning a `WorkManagerFactory`. Initialises `androidx.work.WorkManager`
  with a `KmpWorkerFactory` that consults the consumer's `WorkerRegistry` (registry-first;
  reflection fallback enabled via `WorkerConfig.androidConfig.useReflectionFactory = true`,
  the default). Strict mode (`false`) throws on unregistered workers.
- **`iosWorkManagerFactory()`** — new top-level `fun` in `cmp-worker-ios` (requires
  `@OptIn(ExperimentalWorkerApi::class)`) returning a `WorkManagerFactory`. Wraps the
  `WorkerRegistry` in a `WorkerRegistryIosAdapter` (throws on unregistered worker — iOS has
  no reflection fallback).
- **`desktopWorkManagerFactory()`** — new top-level `fun` in `cmp-worker-desktop` returning
  a `WorkManagerFactory`. Threads the registry through a `ChainedDesktopWorkerFactory` that
  consults the registry first, then falls back to `Class.forName` reflection.
- **`webWorkManagerFactory()`** — new top-level `fun` in `cmp-worker-web` (requires
  `@OptIn(ExperimentalWorkerApi::class)`) returning a `WorkManagerFactory`. Lives in
  commonMain (both JS + WasmJs share the same factory body — per-target `expect`/`actual`
  shims resolve the constraint evaluator + persistence + online watcher).
- **Removed** — `PlatformWorkManager` (expect object + 5 platform actuals) /
  `initializeWorkerAndroid(...)` / `initIosWorkManager(...)` /
  `initializeWorkerDesktop(...)` / `initWebWorkManager(...)` / `KoinAndroidWorkerFactory`
  sample-side helper / `WorkManagerProvider(workManager = PlatformWorkManager())` default.
- **Compose** — `WorkManagerProvider(workManager: WorkManager, content)` no longer has a
  default `workManager` value. Callers supply the `WorkManager` explicitly (typically from
  the Koin graph via `koinInject<WorkManager>()` on commonMain or
  `KoinJavaComponent.getKoin().get<WorkManager>()` on Android).
- Tests refactored: `cmp-worker-koin:WorkKoinModuleTest` + `WorkKoinModuleJvmTest` now use
  a test `WorkManagerFactory` instead of `PlatformWorkManager.configure(...)`. Two new
  assertions cover (a) factory invocation receives the consumer-supplied config + workers,
  and (b) `WorkerConfig` + `WorkerRegistry` are exposed as Koin singles.
- BCV snapshots regenerated for `cmp-worker-kmp`, `cmp-worker-koin`, `cmp-worker-desktop`.
  `cmp-worker-android` + `cmp-worker-ios` klib snapshots compile clean (klib api dump task
  is a no-op in their gradle config — pre-existing state, not regressed).
- Sample apps (`cmp-worker-sample-android`, `cmp-worker-sample` JVM + JS) updated to the
  new pattern — single `startKoin` call wiring config + registry + factory.

#### Phase 0 base refactor (v3.0.0-alpha00 — prior baseline retained below for history)

- **`workKoinModule` val → function** — **BREAKING (source-only)**: the previously `val`-shaped
  Koin module is now a `fun` with two optional parameters. Existing call sites need a 1-line edit:
  `modules(workKoinModule, ...)` → `modules(workKoinModule(), ...)`. Binary callers (Java consumers
  via reflection) are unaffected because the BCV surface changed shape too. See `MIGRATION_FROM_2_x.md`.
- **`WorkerConfig`** — new `data class` in `io.github.mobilebytelabs.worker.config` carrying
  `logLevel`, `defaultRetryConfig`, and `observers`. Unified commonMain configuration surface;
  replaces ad-hoc per-platform config classes (which remain in their legacy form for v3.0.0-alpha00
  and migrate per-actual in v3.0.0-alpha00.X follow-ups).
- **`LogLevel`** — new public enum (`VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `SILENT`) for
  worker-kmp internal logs (kermit-backed).
- **`WorkerRegistry`** — new type-safe registry in `io.github.mobilebytelabs.worker.registry`.
  Consumers declare workers via `workerRegistry { register<SyncWorker> { ctx -> SyncWorker(ctx, get()) } }`
  in commonMain; platform actuals instantiate by simple class name (matching the
  `OneTimeWorkRequestBuilder` convention; `KClass::simpleName` is the only portable
  reflection surface across iOS/JS/Wasm/JVM).
  Becomes immutable once loaded into Koin; late registration throws `WorkerRegistryAlreadyLoadedException`
  (defends against T23 per `SECURITY.md`).
- **`PlatformContext`** — new commonMain `expect class` in `io.github.mobilebytelabs.worker`.
  Sentinel class on JVM/iOS/JS/WasmJs in v3.0.0-alpha00; becomes `actual typealias` to
  `android.content.Context` once cmp-worker-android migrates in v3.0.0-alpha00.1.
- **`MIGRATION_FROM_2_x.md`** — new source-repo-root migration doc covering the `workKoinModule`
  shape change + the staged per-actual zero-init plan + the future `cmp-worker-migrate` Gradle plugin.
- **BC test gate verifies legacy v2.1.0 behaviour** — the new module shape is exercised against
  the `cmp-worker-bc-test` classpath (Phase 13) — failure to keep the v2.x deprecation contract
  fails CI.
- 7 new tests in `WorkerRegistryTest` (cmp-worker-kmp commonTest): DSL build, typed-factory
  storage by qualifiedName, unknown-class returns null, register-after-lock throws, and
  3 path-injection rejection paths (`..`, `/`, space) per the SECURITY.md T22 mitigation.
- **`cmp-worker-store5`** (NEW artifact) — Store5 bridge. `StoreBackedWorker<K, Output>` base
  class + `StoreRefreshScheduler` (`schedulePeriodicRefresh<T>` / `cancelRefresh` /
  `observeRefreshes`) + `workStore5KoinModule`. Pinned to `org.mobilenativefoundation.store:store5:5.1.0-alpha06`
  to inherit the full KMP target matrix including wasmJs. Maven Central coordinates
  `io.github.mobilebytelabs:worker-store5:3.0.0-alpha02`. Future: `MutableStoreSyncWorker` +
  `StoreFreshnessWorker` ship in alpha02.X follow-ups.
- **`cmp-worker-storeflow`** (NEW artifact) — worker-anchored Store-flow + submit-outbox bridge.
  Scaffolded minimum-shippable form: `FetchPolicy` enum (lifted verbatim from
  `kmp-project-template/core-base/store/screen/FetchPolicy.kt`), `SubmitOutbox<P>` interface +
  `InMemorySubmitOutbox<P>` default, `OutboxEntry<P>` + `OutboxState`,
  `WorkScheduledOfflineSubmitSyncer<P, R>` (delegates periodic + connectivity-gated retry to
  `WorkManager.enqueueUniquePeriodicWork`), `SyncerWorker` (no-op scaffold), and
  `workStoreFlowKoinModule`. Maven Central coordinates
  `io.github.mobilebytelabs:worker-storeflow:3.0.0-alpha03`. Future: per-platform persistent
  outbox backends (Room/SQLDelight), `SyncerWorker.doWork()` outbox-flush wiring, paging
  integration, Compose helpers, and per-platform retry heuristics ship in alpha03.X follow-ups.

#### Native API parity (Phase 7 alpha04 scaffold)

- **`OutOfQuotaPolicy`** — new public enum in `cmp-worker-kmp` commonMain
  (`io.github.mobilebytelabs.worker`) mirroring `androidx.work.OutOfQuotaPolicy`. Two values:
  `RUN_AS_NON_EXPEDITED_WORK_REQUEST`, `DROP_WORK_REQUEST`. Governs behaviour when the
  Android 12+ expedited-work quota is exhausted; no-op on iOS/Desktop/Web (those platforms
  have no expedited-quota concept; the request runs as ordinary background work).
- **`PLATFORM_API_MATRIX.md`** — new source-repo-root doc surfacing every native
  background-execution API × every platform worker-kmp targets. Documents current cell state
  + per-cell alpha targets for `setExpedited(OutOfQuotaPolicy)` (alpha04.X), Android 14
  foreground-service types (alpha04.X), `setInitialDelay(Duration)` (alpha04.X),
  `BGAppRefreshTaskRequest` (alpha04.X), Info.plist contract validation (alpha04.X),
  Periodic Background Sync API (alpha04.X, Chrome-only), and Web Notifications API
  (alpha04.X consumer-opt-in).
- `OneTimeWorkRequestBuilder.setExpedited(OutOfQuotaPolicy)` wiring — Android-actual landing
  in alpha04.X follow-up (scaffold intentionally avoids modifying the existing builder while
  the enum is established).

#### Desktop true-background daemon (Phase 8 alpha05 scaffold)

- **`cmp-worker-desktop-daemon`** (NEW module — JVM-only) — scaffold for the desktop
  true-background daemon. The daemon is invoked by the host OS scheduler (Windows Task
  Scheduler / macOS launchd / Linux systemd-user timer / cron) to process pending work
  when the consumer app is not running.
- **`DesktopBackgroundDaemon.main(args)`** — entry point in
  `io.github.mobilebytelabs.worker.daemon`. CLI flags: `--persistence-dir <path>`
  (default `~/.worker-kmp`), `--max-runtime-seconds <n>` (default 120), `--log-file <path>`,
  `--probe` (print capabilities + exit). alpha05 scaffold logs intent + exits cleanly;
  lock-file acquisition, JAR integrity check, persistence loading, and work execution
  land in alpha05.X follow-ups.
- **`DesktopBackgroundConfig`** — `data class` carrying installer parameters (`appId`,
  `daemonJarPath`, `runtimeJavaHome`, `persistenceDir`, `pollIntervalMin`,
  `installOnFirstRun`, `uninstallOnAppUninstall`, `runOnlyIfLoggedOn`).
- **`DesktopBackgroundInstaller`** — per-OS installer interface (`install` / `uninstall` /
  `isInstalled` / `probe`). Per-OS impls (Windows `schtasks`, macOS launchd user agent,
  Linux `systemd --user` with cron fallback) land in alpha05.X follow-ups. alpha05 ships a
  `StubInstaller` returning `InstallResult.Failure("alpha05 scaffold")` for `install`/`uninstall`
  and an OS-family-only `DesktopOsCapability` probe.
- **`InstallResult`** — sealed class (`Success` / `Failure(reason)`).
- **`DesktopOsCapability`** — capability descriptor (`osFamily`, `hasSchtasks`,
  `hasLaunchctl`, `hasSystemctlUser`, `hasCron`, `notes`). alpha05 probe detects OS family
  only; ProcessBuilder-based per-tool probes land in alpha05.X.
- **`OsFamily`** — enum (`WINDOWS` / `MACOS` / `LINUX` / `OTHER`).
- **`createDesktopBackgroundInstaller()`** — top-level factory returning the per-OS installer
  for the current host (alpha05 returns `StubInstaller`).
- 4 smoke tests in `DesktopDaemonTest`: `parseFlags` defaults sane, `--probe` flag parsed,
  `installer.probe()` detects host OS family, `installer.install()` returns Failure in the
  alpha05 scaffold.
- **`cmp-worker-desktop-daemon/README.md`** — module README documenting current alpha05
  scaffold state + per-target alpha05.X follow-up landing list.
- Maven Central coordinates `io.github.mobilebytelabs:worker-desktop-daemon:3.0.0-alpha05`
  (publishing config lands alongside the alpha05.X shadowJar fat-JAR packaging).

#### Web Push universal background (Phase 9 alpha06 scaffold)

- **`cmp-worker-web-push`** (NEW module — full KMP target matrix: jvm + iosArm64 +
  iosSimulatorArm64 + js(IR) + wasmJs) — scaffold for the universal-browser background
  module. Consumer's server is the cron; sends a Web Push message per RFC 8030 that
  wakes the Service Worker, which then runs pending work entries from IndexedDB.
- **`WebPushConfig`** — `data class` carrying subscription parameters (`enabled`,
  `vapidPublicKey`, `serverEndpoint`, `foregroundFallback`,
  `notificationPermissionAutoRequest`, `serviceWorkerScript`, `subscriptionExpiryDays`).
  Safe defaults: `enabled=false`, `notificationPermissionAutoRequest=false`,
  `foregroundFallback=true`, `subscriptionExpiryDays=90`.
- **`WebPushSubscription`** — `data class` (`endpoint`, `p256dh`, `auth`) for RFC 8030
  subscription metadata. Treat `endpoint` as a bearer secret.
- **`WebPushSubscriber`** — `interface` for subscription lifecycle
  (`ensureSubscribed` / `unsubscribe` / `currentSubscription` / `pushSupported` /
  `requiresPwaInstall`). alpha06 ships log-only stub actuals on JVM/iOS/JS/WasmJs;
  real JS `navigator.serviceWorker.register(...)` + `pushManager.subscribe(...)`
  + WasmJs `@JsFun` bindings land in alpha06.X.
- **`createWebPushSubscriber()`** — top-level `expect fun` factory returning the
  platform actual (alpha06 returns log-only stubs everywhere).
- **`workWebPushKoinModule`** — Koin module exposing `single<WebPushSubscriber>` via
  the factory; consumer registers alongside `workKoinModule(...)`.
- **`worker-kmp-sw.js`** — Service Worker JS template at
  `cmp-worker-web-push/src/jsMain/resources/`. Listens for `push` + `periodicsync`
  events; the IndexedDB read + work dispatch are stub no-ops at alpha06 (alpha06.X
  delivers full SW-context work execution + BroadcastChannel cross-tab dedup).
  CSP-clean: no eval, no document.write, no innerHTML.
- 2 smoke tests in `WebPushSmokeTest` (commonTest): `WebPushConfig` defaults safe,
  stub subscriber returns null + reports `pushSupported=false`.
- **`cmp-worker-web-push/README.md`** — module README documenting current alpha06
  scaffold state + per-target alpha06.X follow-up landing list.
- **`WEB_PUSH_SERVER_GUIDE.md`** — new source-repo-root doc scaffold for consumer
  push servers (RFC 8030 protocol, VAPID key generation, server obligations per
  SECURITY.md T7-T15, reference server placeholders for Node.js + Ktor).
- Maven Central coordinates `io.github.mobilebytelabs:worker-web-push:3.0.0-alpha06`
  (real per-platform JS/WasmJs impls + VAPID Gradle task + reference push servers
  land alongside alpha06.X follow-ups).

#### Migration toolkit (Phase 12 alpha08 scaffold)

- **`cmp-worker-migrate`** (NEW module — JVM-only Gradle plugin, NOT a KMP library)
  — scaffold for the v2.x → v3 source-migration toolkit. Uses
  `kotlin("jvm") + java-gradle-plugin`; declares
  `compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")` for the
  forthcoming AST-driven scanner.
- **`io.github.mobilebytelabs.worker.migrate`** — Gradle plugin ID. Registers two
  tasks under the `worker-kmp` group:
  - `cmpWorkerMigrateCheck` — reports findings as Markdown; no changes.
    alpha08 placeholder logs "no findings"; real V2PatternDetector via Kotlin
    compiler frontend + confidence classifier (HIGH/MEDIUM/LOW) + unified-diff
    generation lands in alpha08.X.
  - `cmpWorkerMigrateApply` — applies HIGH-confidence diffs; emits MEDIUM/LOW
    suggestions to `migrate-suggestions.md`. alpha08 placeholder logs only.
- **`WorkerMigratePlugin`** — plugin entry point in
  `io.github.mobilebytelabs.worker.migrate`. Registers the two tasks above with
  Gradle group `worker-kmp` + descriptions.
- 2 smoke tests in `WorkerMigratePluginTest`: plugin registers both tasks via
  `ProjectBuilder.builder()` + `plugins.apply(...)`; check task lands in
  `worker-kmp` group.
- **`cmp-worker-migrate/README.md`** — module README documenting current alpha08
  scaffold state + per-target alpha08.X follow-up landing list (5 integration
  fixtures: v2-android-app / v2-multiplatform-app / v2-koin-already / v2-hilt-app
  / v2-custom-factory; idempotency tests; Gradle Plugin Portal publishing;
  ≤10-min migration time verification on real ≥30-worker codebase).
- BCV: `cmp-worker-migrate` added to root `apiValidation.ignoredProjects` (Gradle
  plugins do not participate in BCV).
- Maven Central coordinates `io.github.mobilebytelabs:worker-migrate:3.0.0-alpha08`
  (Gradle Plugin Portal listing lands in alpha08.X).

### Telemetry

- **`WorkObserver` SAM interface + `WorkEvent` sealed class** — public SPI in `cmp-worker-kmp`. 4 lifecycle events (Enqueued / Started / Progress / Resulted). Consumers wire OpenTelemetry / Sentry / Firebase Performance bridges; see OBSERVERS.md for patterns.
- **`LoggingWorkObserver`** — out-of-box implementation in `cmp-worker-kmp` core; uses Kermit (`co.touchlab.kermit` v2.0.6) for cross-platform structured logging. Single-line log shape: `worker-kmp <event> id=<uuid> <fields>` at INFO/DEBUG/WARN levels.
- **`TestWorkManager.observedEvents`** — in-test observer; records all simulated lifecycle events for assertions in test code.
- **`OBSERVERS.md`** — public docs at source repo root; documents SPI + LoggingWorkObserver + 3 bridge patterns (OTel / Sentry / Firebase Perf) + per-platform actual wiring roadmap.
- Per-platform observer emission lands in Phases 1/7/8/9 as those phases ship their actual implementations. v2.2.0 ships the SPI + LoggingWorkObserver + TestWorkManager recording.

### Foreground tasks

- **`ForegroundWorker`** — new abstract `CoroutineWorker` subclass in `cmp-worker-kmp` commonMain.
  Long-running, user-visible work that runs outside the platform's background execution window.
  Subclass + override `doWork()` + call `setForeground(ForegroundInfo)` to promote.
- **`ForegroundInfo`** — notification + progress descriptor (`notificationId`, `title`, `message`,
  `progress`, `serviceType`, `cancelAction`).
- **`ForegroundServiceType`** — enum mirroring Android 14's `ServiceInfo.FOREGROUND_SERVICE_TYPE_*`
  constants (13 variants: `DATA_SYNC`, `MEDIA_PLAYBACK`, `MEDIA_PROJECTION`, `CONNECTED_DEVICE`,
  `PHONE_CALL`, `CAMERA`, `MICROPHONE`, `LOCATION`, `HEALTH`, `REMOTE_MESSAGING`, `SHORT_SERVICE`,
  `SPECIAL_USE`, `SYSTEM_EXEMPTED`). No-op on iOS/Desktop/Web.
- **`ForegroundNotSupportedException`** — thrown when the platform cannot honor a
  `setForeground` call (e.g. iOS <17 without UNNotifications permission).
- **`ExperimentalForegroundApi`** — `@RequiresOptIn(level = WARNING)` annotation gating the
  foreground APIs until v3.0.0 GA. Opt-in: `@OptIn(ExperimentalForegroundApi::class)` at use site.
- **`runAsForeground(worker, info)`** — `expect suspend fun` per-platform bridge. **alpha01
  scaffolds log-only stubs across all 4 actuals (jvm, ios, js, wasmJs)** — kermit INFO log only,
  no actual platform promotion. Per-platform actuals land in alpha01.X follow-ups:
  - Android `setForeground(ForegroundInfo)` via androidx.work + Manifest `foregroundServiceType`
  - iOS 17+ `BGContinuedProcessingTaskRequest` + `BGContinuedProcessingTaskUpdate` progress
  - iOS 13-16 `BGProcessingTaskRequest` + `UNNotification` shim
  - Desktop `java.awt.SystemTray` icon + supervisor-scope keepAlive
  - Web persistent Service Worker + Notifications API
- **`FOREGROUND_TASKS.md`** — new source-repo-root doc covering when-to-use, per-platform behavior
  table, current alpha01 stub state, consumer usage example, Android Manifest + iOS Info.plist
  snippets for the alpha01.X landing.
- **`TRUE_BACKGROUND_MATRIX.md`** — scaffold doc for the `TrueBackgroundLevel` enum (lands in
  `BackgroundCapabilities` alpha05 per Phase 8) + per-platform equivalence target matrix.
- 4 new tests in `ForegroundWorkerTest` (cmp-worker-kmp commonTest): defaults sanity,
  service-type field carry, 13-variant enum coverage, `setForeground` smoke test against the
  stub `runAsForeground` actual.

### Performance

- **JMH benchmark module** (`cmp-worker-bench`) — JVM-only module with JMH 1.37 (Gradle plugin `me.champeau.jmh` v0.7.2). Initial benchmarks: `EnqueueBenchmark` (one-time + constraints) and `PersistenceBenchmark` (TestWorkManager in-memory state at N=10/100/1000). `ObserverChainBenchmark` is a stub awaiting Phase 4's `WorkObserver` SPI.
- **Baseline capture** — `scripts/perf-capture-baseline.sh <version>` runs JMH + writes JSON to `perf-baselines/<version>.json`. First baseline captured at v2.2.0 release tag.
- **CI regression gate** — `.github/workflows/perf-check.yml` runs a quick JMH pass on every PR + compares against latest baseline. Fails on >20% slowdown for any benchmark. Bypass label: `skip-perf-check` (use sparingly).
- **`PERFORMANCE.md`** — public-facing performance overview at source repo root; documents metrics, methodology, runner spec, capture instructions.

### Backward compatibility

- **BC test gate** — new `cmp-worker-bc-test` module pulls `worker-kmp:2.1.0` from Maven Central as a v2Compat classpath + current local build as v3Compat. Same test classes run against both; CI fails on divergence. Protects deprecated v2 APIs through the v3.x grace window per RULE-CMD-SLIM-001 + Phase 13.
- **`scripts/bc-test-diff.sh`** + `.github/workflows/bc-test.yml` — CI gate. Bypass label `skip-bc-test`.
- **`BACKWARD_COMPATIBILITY.md`** — deprecation policy + BC test gate documentation at source repo root.
- **`.github/ISSUE_TEMPLATE/bc-break.md`** — GitHub Issue template for reporting BC regressions; auto-labels `bc-break` + `priority/critical`.
- **`:cmp-worker-bc-test:checkDeprecatedCoverage`** — Gradle task scaffold. Currently trivial (no @Deprecated symbols in v2.2.0 baseline). Gains real enforcement at v3.0.0-alpha00 when initializeWorkerXxx() become @Deprecated per Phase 0.

### Security

- **STRIDE threat model** — `SECURITY.md` (28-row matrix) + `SECURITY_ASSUMPTIONS.md` (trust assumptions) + `THREAT_MODEL_TEMPLATE.md` (consumer-extension template) land at the source repo root. Per Phase 10 of the worker-kmp v3.0.0 epic.
- **CI security-scan workflow** — `.github/workflows/security-scan.yml` runs 4 active jobs on every PR + weekly Monday 06:00 UTC: OWASP dependency-check (continue-on-error), gitleaks secret scanner, CycloneDX SBOM generation (org.cyclonedx.bom v1.10.0 Gradle plugin), npm audit (web-push-server-node sample). 2 placeholder jobs (daemon JAR integrity at v3.0.0-alpha05; Service Worker static check at v3.0.0-alpha06) reserved for Phase 8 + Phase 9.
- **SECURITY.md surface-coverage CI gate** — `scripts/security-doc-coverage.sh` + workflow job fails CI when an attack-surface directory is touched without a corresponding SECURITY.md row update.
- **PR template `## Security review` section** — `.github/PULL_REQUEST_TEMPLATE.md` gains a checkbox section between Spec + Quality review stages. Required when attack surface touched.

### Internal

- CycloneDX SBOM Gradle plugin (`org.cyclonedx.bom` v1.10.0) registered at root build.gradle.kts. `./gradlew cyclonedxBom` produces `build/reports/bom.json`.

### Release process + samples

- **`RELEASE.md`** — process doc for v3.0.0-beta01 → rc1 → rc2 → GA progression. Per Phase 14 of the v3.0.0 epic.
- **`POSTMORTEM_TEMPLATE.md`** — template for post-release retros (within 14 days of GA per Phase 14).
- **`.github/ISSUE_TEMPLATE/v3-beta-feedback.md`** — community feedback during beta/RC burn-in.
- **`cmp-worker-sample-ios/`** — scaffold dir + Info.plist guidance. Full Xcode project at v3.0.0-alpha07.
- **`cmp-worker-sample-hilt/`** — scaffold dir + Hilt integration pattern doc. Full Android APK at v3.0.0-alpha07.
- **`cmp-worker-sample/WEB_BROWSER.md`** — marks the alpha07 wasmJsBrowserMain Compose-for-Web sample path.

## [2.1.0] - 2026-05-27

### Infrastructure

#### Binary Compatibility Validator
- Added `org.jetbrains.kotlinx.binary-compatibility-validator` (BCV) v0.17.0 to track public API surface across releases.
- `.api` dump files generated for `cmp-worker-kmp`, `cmp-worker-compose`, `cmp-worker-desktop`, `cmp-worker-web`, `cmp-worker-koin`.
- `cmp-worker-sample`, `cmp-worker-sample-android`, `cmp-worker-test` excluded from API tracking.

#### Dokka Convention Plugin
- Added Dokka 2.0 convention plugin (`io.github.mobilebytelabs.dokka`) following the same pattern as Spotless and Detekt.
- All publishable modules (`cmp-worker-kmp`, `cmp-worker-android`, `cmp-worker-compose`, `cmp-worker-desktop`, `cmp-worker-ios`, `cmp-worker-web`, `cmp-worker-koin`) automatically generate KDoc HTML via `./gradlew dokkaGenerate`.
- Dokka V2 mode enabled (`org.jetbrains.dokka.experimental.gradle.pluginMode=V2EnabledWithHelpers`) for AGP 9.x compatibility.

### Samples

#### Compose Desktop Demo (`cmp-worker-sample`)
- New `ComposeSampleApp` — Compose Desktop window with `WorkSchedulerScreen`, `WorkMonitorScreen`, and `BackgroundCapabilitiesBanner` tabs. Demonstrates full UI workflow from task scheduling to live status monitoring.

#### Android Sample App (`cmp-worker-sample-android`)
- New standalone Android application module with Compose UI, Koin DI wiring, and `SyncWorker`.
- `MainActivity` shows the same two-tab layout (`WorkSchedulerScreen` + `WorkMonitorScreen`) with `BackgroundCapabilitiesBanner`.
- `WorkerSampleApp` calls `initializeWorkerAndroid` and `startKoin { modules(workKoinModule, appModule) }` in `Application.onCreate`.

### Documentation

#### iOS Swift Integration Guide (`cmp-worker-ios/SWIFT_INTEGRATION.md`)
- New step-by-step guide covering: SwiftUI + UIKit setup, `IosWorkerFactory` implementation, enqueueing work from Swift, observing work status via `AsyncSequence`, `BGTaskScheduler` opt-in, and Koin DI integration.

[2.1.0 was originally listed here — entries follow below]

### Added

#### Koin DI Integration (`cmp-worker-koin`) — new artifact

- **`cmp-worker-koin`** — new optional artifact `io.github.mobilebytelabs:worker-koin` that
  provides first-class Koin 4.x dependency-injection support.
- **`workKoinModule`** — a Koin `Module` that registers `WorkManager` as a process-scoped
  singleton backed by `PlatformWorkManager()`. Consumers include it in their `startKoin` block:
  ```kotlin
  startKoin { modules(workKoinModule, appModule) }
  ```
  Resolving `get<WorkManager>()` anywhere in the Koin graph returns the same platform-configured
  instance without passing it by hand.
- Targets: JVM, iOS (iosArm64 + iosSimulatorArm64), JS (IR), WasmJs — same platform matrix
  as `worker-kmp`.
- 6 tests in `WorkKoinModuleTest` (JVM + common): singleton semantics, override pattern,
  `workKoinModule` object non-null, JVM platform-wired resolution, same-instance assertion.
- **Hilt integration guide** added to README — copy-pasteable `HiltWorkerFactory`,
  `WorkerBindingsModule`, and `@EntryPoint` pattern for Android Hilt consumers.
- Koin worker-factory bridge pattern documented — shows how to register workers as Koin
  `factory { (ctx: WorkerContext) -> ... }` entries and bridge them to each platform's
  worker factory interface via `parametersOf(context)`.
- **`KoinSampleApp`** — new runnable JVM sample (`cmp-worker-sample/jvmMain`) that demonstrates
  the full Koin DI flow: init desktop WorkManager with a Koin-backed factory, start Koin with
  `workKoinModule + appModule`, resolve `WorkManager` from the container, enqueue a
  `KoinGreetingWorker` with an injected `GreetingRepository`, and observe the result.

#### Common API (`cmp-worker-kmp`)

- **`BackgroundCapabilities`** — new data class (`supportsPersistence: Boolean`,
  `supportsOsScheduling: Boolean`) with `expect fun platformBackgroundCapabilities()` so shared
  code can query at runtime what the current platform supports without importing any
  platform-specific module.

  | Platform | `supportsPersistence` | `supportsOsScheduling` |
  |---|:---:|:---:|
  | Android | ✓ | ✓ |
  | iOS | ✓ | ✓ |
  | Desktop (JVM) | ✓ | — |
  | Web (JS) | ✓ | — |
  | Web (WasmJs) | — | — |

- **`WorkManager` kdoc** updated to reflect actual per-platform background capabilities and
  point to `platformBackgroundCapabilities()`.
- **`TestWorkManager.simulateRunning(id)`** — new helper for driving RUNNING state transitions
  in unit tests; mirrors the existing `simulateSuccess` / `simulateFailure` / `simulateProgress`.
- 3 new tests in `WorkManagerTest` (31 total):
  - `platformBackgroundCapabilities_returnsNonNullInstance`
  - `platformBackgroundCapabilities_fieldsAreBooleans`
  - `simulateRunning_transitionsToRunning`

#### Compose Multiplatform (`cmp-worker-compose`)

- **`BackgroundCapabilitiesBanner`** — informational `@Composable` banner that shows whether
  Persistence and OS scheduling are available on the current platform. Accepts an explicit
  `BackgroundCapabilities` parameter (defaults to `platformBackgroundCapabilities()`) so it can
  be previewed or tested without a real platform.
- **`WorkCountBadge`** — `@Composable` that overlays a numeric [Material 3 `Badge`] on any
  icon or composable, showing the count of active (non-terminal) work items for a given tag.
  Disappears automatically when the count reaches zero.
- **`rememberActiveWorkCount(tag)`** — reactive `State<Int>` that counts non-terminal work
  items for `tag`; derived from `collectWorkInfosByTagAsState`, updates as work transitions.
- **`WorkManager.hasActiveWork(tag)`** — suspending extension that returns `true` if at least
  one non-terminal work item with `tag` exists; useful for gating UI elements.
- 4 new tests in `FakeWorkManagerTest` (13 total):
  - `platformBackgroundCapabilities_isCallable`
  - `hasActiveWork_falseWhenEmpty`
  - `hasActiveWork_trueWhenRunningItemExists`
  - `hasActiveWork_falseWhenAllTerminal`

#### iOS (`cmp-worker-ios`)

- **`IosWorkManagerConfig`** — new configuration data class (backwards-compatible; all fields have
  safe defaults):
  - `enableBackgroundTasks: Boolean = false` — opt-in BGTaskScheduler integration.
  - `bgProcessingTaskIdentifier: String = ""` — `BGProcessingTask` identifier; must match
    `Info.plist → BGTaskSchedulerPermittedIdentifiers` and be non-empty when
    `enableBackgroundTasks = true`.
  - `enablePersistence: Boolean = true` — work state is written to `NSUserDefaults` so
    pending/running work survives foreground/background transitions and app restarts.
  - `persistenceKey: String = "worker-kmp-ios"` — `NSUserDefaults` key; override when multiple
    app extensions share the same suite to prevent key collisions.
- **`initIosWorkManager(workerFactory, config = DEFAULT)`** — init function now accepts optional
  `IosWorkManagerConfig`; existing call sites compile and behave identically without any change.
- **BGTaskScheduler integration** (when `enableBackgroundTasks = true`):
  - Registers a `BGProcessingTask` handler at init time (before `applicationDidFinishLaunching`
    returns) that calls `runPendingWork()` when iOS wakes the app in the background.
  - `awaitConstraintsSatisfied()` — called before each enqueued worker; schedules a
    `BGProcessingTaskRequest` with `requiresNetworkConnectivity` / `requiresExternalPower`
    mapped from the work request's `Constraints`.
  - `registerBgProcessingTask` / `scheduleBgProcessingTask` — internal helpers wrapping
    `BGTaskScheduler.sharedScheduler`; errors swallowed so polling fallback continues.
- **NSUserDefaults persistence** — `NSUserDefaultsWorkPersistence` serialises each `WorkInfo`
  as a `|`-delimited string stored as an `NSArray`; `restoreFromPersistence()` re-enqueues
  RUNNING items as ENQUEUED (interrupted by an app kill).
- `InMemoryIosWorkPersistence` — in-memory persistence implementation for test isolation.
- **`IosWorkStateStore` thread-safety fix** — all state mutations now use
  `MutableStateFlow.update {}` (CAS-loop) to prevent the race where `transitionToRunning()`
  could overwrite a CANCELLED state set concurrently by `cancelWorkById()`.
- 5 new tests (total: 18 in `IosWorkManagerTest`):
  - `config_defaults_disableBackgroundTasksAndEnablePersistence`
  - `config_canEnableBackgroundTasks`
  - `persistence_save_isCalledOnEnqueue`
  - `persistence_restore_reEnqueuesInterruptedWork`
  - `persistence_restore_keepsFinalStates`

#### Desktop (`cmp-worker-desktop`)

- **File-based persistence** — work state now survives JVM process restarts. `PropertiesFileWorkPersistence`
  writes each `WorkInfo` as a `.properties` file under `persistencePath`; `restoreFromPersistence()`
  re-enqueues RUNNING items as ENQUEUED (interrupted mid-execution by a JVM shutdown).
- `DesktopWorkManagerConfig.persistenceEnabled: Boolean = true` — already existed; now fully wired to
  `PropertiesFileWorkPersistence`. Set `persistenceEnabled = false` (or use `IN_MEMORY` preset) to
  disable all file I/O.
- `DesktopWorkManagerConfig.persistencePath: File` — default `~/.worker-kmp`; override to control
  where `.properties` files are stored (useful for multi-app or sandboxed environments).
- **Internal persistence interface** `DesktopWorkPersistence` with three implementations:
  - `PropertiesFileWorkPersistence` — production: each work item stored as `<id>.properties`.
  - `InMemoryDesktopWorkPersistence` — test isolation: in-memory, zero file I/O.
  - `NoOpDesktopWorkPersistence` — when `persistenceEnabled = false`.
- **`initializeWorkerDesktop(config, workerFactory)`** — init function now accepts an optional
  `DesktopWorkerFactory`; existing call sites compile without changes (defaults to
  `ReflectionWorkerFactory`).
- `DesktopWorkStateStore` — all state mutations now persist/delete via the injected persistence; the
  existing manual CAS-loop (`compareAndSet`) pattern is retained for thread-safety.
- 5 new tests (total: 18 in `DesktopWorkManagerTest`):
  - `persistence_enqueuedWorkRestoredOnRestart`
  - `persistence_runningWorkRestoredAsEnqueued`
  - `persistence_succeededWork_removedFromPersistence`
  - `persistence_cancelledWork_removedFromPersistence`
  - `persistence_disabled_noOp`

#### Web (`cmp-worker-web`)

- **Browser Background Sync API integration** (opt-in, `enableBackgroundSync = false` by default) —
  when enabled, `WebWorkManager` registers a Service Worker sync tag for constrained work so the
  browser can wake the page when connectivity is restored, even across tab focus changes.
- `WebWorkManagerConfig.enableBackgroundSync: Boolean = false` — opt-in flag; when `true`,
  `awaitConstraintsSatisfied` merges a third wake-up source: `backgroundSyncFlow(syncTag)`.
- `WebWorkManagerConfig.serviceWorkerScript: String = "/worker-kmp-sw.js"` — path to the Service
  Worker file served by the host application. Defaults to `/worker-kmp-sw.js`.
- `backgroundSyncServiceWorkerScript(): String` — returns the worker-kmp Service Worker script as
  a `String` so consumers can embed or serve it dynamically without copying a static `.js` resource.
- `cmp-worker-web/src/jsMain/resources/worker-kmp-sw.js` — bundled Service Worker template; copy
  to your web server root (or use `backgroundSyncServiceWorkerScript()` for dynamic serving).
- `expect`/`actual` `isBackgroundSyncSupported(): Boolean` — returns `true` only on JS targets where
  both `serviceWorker` and `SyncManager` are available; `false` on JVM and WasmJs (conservative
  fallback to polling + online-watcher when unsupported).
- `expect`/`actual` `backgroundSyncFlow(tag): Flow<Unit>` — `callbackFlow` that listens for
  `postMessage` events from the Service Worker with `{ type: 'WORKER_KMP_SYNC', tag }`.
- `expect`/`actual` `registerBackgroundSyncTag(tag, swScript)` — registers the Service Worker (if
  not already registered) and calls `registration.sync.register(tag)`; errors are swallowed so
  polling + online-watcher handle the constraint wait as a fallback.
- 4 new Background Sync tests (total: 33 in `WebWorkManagerTest`):
  - `backgroundSync_isDisabled_byDefault`
  - `backgroundSync_notSupported_onJvm`
  - `backgroundSync_whenNotSupported_constraintStillResolves`
  - `backgroundSyncServiceWorkerScript_containsSyncAndMessageHandlers`

## [2.0.0] - 2026-05-26

### Added

#### Web (`cmp-worker-web`)

- **`WebWorkManagerConfig` persistence settings** — two new fields:
  - `enablePersistence: Boolean = true` — set to `false` to skip IndexedDB entirely (work
    survives only for the current page session); useful in SSR, test, or private-browsing contexts.
  - `persistenceDbName: String = "worker-kmp"` — override when multiple apps share the same
    origin to prevent IndexedDB key collisions.
  - Both fields are backwards-compatible; existing code that constructs `WebWorkManagerConfig`
    with only `constraintCheckIntervalMs` continues to compile and behave identically.
- **IndexedDB persistence** — `WebWorkPersistence` internal interface (`save`, `loadAll`, `delete`)
  with `expect fun createWebWorkPersistence(config: WebWorkManagerConfig)` for per-platform actuals.
- `IndexedDbWorkPersistence(dbName: String)` (JS target) — a factory function `buildIdbHelper(dbName)`
  wraps a self-contained `js("(function(dbName){...})")` object that owns the `IDBDatabase`
  connection; all three operations (`put`, `getAll`, `delete`) are Promise-based and awaited via
  `Promise.await()`. Private-browsing / SSR guard: `typeof indexedDB !== 'undefined'`.
- `NoOpPersistence` (JS target, `enablePersistence = false`) — in-memory only; returned by
  `createWebWorkPersistence` when persistence is disabled.
- `NoOpWorkPersistence` (JVM + WasmJs targets) — no-op actual; in-memory state is authoritative.
- `WebWorkStateStore` — now accepts optional `WebWorkPersistence` + `CoroutineScope`; fires
  save/delete persistence calls as fire-and-forget `scope.launch` on every state mutation;
  terminal states (SUCCEEDED, FAILED, CANCELLED) trigger `delete` to keep IndexedDB clean.
- `WebWorkManager` — extended internal constructor with `persistence` param; `init` block
  restores persisted work via `stateStore.restoreFromPersistence()` on startup; RUNNING entries
  restored as ENQUEUED (interrupted by page reload); restore uses `compareAndSet` to avoid
  clobbering in-flight state mutations.
- **Online/offline event-driven constraint wake-up** — `awaitConstraintsSatisfied` now merges
  a `timerFlow` (poll fallback) with `onlineWatcher()` (platform-specific); JS target wires
  `window.addEventListener("online"/"offline")` via `callbackFlow` so network constraint
  re-evaluation fires instantly on reconnect instead of waiting up to 5 seconds.
- `OnlineWatcher.kt` — `internal expect fun onlineWatcher(): Flow<Unit>`; JS actual uses
  `callbackFlow` + `awaitClose` for proper listener lifecycle; JVM + WasmJs use `emptyFlow()`.
- 5 new persistence + constraint tests (26 total in `WebWorkManagerTest`):
  - `persistence_save_isCalledOnEnqueue` — verifies save history after work completes
  - `persistence_delete_isCalledOnTerminalState` — verifies cleanup on SUCCEEDED
  - `persistence_restore_reEnqueuesInterruptedWork` — RUNNING → ENQUEUED on restore
  - `persistence_restore_keepsFinalStates` — SUCCEEDED / FAILED history preserved
  - `enqueue_withUnsatisfiedConstraint_executesWhenConstraintSatisfied_viaEvaluator` — constraint polling with evaluator call-count verification
- **`isWebWorkManagerSupported()`** — `expect`/`actual` progressive-enhancement check: returns
  `true` on JS and WasmJs targets (always a real web runtime), `false` on JVM (test host only).
  Consumers should guard `WebWorkManager` initialisation with this check for SSR / CLI contexts.
- **`ExistingPeriodicWorkPolicy.KEEP` and `UPDATE` correctness** — `enqueueUniquePeriodicWork`
  now properly checks the state store for a non-finished work entry with the same unique-work
  name tag: `KEEP` returns the existing `id` without creating a second job; `UPDATE` (like
  `REPLACE`) cancels the prior entry before enqueuing the new request.
- 3 additional tests (29 total):
  - `enqueueUniquePeriodicWork_keep_returnsExistingIdWithoutCreatingNew`
  - `enqueueUniquePeriodicWork_update_replacesExistingWork`
  - `isWebWorkManagerSupported_returnsFalseOnJvm`

#### Sample (`cmp-worker-sample`)

- **Web / Node.js sample** (`jsMain`) — `WebSampleMain.kt` covers 6 scenarios end-to-end using
  only the public API (`initWebWorkManager`, `PlatformWorkManager()`):
  1. One-time work with typed input/output data
  2. Progress reporting via `setProgress(WorkProgress(percent, workDataOf(...)))`
  3. Retry with exponential backoff (`BackoffPolicy.EXPONENTIAL`, `maxAttempts = 3`)
  4. Unique periodic work (100 ms interval, ~8 executions observed over 850 ms)
  5. `KEEP` policy — second `enqueueUniquePeriodicWork` call returns the first work's `id`
  6. Network constraint declaration (`NetworkType.CONNECTED`)
  Run with: `./gradlew :cmp-worker-sample:jsNodeRun`

#### Compose Multiplatform (`cmp-worker-compose`)

- Added `js(IR)` and `wasmJs` browser targets — all existing composables
  (`WorkMonitorScreen`, `WorkSchedulerScreen`, `WorkInfoCard`, `WorkStatusChip`,
  `WorkProgressIndicator`, `LocalWorkManager`, `WorkManagerProvider`,
  `collectWorkInfosByTagAsState`, `collectWorkInfoByIdAsState`) now compile and run on
  Kotlin/JS and Kotlin/Wasm web targets with no code changes required.
- Fixed: `MenuAnchorType` → `ExposedDropdownMenuAnchorType` (M3 1.4 rename) in
  `WorkSchedulerScreen`.
- Fixed: unnecessary `!!` non-null assertions on `onRetry`/`onCancel` lambdas in
  `WorkInfoCard` — replaced with `?: {}` safe fallback (callers already guard with
  `showRetry`/`showCancel` checks).

#### Web (`cmp-worker-web`)

- `WebConstraintEvaluator` — `internal` SAM interface (`suspend fun evaluate(Constraints): Boolean`)
  injected into `WebWorkManager`; allows test-time substitution without exposing the internal type
  in the public API.
- `WebWorkManagerConfig` — configuration data class with `constraintCheckIntervalMs: Long = 5_000`;
  controls the polling interval used by `awaitConstraintsSatisfied`.
- Constraint-aware execution in `WebWorkManager.enqueue()` — work is deferred until all constraints
  are satisfied; polling loop delegates to `WebConstraintEvaluator`.
- **JS target** (`jsMain`) — `DefaultWebConstraintEvaluator` evaluates all five constraint types:
  - `requiredNetworkType` via synchronous `navigator.onLine`.
  - `requiresBatteryNotLow` via async `navigator.getBattery().then(b => b.level > 0.2)`
    (conservative `true` when Battery Status API unavailable).
  - `requiresCharging` via async `navigator.getBattery().then(b => b.charging)`
    (conservative `true` when Battery Status API unavailable).
  - `requiresStorageNotLow` via async `navigator.storage.estimate()` — passes when
    free quota > 5 MB (conservative `true` when StorageManager unavailable).
- **Wasm target** (`wasmJsMain`) — `DefaultWebConstraintEvaluator` evaluates `requiredNetworkType`
  via `navigator.onLine`; battery/storage return conservative `true` (Battery Status API and
  StorageManager are async and not directly bindable via `@JsFun`).
- **JVM test target** added to `cmp-worker-web` — `commonTest` now runs via `jvmTest` (seconds)
  instead of `jsNodeTest` (minutes); `jsBrowserTest` and `jsNodeTest` both disabled.
- 9 new constraint tests covering battery-not-low, charging, storage-not-low, multi-constraint
  satisfied/unsatisfied scenarios (total: 19 tests in `WebWorkManagerTest`).

### Changed

- `WebWorkManager` primary constructor is now `internal` (takes `workerFactory`, `config`,
  `constraintEvaluator`); the public constructor accepts only `workerFactory` + `config` and
  calls `defaultConstraintEvaluator()` internally — prevents leaking the `internal`
  `WebConstraintEvaluator` type into the public API.

## [1.2.0] - 2026-05-26

### Added

#### Core API (`cmp-worker-kmp`)

- `ConditionalWorker` — abstract `CoroutineWorker` subclass that gates execution on a runtime
  `condition()` check; returns `WorkResult.failure` when condition is `false`, allowing callers
  to skip work based on feature flags, auth state, or resource availability without retrying.
- `DefaultWorkContinuation` — concrete implementation of `WorkContinuation` that executes chain
  steps sequentially: each step is fully awaited before the next is enqueued; the chain halts
  on any `FAILED` or `CANCELLED` step; output data from each step is merged and forwarded as
  input to the next step (accumulated across all prior steps).
- `WorkData.mergeWith()` — internal extension that merges two `WorkData` instances, with the
  right-hand operand's keys taking precedence on collision.

#### Testing (`cmp-worker-test`)

- `WorkContinuationTest` — 10 tests covering single-step, two-step, three-step chains, halt on
  failure/cancel/middle-failure, output data propagation, accumulated output across 3 steps,
  original input preservation when predecessor has no output, and parallel initial steps.
- `WorkContinuationConditionalWorkerTest` — 3 tests covering condition-true execution,
  condition-false skip, and retry passthrough from `doConditionalWork`.

## [1.1.0] - 2026-05-26

### Added

#### Core API (`cmp-worker-kmp`)

- `ContentUriTrigger` — data class representing a content-provider URI trigger (Android-only,
  API 24+); added to `Constraints` via `addContentUriTrigger(uriString, triggerForDescendants)`.

#### Android (`cmp-worker-android`)

- `Constraints.toAndroid()` now maps `contentUriTriggers` to `androidx.work.Constraints.Builder.addContentUriTrigger()`.

#### Compose Multiplatform (`cmp-worker-compose`)

- `WorkStatusChip` — `AssistChip` bound to `WorkInfo.State` with state-coloured icon and label.
- `WorkProgressIndicator` — `LinearProgressIndicator` bound to `WorkProgress`; indeterminate
  when `progress.isIndeterminate`, determinate otherwise; optional status message label.
- `WorkInfoCard` — M3 `Card` displaying work ID, `WorkStatusChip`, `WorkProgressIndicator`
  (when running or progress > 0), output data key-value pairs, Cancel and Retry action buttons.
- `WorkMonitorScreen` — `LazyColumn` of `WorkInfoCard` items observed via `collectWorkInfosByTagAsState`;
  configurable empty-state message and per-item Cancel/Retry callbacks.
- `WorkSchedulerScreen` — full scheduling form with worker class, tag, one-time vs. periodic
  toggle, repeat interval, and network/charging/battery constraint checkboxes; calls `onSchedule`
  with the built `WorkRequest` on submission.

## [1.0.0] - 2026-05-26

Initial release of worker-kmp — a Kotlin Multiplatform WorkManager library with a
unified API across Android, iOS, Desktop (JVM), and Web (JS/WasmJs).

### Modules

| Artifact | Description |
|---|---|
| `io.github.mobilebytelabs:cmp-worker-kmp` | Core API — interfaces, models, DSL helpers |
| `io.github.mobilebytelabs:cmp-worker-android` | Android implementation backed by `androidx.work` |
| `io.github.mobilebytelabs:cmp-worker-ios` | iOS implementation (foreground-only) |
| `io.github.mobilebytelabs:cmp-worker-desktop` | JVM/Desktop implementation via coroutine executor |
| `io.github.mobilebytelabs:cmp-worker-web` | JS/WasmJs implementation (foreground-only) |
| `io.github.mobilebytelabs:cmp-worker-compose` | Compose Multiplatform extensions (`LocalWorkManager`, `WorkInfo` extensions) |
| `io.github.mobilebytelabs:cmp-worker-test` | Testing utilities — `TestWorkManager`, fake implementations |
| `io.github.mobilebytelabs:cmp-worker-sample` | Runnable JVM sample demonstrating all worker patterns |

### Added

#### Core API (`cmp-worker-kmp`)

- `WorkManager` interface — `enqueue`, `enqueueUniquePeriodicWork`, `cancelWorkById`,
  `cancelAllWorkByTag`, `getWorkInfosByTag` (Flow), `getWorkInfoById`
- `CoroutineWorker` — abstract base class with `doWork(): WorkResult`, `setProgress()`,
  `inputData`, `tags`, and `id`
- `WorkRequest` sealed class with two concrete types:
  - `OneTimeWorkRequest` — single execution; builder via `oneTimeWorkRequest<T> {}` DSL
  - `PeriodicWorkRequest` — repeating schedule; builder via `periodicWorkRequest<T>(interval) {}` DSL
- `WorkResult` sealed class — `Success(outputData)`, `Failure(message, outputData)`, `Retry(reason)`
- `WorkInfo` data class — `id`, `state`, `progress`, `outputData`, `tags`, `runAttemptCount`, `isFinished`
- `WorkInfo.State` enum — `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `BLOCKED`
- `WorkData` — typed key-value store (`String`, `Int`, `Long`, `Float`, `Boolean`, `Array<String>`)
  with `workDataOf()` DSL helper
- `WorkProgress` — percentage (0–100) + optional data; `NONE`, `COMPLETE` constants, `of()` factory
- `Constraints` — `NetworkType`, `requiresCharging`, `requiresDeviceIdle`, `requiresBatteryNotLow`,
  `requiresStorageNotLow`; builder DSL via `Constraints { }` operator
- `NetworkType` enum — `NOT_REQUIRED`, `CONNECTED`, `UNMETERED`, `NOT_ROAMING`, `METERED`
- `ExistingPeriodicWorkPolicy` enum — `KEEP`, `REPLACE`, `UPDATE`
- `RetryConfig` — `maxAttempts`, `initialDelay`, `maxDelay`, `BackoffPolicy` (EXPONENTIAL / LINEAR),
  `multiplier`; presets `DEFAULT`, `AGGRESSIVE`, `CONSERVATIVE`
- `WorkContinuation` interface — chainable sequential/parallel work via `beginWith()` + `then()` + `enqueue()`
- `WorkerContext` interface — platform bridge for ID, input data, tags, and progress reporting
- `@ExperimentalWorkerApi` opt-in annotation — marks iOS and Web factory APIs as foreground-only
- Exception types: `WorkEnqueueException`, `WorkerInstantiationException`, `WorkDataSerializationException`

#### Android (`cmp-worker-android`)

- `AndroidWorkManager` — delegates to `androidx.work.WorkManager` with full constraint,
  scheduling, and background execution support
- `KmpAndroidWorker` — bridges `androidx.work.CoroutineWorker` to the KMP `CoroutineWorker` contract
- `KmpWorkerFactory` — `androidx.work.WorkerFactory` that instantiates KMP workers by class name
- `AndroidWorkManagerInit.initAndroidWorkManager()` — one-call initialisation helper

#### iOS (`cmp-worker-ios`)

- `IosWorkManager` — foreground-only implementation; requires `@OptIn(ExperimentalWorkerApi::class)`
- `IosWorkStateStore` — in-memory state store with `StateFlow`-backed observation
- `IosWorkManagerInit.initIosWorkManager()` — one-call initialisation helper

#### Desktop (`cmp-worker-desktop`)

- `DesktopWorkManager` — coroutine-based executor with configurable thread pool
- `DesktopWorkStateStore` — thread-safe in-memory state store
- `DesktopConstraintEvaluator` — best-effort network and battery constraint checking on JVM
- `DesktopWorkManagerConfig` — pool size and shutdown-timeout configuration
- `DesktopWorkManagerInit.initDesktopWorkManager()` — one-call initialisation helper

#### Web / JS / WasmJs (`cmp-worker-web`)

- `WebWorkManager` — foreground-only coroutine-based implementation; requires `@OptIn(ExperimentalWorkerApi::class)`
- `WebWorkStateStore` — in-memory state store with `StateFlow`-backed observation
- `WebWorkManagerInit.initWebWorkManager()` — one-call initialisation helper for JS and WasmJs targets

#### Compose Multiplatform (`cmp-worker-compose`)

- `LocalWorkManager` — `CompositionLocal` for injecting `WorkManager` into the Compose tree
- `WorkInfo` extension functions for Compose-friendly state observation

#### Testing (`cmp-worker-test`)

- `TestWorkManager` — in-memory `WorkManager` implementation for unit tests; supports
  manual state transitions, result injection, and run-attempt simulation
- Full test suite for `TestWorkManager` covering all scheduling and cancellation scenarios

### Infrastructure

- Gradle 9.5.1, Kotlin 2.3.21, AGP 9.2.1, Compose Multiplatform 1.11.0
- Build-logic convention plugins for Spotless (ktlint 1.8.0) and Detekt (1.23.8)
- Reusable CI via `MobileByteLabs/mbl-actionhub` — full matrix on push, fast JVM-only gate on PRs
- Maven Central publishing via `vanniktech/gradle-maven-publish-plugin` 0.30.0
- Single version source of truth in `gradle.properties` (`worker.version`)

[Unreleased]: https://github.com/MobileByteLabs/worker-kmp/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/MobileByteLabs/worker-kmp/compare/v1.2.1...v2.0.0
[1.2.0]: https://github.com/MobileByteLabs/worker-kmp/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/MobileByteLabs/worker-kmp/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/MobileByteLabs/worker-kmp/releases/tag/v1.0.0
