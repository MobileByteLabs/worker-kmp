---
module: cmp-worker-ios
artifact: io.github.mobilebytelabs:cmp-worker-ios
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.ios
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-ios — Development

> Single source of truth for development state of `cmp-worker-ios` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-ios` | `com.mobilebytelabs.kmptoolkit.worker.ios` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-ios) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-ios/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| iosMain | ✅ | ✅ real | 0 | 10 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-ios.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-ios>
final class io.github.mobilebytelabs.worker.ios/IosWorkManager : io.github.mobilebytelabs.worker/WorkManager { // io.github.mobilebytelabs.worker.ios/IosWorkManager|null[0]
    constructor <init>(io.github.mobilebytelabs.worker.ios/IosWorkerFactory, io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig = ...) // io.github.mobilebytelabs.worker.ios/IosWorkManager.<init>|<init>(io.github.mobilebytelabs.worker.ios.IosWorkerFactory;io.github.mobilebytelabs.worker.ios.IosWorkManagerConfig){}[0]

    final fun getWorkInfosByTag(kotlin/String): kotlinx.coroutines.flow/Flow<kotlin.collections/List<io.github.mobilebytelabs.worker/WorkInfo>> // io.github.mobilebytelabs.worker.ios/IosWorkManager.getWorkInfosByTag|getWorkInfosByTag(kotlin.String){}[0]
    final fun shutdown() // io.github.mobilebytelabs.worker.ios/IosWorkManager.shutdown|shutdown(){}[0]
    final suspend fun cancelAllWorkByTag(kotlin/String) // io.github.mobilebytelabs.worker.ios/IosWorkManager.cancelAllWorkByTag|cancelAllWorkByTag(kotlin.String){}[0]
    final suspend fun cancelWorkById(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.ios/IosWorkManager.cancelWorkById|cancelWorkById(kotlin.uuid.Uuid){}[0]
    final suspend fun enqueue(io.github.mobilebytelabs.worker/OneTimeWorkRequest): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.ios/IosWorkManager.enqueue|enqueue(io.github.mobilebytelabs.worker.OneTimeWorkRequest){}[0]
    final suspend fun enqueueUniquePeriodicWork(kotlin/String, io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy, io.github.mobilebytelabs.worker/PeriodicWorkRequest): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.ios/IosWorkManager.enqueueUniquePeriodicWork|enqueueUniquePeriodicWork(kotlin.String;io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy;io.github.mobilebytelabs.worker.PeriodicWorkRequest){}[0]
    final suspend fun getWorkInfoById(kotlin.uuid/Uuid): io.github.mobilebytelabs.worker/WorkInfo? // io.github.mobilebytelabs.worker.ios/IosWorkManager.getWorkInfoById|getWorkInfoById(kotlin.uuid.Uuid){}[0]
}

final class io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig { // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig|null[0]
    constructor <init>(kotlin/Boolean = ..., kotlin/String = ..., kotlin/Boolean = ..., kotlin/String = ..., kotlin/String = ...) // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.<init>|<init>(kotlin.Boolean;kotlin.String;kotlin.Boolean;kotlin.String;kotlin.String){}[0]

    final val appRefreshTaskIdentifier // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.appRefreshTaskIdentifier|{}appRefreshTaskIdentifier[0]
        final fun <get-appRefreshTaskIdentifier>(): kotlin/String // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.appRefreshTaskIdentifier.<get-appRefreshTaskIdentifier>|<get-appRefreshTaskIdentifier>(){}[0]
    final val bgProcessingTaskIdentifier // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.bgProcessingTaskIdentifier|{}bgProcessingTaskIdentifier[0]
        final fun <get-bgProcessingTaskIdentifier>(): kotlin/String // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.bgProcessingTaskIdentifier.<get-bgProcessingTaskIdentifier>|<get-bgProcessingTaskIdentifier>(){}[0]
    final val enableBackgroundTasks // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.enableBackgroundTasks|{}enableBackgroundTasks[0]
        final fun <get-enableBackgroundTasks>(): kotlin/Boolean // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.enableBackgroundTasks.<get-enableBackgroundTasks>|<get-enableBackgroundTasks>(){}[0]
    final val enablePersistence // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.enablePersistence|{}enablePersistence[0]
        final fun <get-enablePersistence>(): kotlin/Boolean // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.enablePersistence.<get-enablePersistence>|<get-enablePersistence>(){}[0]
    final val persistenceKey // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.persistenceKey|{}persistenceKey[0]
        final fun <get-persistenceKey>(): kotlin/String // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.persistenceKey.<get-persistenceKey>|<get-persistenceKey>(){}[0]

    final fun component1(): kotlin/Boolean // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.component1|component1(){}[0]
    final fun component2(): kotlin/String // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.component2|component2(){}[0]
    final fun component3(): kotlin/Boolean // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.component3|component3(){}[0]
    final fun component4(): kotlin/String // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.component4|component4(){}[0]
    final fun component5(): kotlin/String // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.component5|component5(){}[0]
    final fun copy(kotlin/Boolean = ..., kotlin/String = ..., kotlin/Boolean = ..., kotlin/String = ..., kotlin/String = ...): io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.copy|copy(kotlin.Boolean;kotlin.String;kotlin.Boolean;kotlin.String;kotlin.String){}[0]
    final fun equals(kotlin/Any?): kotlin/Boolean // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.equals|equals(kotlin.Any?){}[0]
    final fun hashCode(): kotlin/Int // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.hashCode|hashCode(){}[0]
    final fun toString(): kotlin/String // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.toString|toString(){}[0]

    final object Companion { // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.Companion|null[0]
        final val DEFAULT // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.Companion.DEFAULT|{}DEFAULT[0]
            final fun <get-DEFAULT>(): io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig // io.github.mobilebytelabs.worker.ios/IosWorkManagerConfig.Companion.DEFAULT.<get-DEFAULT>|<get-DEFAULT>(){}[0]
    }
}
```



