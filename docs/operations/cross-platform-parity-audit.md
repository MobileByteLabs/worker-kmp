# Cross-platform worker parity — Audit report

> **Last audited:** 2026-06-01 against commit `f6038d9` on `development`.
> **Method:** 6-subagent parallel-Explore probe + local-grep harness.
> **Re-audit:** nightly via `.github/workflows/parity-audit.yml`.
> **Harness:** `scripts/run-parity-audit.sh` — `--verify` mode for CI consistency check.

This page is the canonical evidence ledger for worker-kmp's "single commonMain API for background + foreground worker scheduling across all 4 platforms" claim. Every cell cites a source file and line range so the claim is defensible without manual code archaeology. The harness refreshes the matrix nightly; future audits don't require this kind of one-shot probe.

---

## Summary

| Platform | Background | Foreground | Single-API claim |
|---|:--:|:--:|:--:|
| Android (API 21+) | ✓ 15/15 | ✓ | ✓ |
| iOS (13+) | ✓ 8/8 | ✓ (17+: ContinuedProcessing · ≤16: BGProcessingTask + UNNotification shim) | ✓ |
| Desktop (JVM 11+) | ✓ 7/8 in-process + 3/3 OS daemons | ✓ in-process + daemon dispatch | ✓ |
| Web (JS) | ✓ | ⚠ SW notification analog only | ✓ |
| Web (WasmJs) | ✓ | ⚠ SW notification analog only | ✓ |

**Bottom line:** As of 2026-06-01, worker-kmp delivers a single commonMain API (51 public symbols under `io.github.mobilebytelabs.worker.*`) for both background AND foreground worker scheduling. Consumer commonMain references zero `androidx.work.*` / `BGTaskScheduler` / `ServiceWorker` symbols. Web's "foreground" is intentionally degenerate — there's no native foreground concept; the SW + notification analog is documented as designed behaviour, not a gap.

---

## commonMain API surface

51 public symbols across `cmp-worker-kmp/src/commonMain/kotlin/io/github/mobilebytelabs/worker/`. Single namespace.

| Category | Symbol | File:line |
|---|---|---|
| Core | `WorkManager` interface | `WorkManager.kt` |
| Core | `CoroutineWorker` abstract class | `CoroutineWorker.kt` |
| Core | `WorkResult` (Success/Failure/Retry) | `WorkResult.kt` |
| Requests | `WorkRequest` + `OneTimeWorkRequest` + `PeriodicWorkRequest` | `WorkRequest.kt` |
| Requests | `oneTimeWorkRequest<T> { }` / `periodicWorkRequest<T> { }` DSL | `WorkRequestDsl.kt` |
| Constraints | `Constraints` builder + `NetworkType` enum | `Constraints.kt` |
| Constraints | `ContentUriTrigger` | `ContentUriTrigger.kt` |
| Constraints | `ExistingPeriodicWorkPolicy` enum | `ExistingPeriodicWorkPolicy.kt` |
| Retry | `RetryConfig` + `BackoffPolicy` enum | `RetryConfig.kt` |
| Lifecycle | `WorkInfo` (state machine) + `WorkProgress` | `WorkInfo.kt` |
| Lifecycle | `WorkContinuation` interface | `WorkContinuation.kt` |
| Data | `WorkData` + `workDataOf(...)` builder | `WorkData.kt` |
| Context | `WorkerContext` interface | `WorkerContext.kt` |
| **Foreground** | **`ForegroundWorker` abstract class** | **`ForegroundWorker.kt`** |
| **Foreground** | **`ForegroundInfo` data class** | **`ForegroundWorker.kt:103-110`** |
| **Foreground** | **`ForegroundServiceType` enum (13 vals)** | **`ForegroundWorker.kt:122-136`** |
| **Foreground** | **`runAsForeground()` expect suspend fun** | **`ForegroundWorker.kt:148`** |
| **Foreground** | **`ForegroundNotSupportedException`** | **`ForegroundWorker.kt:156-157`** |
| Annotations | `@ExperimentalForegroundApi(level = WARNING)` | `ExperimentalForegroundApi.kt:10-17` |
| Platform | `platformBackgroundCapabilities()` expect fun | `BackgroundCapabilities.kt` |
| Platform | `PlatformContext` expect class | `PlatformContext.kt` |
| Observer | `WorkObserver` SAM + `WorkEvent` + `LoggingWorkObserver` | `WorkObserver.kt` |
| Registry | `WorkerRegistry` + `workerRegistry { }` DSL | `WorkerRegistry.kt` |
| Config | `WorkerConfig` + 5 per-platform configs | `config/*.kt` |
| Exceptions | `WorkEnqueueException` / `WorkerInstantiationException` / etc. | various |

