# Changelog

All notable changes to worker-kmp will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/MobileByteLabs/worker-kmp/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/MobileByteLabs/worker-kmp/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/MobileByteLabs/worker-kmp/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/MobileByteLabs/worker-kmp/releases/tag/v1.0.0