---

## §4 Spec Snapshot (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30 -->

**Problem this module solves:** _TBD by author._

**Core invariants:**
- _TBD by author._

**Out of scope (by design):**
- _TBD by author._

---

## §5 Extension Recipes (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30 -->

### Recipe: Add a new platform actual

1. _TBD by author._
2. _TBD by author._
3. _TBD by author._

### Recipe: Extend the public API

1. _TBD by author._
2. _TBD by author._

### Recipe: Add a new variant under an existing platform (e.g. tvosArm64)

1. _TBD by author._
2. _TBD by author._

---

## §6 Active Development Log (auto-gen)

| Date | Author | PR | Summary | State |
|------|--------|----|---------|-------|
| (no open PRs labeled `cmp-worker-ios` — refresh via `gh pr list --label cmp-worker-ios` then re-run scan) | — | — | — | — |

---

## §7 Cross-Platform Parity Recipes (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30 -->

### Pattern: _Pattern name TBD_

**When to use:** _TBD_
**Code shape:**
```kotlin
// TBD
```

---

## §8 Related

| Type | Reference |
|------|-----------|
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) + [RULE-LIB-OBSERVABILITY-SURFACE-001](../../../../../../layers/framework/rules/RULE-LIB-OBSERVABILITY-SURFACE-001.md) |
| External docs | [README](README.md) |

---

## §9 Observability Surface (authored — LLM-seeded)

<!-- AUTHOR: WIP — initial draft from 2026-05-30. Per RULE-LIB-OBSERVABILITY-SURFACE-001 (LD-9a..LD-9d). -->

| Signal Tier | Status | Details |
|-------------|--------|---------|
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-ios@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
| T1 (config + version health)  | enabled | events: `lib_init_success`, `lib_init_failure` (FirebaseAnalyticsHealthHook) |
| T2 (lifecycle events)         | opted-out | (author when ready — populate event_schema YAML below + flip to enabled) |
| T3 (performance traces)       | opted-out | (opt-in per consumer; FirebasePerformanceHook wraps `*_start` / `*_end` lifecycle events) |
| T4 (full API usage)           | opted-out | opt-in per consumer + per end-user; iOS ATT prompt required |

```yaml
# DEVELOPMENT_OBSERVABILITY.schema.yaml-conformant block
tiers:
  T0: enabled
  T1: enabled
  T2: opted-out
custom_key_format: "library:cmp-worker-ios@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-ios.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-ios.observability_opt_in=true` in consumer's `lib-integrate.properties`.