**Single-API claim test:** a consumer's commonMain Worker can schedule background + foreground work using only `io.github.mobilebytelabs.worker.*` symbols. Platform-specific calls (`androidx.work.*`, `BGTaskScheduler.*`, `navigator.serviceWorker.*`) stay platform-side, hidden via expect/actual + factory pattern.

---

## Per-platform feature matrix

### Android (`cmp-worker-android`)

| AndroidX feature | Wired? | Evidence |
|---|:--:|---|
| `OneTimeWorkRequest` | ✓ | `AndroidWorkManager.kt:58-84` |
| `PeriodicWorkRequest` | ✓ | `AndroidWorkManager.kt:86-105` |
| `WorkManager.enqueue` | ✓ | `AndroidWorkManager.kt:24-27` |
| `enqueueUniquePeriodicWork` | ✓ | `AndroidWorkManager.kt:29-40` |
| `Constraints` (all 6 types incl. ContentUri) | ✓ | `WorkConverters.kt:45-59` |
| `setExpedited` (API 31+ foreground) | ✓ | `AndroidWorkManager.kt:73-82` (SDK_INT-guarded) |
| `CoroutineWorker.setForeground(ForegroundInfo)` | ✓ | `AndroidForegroundBridge.kt:74-98` (reflective bridge — avoids Android SDK dep in cmp-worker-kmp JVM core) |
| ForegroundService notifications | ✓ | `AndroidForegroundBridge.kt:114-128` (NotificationCompat builder + progress) |
| `setBackoffCriteria` (linear/exponential) | ✓ | `AndroidWorkManager.kt:63-67` |
| `InputData` / `outputData` passthrough | ✓ | `AndroidWorkManager.kt:107-121` |
| `WorkContinuation.beginWith().then(...)` | ✓ | `WorkContinuation.kt:65-74` |
| `beginUniqueWork(name, ExistingWorkPolicy)` | ✓ | Wired via this audit's sub-plan 03 — `AndroidWorkManager.kt` `beginUniqueWork()` method |
| `getWorkInfosByTag` Flows | ✓ | `AndroidWorkManager.kt:50-51` |
| `getWorkInfoById` snapshot | ✓ | `AndroidWorkManager.kt:53-54` |
| `cancelWorkById` / `cancelAllWorkByTag` | ✓ | `AndroidWorkManager.kt:42-48` |

**ForegroundServiceType mapping:** all 13 Android variants (DATA_SYNC, MEDIA_PLAYBACK, MEDIA_PROJECTION, CONNECTED_DEVICE, PHONE_CALL, CAMERA, MICROPHONE, LOCATION, HEALTH, REMOTE_MESSAGING, SHORT_SERVICE, SPECIAL_USE, SYSTEM_EXEMPTED) supported. Bridge is reflective so the core `cmp-worker-kmp` module stays Android-SDK-free.

### iOS (`cmp-worker-ios`)

| Native iOS background API | Wired? | Evidence |
|---|:--:|---|
| `BGAppRefreshTask` (short, periodic) | ✓ | `BgTaskScheduler.kt:59-68` |
| `BGProcessingTask` (long, constrained) | ✓ | `BgTaskScheduler.kt:40-48` |
| `BGContinuedProcessingTaskRequest` (iOS 17+, foreground equivalent) | ✓ | Sub-plan 02 of this epic — `BgContinuedProcessing.kt` (@ObjCName interop layer mirroring the upstream iOS 17 ObjC interface). Pre-epic: falls back to BGProcessingTask + UNNotification shim. |
| `BGTaskScheduler.register(forTaskWithIdentifier:)` | ✓ | `BgTaskScheduler.kt:18` |
| `BGTaskScheduler.submit(BGTaskRequest)` | ✓ | `BgTaskScheduler.kt:46, 66` |
| `BGTaskScheduler.cancel(forTaskWithIdentifier:)` | ✓ | Sub-plan 04 of this epic — `IosWorkManager.cancelTask(identifier: String)` |
| Task expiration handler | ✓ | `BgTaskScheduler.kt:31-34` |
| Info.plist `BGTaskSchedulerPermittedIdentifiers` validation | ✓ | `InfoPlistValidator.kt` (validation at init + Kermit error logging) |

**iOS ≤16 foreground behaviour:** intentional graceful degradation — `BGProcessingTaskRequest` + `UNUserNotificationCenter.requestAuthorization(...)` shim provides "user-visible work" semantics. Documented at `ForegroundWorker.ios.kt:58-70`.

**Info.plist coordination:** consumer adds the task identifiers manually. No codegen helper (yet). `InfoPlistValidator.kt:25-51` checks `UIBackgroundModes` + `BGTaskSchedulerPermittedIdentifiers` at init; logs ERROR if missing.

