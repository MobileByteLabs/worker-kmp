---
module: cmp-worker-web
artifact: io.github.mobilebytelabs:cmp-worker-web
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.web
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-web — Development

> Single source of truth for development state of `cmp-worker-web` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-web` | `com.mobilebytelabs.kmptoolkit.worker.web` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-web) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-web/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| jvmMain | ✅ | ✅ real | 0 | 7 | 2026-05-30 | — |
| jsMain | ✅ | ✅ real | 0 | 8 | 2026-05-30 | — |
| wasmJsMain | ✅ | ✅ real | 0 | 8 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-web.api
```
public final class io/github/mobilebytelabs/worker/web/BackgroundSyncScriptKt {
    public static final fun backgroundSyncServiceWorkerScript ()Ljava/lang/String;
}

public final class io/github/mobilebytelabs/worker/web/BroadcastChannelBridge_jvmKt {
    public static final fun openWorkerKmpBroadcastChannel (Lkotlin/jvm/functions/Function2;)Lio/github/mobilebytelabs/worker/web/WorkerKmpBroadcastSubscription;
}

public final class io/github/mobilebytelabs/worker/web/NotificationPermission : java/lang/Enum {
    public static final field DEFAULT Lio/github/mobilebytelabs/worker/web/NotificationPermission;
    public static final field DENIED Lio/github/mobilebytelabs/worker/web/NotificationPermission;
    public static final field GRANTED Lio/github/mobilebytelabs/worker/web/NotificationPermission;
    public static fun getEntries ()Lkotlin/enums/EnumEntries;
    public static fun valueOf (Ljava/lang/String;)Lio/github/mobilebytelabs/worker/web/NotificationPermission;
    public static fun values ()[Lio/github/mobilebytelabs/worker/web/NotificationPermission;
}

public final class io/github/mobilebytelabs/worker/web/WebNotificationsKt {
    public static final fun requestNotificationPermission (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public static final fun showWorkerNotification (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)V
    public static synthetic fun showWorkerNotification$default (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ILjava/lang/Object;)V
}

public final class io/github/mobilebytelabs/worker/web/WebWorkManager : io/github/mobilebytelabs/worker/WorkManager {
    public static final field $stable I
    public fun <init> (Lio/github/mobilebytelabs/worker/web/WebWorkerFactory;Lio/github/mobilebytelabs/worker/web/WebWorkManagerConfig;)V
    public synthetic fun <init> (Lio/github/mobilebytelabs/worker/web/WebWorkerFactory;Lio/github/mobilebytelabs/worker/web/WebWorkManagerConfig;ILkotlin/jvm/internal/DefaultConstructorMarker;)V
    public fun cancelAllWorkByTag (Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun cancelWorkById (Lkotlin/uuid/Uuid;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun enqueue (Lio/github/mobilebytelabs/worker/OneTimeWorkRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun enqueueUniquePeriodicWork (Ljava/lang/String;Lio/github/mobilebytelabs/worker/ExistingPeriodicWorkPolicy;Lio/github/mobilebytelabs/worker/PeriodicWorkRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun getWorkInfoById (Lkotlin/uuid/Uuid;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun getWorkInfosByTag (Ljava/lang/String;)Lkotlinx/coroutines/flow/Flow;
    public final fun shutdown ()V
}

public final class io/github/mobilebytelabs/worker/web/WebWorkManagerConfig {
    public static final field $stable I
    public static final field Companion Lio/github/mobilebytelabs/worker/web/WebWorkManagerConfig$Companion;
    public fun <init> ()V
    public fun <init> (JZLjava/lang/String;ZLjava/lang/String;Z)V
    public synthetic fun <init> (JZLjava/lang/String;ZLjava/lang/String;ZILkotlin/jvm/internal/DefaultConstructorMarker;)V
    public final fun component1 ()J
    public final fun component2 ()Z
    public final fun component3 ()Ljava/lang/String;
    public final fun component4 ()Z
    public final fun component5 ()Ljava/lang/String;
    public final fun component6 ()Z
    public final fun copy (JZLjava/lang/String;ZLjava/lang/String;Z)Lio/github/mobilebytelabs/worker/web/WebWorkManagerConfig;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/web/WebWorkManagerConfig;JZLjava/lang/String;ZLjava/lang/String;ZILjava/lang/Object;)Lio/github/mobilebytelabs/worker/web/WebWorkManagerConfig;
```

### cmp-worker-web.klib.api
```
// Klib ABI Dump
// Targets: [js, wasmJs]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-web>
final enum class io.github.mobilebytelabs.worker.web/NotificationPermission : kotlin/Enum<io.github.mobilebytelabs.worker.web/NotificationPermission> { // io.github.mobilebytelabs.worker.web/NotificationPermission|null[0]
    enum entry DEFAULT // io.github.mobilebytelabs.worker.web/NotificationPermission.DEFAULT|null[0]
    enum entry DENIED // io.github.mobilebytelabs.worker.web/NotificationPermission.DENIED|null[0]
    enum entry GRANTED // io.github.mobilebytelabs.worker.web/NotificationPermission.GRANTED|null[0]

    final val entries // io.github.mobilebytelabs.worker.web/NotificationPermission.entries|#static{}entries[0]
        final fun <get-entries>(): kotlin.enums/EnumEntries<io.github.mobilebytelabs.worker.web/NotificationPermission> // io.github.mobilebytelabs.worker.web/NotificationPermission.entries.<get-entries>|<get-entries>#static(){}[0]

    final fun valueOf(kotlin/String): io.github.mobilebytelabs.worker.web/NotificationPermission // io.github.mobilebytelabs.worker.web/NotificationPermission.valueOf|valueOf#static(kotlin.String){}[0]
    final fun values(): kotlin/Array<io.github.mobilebytelabs.worker.web/NotificationPermission> // io.github.mobilebytelabs.worker.web/NotificationPermission.values|values#static(){}[0]
}

abstract interface io.github.mobilebytelabs.worker.web/WorkerKmpBroadcastSubscription { // io.github.mobilebytelabs.worker.web/WorkerKmpBroadcastSubscription|null[0]
    abstract fun close() // io.github.mobilebytelabs.worker.web/WorkerKmpBroadcastSubscription.close|close(){}[0]
}

final class io.github.mobilebytelabs.worker.web/WebWorkManager : io.github.mobilebytelabs.worker/WorkManager { // io.github.mobilebytelabs.worker.web/WebWorkManager|null[0]
    constructor <init>(io.github.mobilebytelabs.worker.web/WebWorkerFactory, io.github.mobilebytelabs.worker.web/WebWorkManagerConfig = ...) // io.github.mobilebytelabs.worker.web/WebWorkManager.<init>|<init>(io.github.mobilebytelabs.worker.web.WebWorkerFactory;io.github.mobilebytelabs.worker.web.WebWorkManagerConfig){}[0]

    final fun getWorkInfosByTag(kotlin/String): kotlinx.coroutines.flow/Flow<kotlin.collections/List<io.github.mobilebytelabs.worker/WorkInfo>> // io.github.mobilebytelabs.worker.web/WebWorkManager.getWorkInfosByTag|getWorkInfosByTag(kotlin.String){}[0]
    final fun shutdown() // io.github.mobilebytelabs.worker.web/WebWorkManager.shutdown|shutdown(){}[0]
    final suspend fun cancelAllWorkByTag(kotlin/String) // io.github.mobilebytelabs.worker.web/WebWorkManager.cancelAllWorkByTag|cancelAllWorkByTag(kotlin.String){}[0]
    final suspend fun cancelWorkById(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.web/WebWorkManager.cancelWorkById|cancelWorkById(kotlin.uuid.Uuid){}[0]
    final suspend fun enqueue(io.github.mobilebytelabs.worker/OneTimeWorkRequest): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.web/WebWorkManager.enqueue|enqueue(io.github.mobilebytelabs.worker.OneTimeWorkRequest){}[0]
    final suspend fun enqueueUniquePeriodicWork(kotlin/String, io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy, io.github.mobilebytelabs.worker/PeriodicWorkRequest): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.web/WebWorkManager.enqueueUniquePeriodicWork|enqueueUniquePeriodicWork(kotlin.String;io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy;io.github.mobilebytelabs.worker.PeriodicWorkRequest){}[0]
    final suspend fun getWorkInfoById(kotlin.uuid/Uuid): io.github.mobilebytelabs.worker/WorkInfo? // io.github.mobilebytelabs.worker.web/WebWorkManager.getWorkInfoById|getWorkInfoById(kotlin.uuid.Uuid){}[0]
}

final class io.github.mobilebytelabs.worker.web/WebWorkManagerConfig { // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig|null[0]
    constructor <init>(kotlin/Long = ..., kotlin/Boolean = ..., kotlin/String = ..., kotlin/Boolean = ..., kotlin/String = ..., kotlin/Boolean = ...) // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.<init>|<init>(kotlin.Long;kotlin.Boolean;kotlin.String;kotlin.Boolean;kotlin.String;kotlin.Boolean){}[0]

    final val constraintCheckIntervalMs // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.constraintCheckIntervalMs|{}constraintCheckIntervalMs[0]
        final fun <get-constraintCheckIntervalMs>(): kotlin/Long // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.constraintCheckIntervalMs.<get-constraintCheckIntervalMs>|<get-constraintCheckIntervalMs>(){}[0]
    final val enableBackgroundSync // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.enableBackgroundSync|{}enableBackgroundSync[0]
        final fun <get-enableBackgroundSync>(): kotlin/Boolean // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.enableBackgroundSync.<get-enableBackgroundSync>|<get-enableBackgroundSync>(){}[0]
    final val enablePeriodicBackgroundSync // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.enablePeriodicBackgroundSync|{}enablePeriodicBackgroundSync[0]
        final fun <get-enablePeriodicBackgroundSync>(): kotlin/Boolean // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.enablePeriodicBackgroundSync.<get-enablePeriodicBackgroundSync>|<get-enablePeriodicBackgroundSync>(){}[0]
    final val enablePersistence // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.enablePersistence|{}enablePersistence[0]
        final fun <get-enablePersistence>(): kotlin/Boolean // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.enablePersistence.<get-enablePersistence>|<get-enablePersistence>(){}[0]
    final val persistenceDbName // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.persistenceDbName|{}persistenceDbName[0]
        final fun <get-persistenceDbName>(): kotlin/String // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.persistenceDbName.<get-persistenceDbName>|<get-persistenceDbName>(){}[0]
    final val serviceWorkerScript // io.github.mobilebytelabs.worker.web/WebWorkManagerConfig.serviceWorkerScript|{}serviceWorkerScript[0]
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
| (no open PRs labeled `cmp-worker-web` — refresh via `gh pr list --label cmp-worker-web` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-web@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-web@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-web.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-web.observability_opt_in=true` in consumer's `lib-integrate.properties`.
