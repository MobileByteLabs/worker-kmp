# Add `sync/` module backed by worker-kmp (cross-platform background sync)

> Drafted by the worker-kmp team in
> [`MobileByteLabs/worker-kmp:samples/kmp-project-template/PR_README.md`](https://github.com/MobileByteLabs/worker-kmp/blob/development/samples/kmp-project-template/PR_README.md).
> Source-of-truth lives there; this file is the artifact for the maintainer to copy
> into a PR against `openMF/kmp-project-template`.

## Summary

Adds a `sync/` Gradle module that provides cross-platform background sync via the
[worker-kmp](https://github.com/MobileByteLabs/worker-kmp) library
(`io.github.mobilebytelabs:worker-compose-all:3.0.2`). Architecture mirrors
[android/nowinandroid:sync](https://github.com/android/nowinandroid/tree/main/sync) —
a `CoroutineWorker` driving constructor-injected `Syncable` repositories in parallel, with
Android backed by WorkManager and other platforms via worker-kmp's per-platform actuals.

## What changed

| Area | Change |
|---|---|
| `build-logic/convention/` | New `WorkerComposeConventionPlugin` registered as `org.convention.worker.compose`. Applies worker-app plugin + adds `worker-compose-all` + `koin-compose` deps + `@ExperimentalWorkerApi` opt-in. |
| `gradle/libs.versions.toml` | `+ worker-version = "3.0.2"`, `+ worker-compose-all`, `+ worker-app-plugin`, `+ worker-app` (plugin). |
| `core/data/.../infra/` | `+ Synchronizer.kt` (Synchronizer + Syncable + NetworkChange interfaces + `changeListSync` + `snapshotSync` extensions — ported verbatim from NiA's SyncUtilities.kt). `+ ChangeListVersions.kt` data class. Placed under existing `infra/` package alongside template's NetworkMonitor/TimeZoneMonitor — minimal directory churn. |
| `core/data/.../util/SyncManager.kt` | `+ SyncManager` observer interface (`isSyncing: Flow<Boolean>` + `requestSync()`). NiA verbatim port; lives under `util/` per NiA convention. |
| `core/data/.../currency/CurrencyRepository.kt` | Implements `Syncable` (Frankfurter pilot — syncs FX rates from network). |
| `core/data/.../economic/MacroIndicatorsRepository.kt` | Implements `Syncable` (World Bank pilot — syncs macro indicators). |
| `core/datastore/` | `+ SyncStatePersister.kt` — DataStore-backed JSON persistence of `ChangeListVersions`. |
| `sync/` (NEW MODULE) | `DataSyncWorker` (CoroutineWorker + Synchronizer, constructor-injects both repos), `NotificationWorker`, `WorkScheduler` façade (7 scheduling methods), `DefaultWorkScheduler`, per-platform `SyncManager` (Android WorkManager-backed; iOS/Desktop/Web stubs), Koin module. |
| `cmp-navigation/KoinModules.kt` | `+ SyncModule` in `allModules`. |
| `cmp-android/AndroidApp.kt` | `+ Sync.initialize(scheduler)` in `onCreate`. |

## Why

The template has no background sync solution. worker-kmp is the cross-platform
equivalent of Android's WorkManager built specifically for Kotlin Multiplatform +
Compose Multiplatform — adopting it shares sync code in `commonMain` instead of writing
four separate per-platform implementations.

Nia's sync pattern is well-understood and battle-tested; we port it verbatim (pure-Kotlin
interfaces) and adapt the actual workers + scheduling to use worker-kmp's API.

The `WorkScheduler` façade exposes 7 scheduling entry points covering one-time, periodic,
daily-at-time, exact-time, and notification-at-time scheduling — demonstrated via
`LoanReminderUseCase` in `feature/loans` as the cross-module ergonomics test.

## v1 scope

- **Android**: full WorkManager-backed sync. Production-ready.
- **iOS / Desktop / Web**: `SyncManager` stubs that exercise the integration shape but
  do not schedule periodic background work. Real cross-platform scheduling is planned as
  a follow-up worker-kmp epic (BGTaskScheduler / OS daemons / Service Worker).

## Testing

- [x] `./gradlew :sync:commonTest` PASS (TDD `DefaultWorkSchedulerTest`, `DataSyncWorkerTest`, `NotificationWorkerTest`)
- [x] `./gradlew :core:data:commonTest` PASS (`SyncableTest` on `CurrencyRepository` + `MacroIndicatorsRepository` pilots)
- [x] `./gradlew assemble` PASS across all KMP targets (Android, JVM, iOS arm64+sim, wasmJs)
- [x] Android smoke test: install on emulator, observe `SyncManager.isSyncing` flips
      `false→true→false` within ~5 seconds of `App.onCreate`
      (logcat: `DataSyncWorker: doWork started/completed`)

## Breaking changes

None. Pure additive — existing modules untouched except for additive Koin bindings.

## Maintenance

- worker-kmp version pinned in `gradle/libs.versions.toml` (`worker-version = "3.0.2"`).
  Bump in one place to update.
- The integration is regression-tested in `MobileByteLabs/worker-kmp:samples/kmp-project-template/`
  — every worker-kmp PR runs `./gradlew :samples:kmp-project-template:assemble` so this
  surface never silently breaks.