### Desktop / JVM (`cmp-worker-desktop` + `cmp-worker-desktop-daemon`)

#### In-process queue

| Feature | Wired? | Evidence |
|---|:--:|---|
| `OneTimeWorkRequest` scheduling | ✓ | `DesktopWorkManager.kt:53-65` |
| `PeriodicWorkRequest` (recurring) | ✓ | `DesktopWorkManager.kt:67-86` |
| Work persists while app open | ✓ | Coroutine-backed + `jobRegistry` + `stateStore` |
| File-based persistence (survives app restart) | ✓ | `PropertiesFileWorkPersistence.kt:19-52` (default `~/.worker-kmp`) |
| Constraints (network / storage) | ✓ | `DesktopConstraintEvaluator.kt:14-38` — battery/idle N/A on desktop |
| Retry with backoff (linear/exponential) | ✓ | `DesktopWorkManager.kt:116-135` |
| `WorkInfo` Flows + lifecycle | ✓ | `DesktopWorkManager.kt:102` |
| Cancel by ID / tag | ✓ | `DesktopWorkManager.kt:88-100` |

#### OS-scheduler daemon (foreground/persistent equivalent)

| Feature | Wired? | Evidence |
|---|:--:|---|
| Windows Task Scheduler XML | ✓ | `WindowsTaskInstaller.kt:28-157` (`schtasks /Create /XML`) |
| macOS launchd plist | ✓ | `MacosLaunchdInstaller.kt:25-134` (`launchctl load -w` of `~/Library/LaunchAgents/`) |
| Linux systemd-user (preferred) | ✓ | `LinuxSystemdInstaller.kt:26-172` (`~/.config/systemd/user/{appId}.worker-kmp.{service,timer}`) |
| Linux cron (fallback) | ✓ | `LinuxCronInstaller.kt:25-131` |
| OS-detect router | ✓ | `LinuxInstallerRouter.kt:21-72` + `DesktopInstallerFactory.kt:17-25` |
| Daemon dispatches ENQUEUED work | ✓ | Sub-plan 05 of this epic — persistence schema v2 + `DaemonWorkerRegistry` + JAR-manifest factory loading |

**Daemon scope:** pre-epic the daemon could only flip RUNNING → ENQUEUED (heal stuck states) — couldn't dispatch directly. Sub-plan 05 closes that by bumping persistence to schema v2 (carries full `WorkRequest` payload) + adding a daemon-side `WorkerRegistry` loaded via JAR manifest.

### Web (`cmp-worker-web` + `cmp-worker-web-push`)

#### Background (JS + WasmJs)

| Feature | Wired? | Evidence |
|---|:--:|---|
| In-tab background work | ✓ | `WebWorkManager.kt:45-47` (`SupervisorJob() + Dispatchers.Default`) |
| ServiceWorker registration + lifecycle | ✓ | `BackgroundSync.kt:40-43` (optional, Koin-integrated) |
| SW periodic-sync (browser permission-gated) | ✓ | `BackgroundSync.kt:72-80` (`enablePeriodicBackgroundSync` config) |
| SW background-sync (post-online retry) | ✓ | `BackgroundSync.kt:49-57` (`enableBackgroundSync` config) |
| IndexedDB persistence | ✓ | `IndexedDbWorkPersistence.kt` (factory `buildIdbHelper`, store `work_infos`; NoOpPersistence fallback for private browsing) |
| Cancel + observe (cross-tab) | ✓ | `BroadcastChannelBridge.{js,wasmJs}.kt` (sub-plan 06 of this epic delivers WasmJs real impl; JS impl pre-existing) |

#### Web Push wake-up

| Feature | JS | WasmJs |
|---|:--:|:--:|
| `navigator.serviceWorker.register()` | ✓ pre-existing (`JsWebPushSubscriber.js.kt`) | ✓ (sub-plan 06 — `@JsFun` binding) |
| `pushManager.subscribe()` | ✓ pre-existing | ✓ (sub-plan 06) |
| Server subscription POST | ✓ pre-existing | ✓ (sub-plan 06) |
| `currentSubscription()` / `unsubscribe()` | ✓ | ✓ (sub-plan 06) |

#### Foreground equivalent on Web

Web has **no native foreground concept**. Closest analog: `reg.showNotification(...)` from the Service Worker while doing background work. Documented as **designed behaviour, not a gap** (per Locked Decision D3 of the parity-audit GOAL.md).

### Consumer server-side Web Push library

**NOT shipped by worker-kmp.** Consumer's responsibility to implement the server-side fan-out (RFC 8030 + VAPID + encrypted subscriptions at rest + rate-limit subscribe endpoint + log redaction). See `docs/features/web-push-server.md` for the requirement set.

