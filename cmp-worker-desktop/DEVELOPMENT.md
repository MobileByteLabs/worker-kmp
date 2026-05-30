---
module: cmp-worker-desktop
artifact: io.github.mobilebytelabs:cmp-worker-desktop
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.desktop
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-desktop — Development

> Single source of truth for development state of `cmp-worker-desktop` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-desktop` | `com.mobilebytelabs.kmptoolkit.worker.desktop` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-desktop) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-desktop/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| jvmMain | ✅ | ✅ real | 0 | 8 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-desktop.api
```
public final class io/github/mobilebytelabs/worker/desktop/DesktopWorkManager : io/github/mobilebytelabs/worker/WorkManager {
    public static final field $stable I
    public fun <init> (Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig;Lio/github/mobilebytelabs/worker/desktop/DesktopWorkerFactory;)V
    public synthetic fun <init> (Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig;Lio/github/mobilebytelabs/worker/desktop/DesktopWorkerFactory;ILkotlin/jvm/internal/DefaultConstructorMarker;)V
    public fun cancelAllWorkByTag (Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun cancelWorkById (Lkotlin/uuid/Uuid;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun enqueue (Lio/github/mobilebytelabs/worker/OneTimeWorkRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun enqueueUniquePeriodicWork (Ljava/lang/String;Lio/github/mobilebytelabs/worker/ExistingPeriodicWorkPolicy;Lio/github/mobilebytelabs/worker/PeriodicWorkRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun getWorkInfoById (Lkotlin/uuid/Uuid;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public fun getWorkInfosByTag (Ljava/lang/String;)Lkotlinx/coroutines/flow/Flow;
    public final fun shutdown ()V
}

public final class io/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig {
    public static final field $stable I
    public static final field Companion Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig$Companion;
    public fun <init> ()V
    public fun <init> (IZLjava/io/File;J)V
    public synthetic fun <init> (IZLjava/io/File;JILkotlin/jvm/internal/DefaultConstructorMarker;)V
    public final fun component1 ()I
    public final fun component2 ()Z
    public final fun component3 ()Ljava/io/File;
    public final fun component4 ()J
    public final fun copy (IZLjava/io/File;J)Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig;IZLjava/io/File;JILjava/lang/Object;)Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig;
    public fun equals (Ljava/lang/Object;)Z
    public final fun getConstraintCheckIntervalMs ()J
    public final fun getMaxConcurrentWorkers ()I
    public final fun getPersistenceEnabled ()Z
    public final fun getPersistencePath ()Ljava/io/File;
    public fun hashCode ()I
    public fun toString ()Ljava/lang/String;
}

public final class io/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig$Companion {
    public final fun getDEFAULT ()Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig;
    public final fun getIN_MEMORY ()Lio/github/mobilebytelabs/worker/desktop/DesktopWorkManagerConfig;
}

public final class io/github/mobilebytelabs/worker/desktop/DesktopWorkManagerInitKt {
    public static final fun desktopWorkManagerFactory ()Lio/github/mobilebytelabs/worker/WorkManagerFactory;
}

public abstract interface class io/github/mobilebytelabs/worker/desktop/DesktopWorkerFactory {
    public abstract fun create (Ljava/lang/String;Lio/github/mobilebytelabs/worker/WorkerContext;)Lio/github/mobilebytelabs/worker/CoroutineWorker;
}

public final class io/github/mobilebytelabs/worker/desktop/LaunchDesktopWorkerAppKt {
    public static final fun launchDesktopWorkerApp (Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function2;)V
    public static final fun startWorkerKoinIfAbsent (Lkotlin/jvm/functions/Function0;)V
```

### cmp-worker-desktop.klib.api
```

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
| (no open PRs labeled `cmp-worker-desktop` — refresh via `gh pr list --label cmp-worker-desktop` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-desktop@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-desktop@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-desktop.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-desktop.observability_opt_in=true` in consumer's `lib-integrate.properties`.
