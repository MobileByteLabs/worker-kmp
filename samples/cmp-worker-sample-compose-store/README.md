# cmp-worker-sample-compose-store

Full Compose Multiplatform sample combining all four worker-kmp integration libraries, running on **all 5 KMP targets** (Android, JVM Desktop, iOS arm64, iOS sim arm64, Web wasmJs).

- **`cmp-worker-kmp`** — core `CoroutineWorker` / `WorkManager` API.
- **`cmp-worker-store5`** — `StoreBackedWorker<K, V>` driving a
  [Mobile Native Foundation Store5](https://github.com/MobileNativeFoundation/Store)
  store.
- **`cmp-worker-koin`** — `workKoinModule(...)` wires the `WorkManager` backend +
  the `WorkerRegistry` into the DI graph.
- **`cmp-worker-compose`** — `WorkManagerProvider`, `LocalWorkManager`,
  `WorkInfoCard`, `BackgroundCapabilitiesBanner` — Compose-Multiplatform UI
  primitives that consume worker state directly.

And the platform launcher helpers added in the `worker-kmp-cmp-launchers` epic:

- **`cmp-worker-android`** — `WorkerKmpComposeActivity`, `WorkerKmpStarterApplication`.
- **`cmp-worker-desktop`** — `launchDesktopWorkerApp(title, koinModules, content)`.
- **`cmp-worker-ios`** — `workerKmpMainViewController(koinModules, content)`.
- **`cmp-worker-web`** — `launchWebWorkerApp(canvasElementId, koinModules, content)`.

## What it does

1. The user enters an article id and taps **Sync**.
2. `WorkManager.enqueue` schedules an `ArticleSyncWorker` (subclass of
   `StoreBackedWorker<String, Article>`).
3. The worker forces a fresh fetch on the shared Store5 store; the fake fetcher
   simulates network latency + an occasional transient error.
4. The UI observes **two** independent signals from the same underlying state:
   - the `WorkInfo` flow (tag = `article-sync`) drives a live `WorkInfoCard`,
   - the Store cached stream (`StoreReadRequest.cached`) drives an article details panel.

## Run

| Target | Command |
|--------|---------|
| **Android** | `./gradlew :samples:cmp-worker-sample-compose-store:installDebug` then launch from device/emulator |
| **JVM Desktop** | `./gradlew :samples:cmp-worker-sample-compose-store:jvmRun -DmainClass=io.github.mobilebytelabs.worker.sample.composestore.MainKt --quiet` |
| **iOS Simulator** | See `iosApp/README.md` — `xcodegen generate` then `xcodebuild … build` |
| **Web (wasmJs)** | `./gradlew :samples:cmp-worker-sample-compose-store:wasmJsBrowserDevelopmentRun` then open `http://localhost:8080` |

## Shape

UI lives entirely in `commonMain` (`ui/SampleApp.kt` — no parameters, pulls
dependencies via `koinInject`). Per-platform launcher files are ≤10 source lines
each, each calling the matching library API:

| Platform | Launcher file | Line count* | Library API used |
|----------|---------------|------------:|------------------|
| Android | `androidMain/SampleApplication.kt` | 3 | `WorkerKmpStarterApplication` |
| Android | `androidMain/MainActivity.kt` | 1 | `WorkerKmpComposeActivity` |
| Desktop | `jvmMain/Main.kt` | 4 | `launchDesktopWorkerApp` |
| iOS | `iosMain/MainViewController.kt` | 3 | `workerKmpMainViewController` |
| Web | `wasmJsMain/Main.kt` | 4 | `launchWebWorkerApp` |

\* excluding imports + package + blank lines

The same `SampleApp()` composable runs on all 5 targets — the only per-platform
variance is which `WorkManagerFactory` is plugged into `sampleKoinModules()`.

## File map

| File | Layer |
|------|-------|
| `src/commonMain/.../domain/Article.kt` | Domain model |
| `src/commonMain/.../store/ArticlesStore.kt` | Store5 `Store<String, Article>` + fake fetcher |
| `src/commonMain/.../workers/ArticleSyncWorker.kt` | `StoreBackedWorker` subclass |
| `src/commonMain/.../di/AppModule.kt` | Koin modules (UI + worker registry) |
| `src/commonMain/.../di/SampleKoinSetup.kt` | `sampleKoinModules(factory)` shared by every launcher |
| `src/commonMain/.../ui/SampleApp.kt` | Root composable (no params; uses `koinInject`) |
| `src/commonMain/.../ui/ArticlesScreen.kt` | Interactive screen |
| `src/androidMain/AndroidManifest.xml` | Android app manifest |
| `src/androidMain/.../SampleApplication.kt` | Android `Application` (3 lines) |
| `src/androidMain/.../MainActivity.kt` | Android launcher Activity (1 line) |
| `src/jvmMain/.../Main.kt` | JVM desktop `fun main()` (4 lines) |
| `src/iosMain/.../MainViewController.kt` | iOS launcher exposed to Swift (3 lines) |
| `src/wasmJsMain/.../Main.kt` | wasmJs `fun main()` (4 lines) |
| `src/wasmJsMain/resources/index.html` | Web HTML shell with `<canvas id="composeCanvas">` |
| `iosApp/iOSApp.swift` + `iosApp/ContentView.swift` | SwiftUI host wrapping `MainViewControllerKt.MainViewController()` |
| `iosApp/project.yml` | xcodegen spec — generates `iosApp.xcodeproj` |