---

## Test coverage

| Module | commonTest files | Foreground-specific |
|---|:--:|---|
| `cmp-worker-kmp` | 14 | `ForegroundWorkerTest.kt` + `ForegroundNotSupportedExceptionTest.kt` |
| `cmp-worker-android` | 3 | Pre-epic: background only. This epic's `worker-kmp-platform-engine-tests` sibling epic adds foreground tests. |
| `cmp-worker-ios` | 2 | + `BgContinuedProcessingTest.kt` (sub-plan 02), `IosCancelTaskTest.kt` (sub-plan 04) |
| `cmp-worker-desktop` | 2 | + `PropertiesFileWorkPersistenceV2Test.kt` (sub-plan 05) |
| `cmp-worker-desktop-daemon` | 1 (unit) | + `DaemonDispatchTest.kt` (sub-plan 05 integration test) |
| `cmp-worker-web` | 37 (JS) + 1 (smoke) | n/a — Web foreground is not in scope |
| `cmp-worker-web-push` | 1 (smoke) | + `WasmWebPushSubscriberTest.kt` (sub-plan 06) |
| `cmp-worker-scheduler` | 7 | n/a |
| `cmp-worker-store5` | 5 | n/a |
| `cmp-worker-storeflow` | 5 | n/a |
| `cmp-worker-compose` | 2 | n/a |
| `cmp-worker-koin` | 1 | n/a |
| `cmp-worker-test` | 1 | n/a |

**Coverage milestone (kover-100-coverage epic — merged 2026-06-01):** the 5 Tier-1 commonMain-shaped modules ship at **100% line coverage** (`koverVerify` enforced in CI). Tier-2 platform-actual coverage is owned by the [`worker-kmp-platform-engine-tests`](../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-platform-engine-tests/GOAL.md) sibling epic which adds Robolectric (Android) + Xcode-sim (iOS) + browser-test (Web) harnesses.

---

## What was a gap (now closed by this epic's sub-plans)

| Gap | Status | Closed by |
|---|:--:|---|
| G1 — iOS 17+ `BGContinuedProcessingTaskRequest` not bound | ✓ Closed | Sub-plan 02 — @ObjCName interop layer in `BgContinuedProcessing.kt` |
| G2 — Android `beginUniqueWork(name, ExistingWorkPolicy, request)` not exposed | ✓ Closed | Sub-plan 03 — added to commonMain `WorkManager` interface + all 4 platform actuals |
| G3 — iOS `BGTaskScheduler.cancel(forTaskWithIdentifier:)` not exposed | ✓ Closed | Sub-plan 04 — `IosWorkManager.cancelTask(identifier:)` |
| G4 — Desktop daemon can't dispatch ENQUEUED work directly | ✓ Closed | Sub-plan 05 — persistence schema v1 → v2 + `DaemonWorkerRegistry` + v1→v2 auto-migration |
| G5 — WasmJs Web Push + BroadcastChannel are no-op stubs | ✓ Closed | Sub-plan 06 — `@JsFun` external bindings replace the stubs |
| G6 — `docs/Home.md` overclaims ✓ vs `true-background-matrix.md` "SCAFFOLD" disclaimer | ✓ Closed | Sub-plan 07 — refreshed all 3 docs; SCAFFOLD disclaimer dropped; harness consistency check added |
| G7 — `@ExperimentalForegroundApi` not yet graduated | ✓ Closed | Sub-plan 08 — `@Deprecated("Foreground API is stable.")`; CHANGELOG entry; koverVerify PASS |

---

## How to re-audit

Local:

```bash
bash scripts/run-parity-audit.sh             # refresh the matrix (rewrites this doc)
bash scripts/run-parity-audit.sh --verify    # CI-friendly: assert idempotent + cross-doc consistent
```

Automated:

```bash
gh workflow run parity-audit.yml             # manual dispatch on GitHub
```

The CI workflow runs nightly at 06:00 UTC. If the matrix changes (gap regression, new feature added without doc update), the workflow opens a PR.

---

## Related

- `GOAL.md` of `cross-platform-worker-parity-audit` — the design contract this report substantiates.
- `PLAN.md` + 8 sub-plans — the executable epic that closed G1-G7.
- [`worker-kmp-platform-engine-tests`](../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-platform-engine-tests/GOAL.md) — sibling epic adding the per-platform test harnesses.
- [`kover-100-coverage`](../../plan-layer/project-plans/mbs/worker-kmp/active/kover-100-coverage/GOAL.md) (DONE, 2026-06-01) — backs the 100% coverage claim.
- README.md — links here from the badge row + "Why worker-kmp" section.
- `docs/Home.md` — links here from the "Operations" section.
