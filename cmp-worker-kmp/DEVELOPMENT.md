---
module: cmp-worker-kmp
artifact: io.github.mobilebytelabs:cmp-worker-kmp
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.kmp
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-kmp — Development

> Single source of truth for development state of `cmp-worker-kmp` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-kmp` | `com.mobilebytelabs.kmptoolkit.worker.kmp` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-kmp) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-kmp/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| androidMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| iosMain | ✅ | ✅ real | 0 | 3 | 2026-05-30 | — |
| jvmMain | ✅ | ✅ real | 0 | 3 | 2026-05-30 | — |
| jsMain | ✅ | ✅ real | 0 | 3 | 2026-05-30 | — |
| wasmJsMain | ✅ | ✅ real | 0 | 3 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-kmp.api
```
public final class io/github/mobilebytelabs/worker/BackgroundCapabilities {
	public fun <init> (ZZ)V
	public final fun component1 ()Z
	public final fun component2 ()Z
	public final fun copy (ZZ)Lio/github/mobilebytelabs/worker/BackgroundCapabilities;
	public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/BackgroundCapabilities;ZZILjava/lang/Object;)Lio/github/mobilebytelabs/worker/BackgroundCapabilities;
	public fun equals (Ljava/lang/Object;)Z
	public final fun getSupportsOsScheduling ()Z
	public final fun getSupportsPersistence ()Z
	public fun hashCode ()I
	public fun toString ()Ljava/lang/String;
}

public final class io/github/mobilebytelabs/worker/BackgroundCapabilities_jvmKt {
	public static final fun platformBackgroundCapabilities ()Lio/github/mobilebytelabs/worker/BackgroundCapabilities;
}

public final class io/github/mobilebytelabs/worker/BackoffPolicy : java/lang/Enum {
	public static final field EXPONENTIAL Lio/github/mobilebytelabs/worker/BackoffPolicy;
	public static final field LINEAR Lio/github/mobilebytelabs/worker/BackoffPolicy;
	public static fun getEntries ()Lkotlin/enums/EnumEntries;
	public static fun valueOf (Ljava/lang/String;)Lio/github/mobilebytelabs/worker/BackoffPolicy;
	public static fun values ()[Lio/github/mobilebytelabs/worker/BackoffPolicy;
}

public abstract class io/github/mobilebytelabs/worker/ConditionalWorker : io/github/mobilebytelabs/worker/CoroutineWorker {
	public fun <init> (Lio/github/mobilebytelabs/worker/WorkerContext;)V
	public abstract fun condition (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
	public abstract fun doConditionalWork (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
	public final fun doWork (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
}

public final class io/github/mobilebytelabs/worker/Constraints {
	public static final field Companion Lio/github/mobilebytelabs/worker/Constraints$Companion;
	public synthetic fun <init> (Lio/github/mobilebytelabs/worker/NetworkType;ZZZZLjava/util/List;Lkotlin/jvm/internal/DefaultConstructorMarker;)V
	public fun equals (Ljava/lang/Object;)Z
	public final fun getContentUriTriggers ()Ljava/util/List;
	public final fun getRequiredNetworkType ()Lio/github/mobilebytelabs/worker/NetworkType;
	public final fun getRequiresBatteryNotLow ()Z
	public final fun getRequiresCharging ()Z
	public final fun getRequiresDeviceIdle ()Z
	public final fun getRequiresStorageNotLow ()Z
	public fun hashCode ()I
}

public final class io/github/mobilebytelabs/worker/Constraints$Builder {
	public fun <init> ()V
	public final fun addContentUriTrigger (Ljava/lang/String;Z)Lio/github/mobilebytelabs/worker/Constraints$Builder;
	public final fun build ()Lio/github/mobilebytelabs/worker/Constraints;
	public final fun setRequiredNetworkType (Lio/github/mobilebytelabs/worker/NetworkType;)Lio/github/mobilebytelabs/worker/Constraints$Builder;
```

### cmp-worker-kmp.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64, js, wasmJs]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-kmp>
open annotation class io.github.mobilebytelabs.worker/ExperimentalForegroundApi : kotlin/Annotation { // io.github.mobilebytelabs.worker/ExperimentalForegroundApi|null[0]
    constructor <init>() // io.github.mobilebytelabs.worker/ExperimentalForegroundApi.<init>|<init>(){}[0]
}

open annotation class io.github.mobilebytelabs.worker/ExperimentalWorkerApi : kotlin/Annotation { // io.github.mobilebytelabs.worker/ExperimentalWorkerApi|null[0]
    constructor <init>() // io.github.mobilebytelabs.worker/ExperimentalWorkerApi.<init>|<init>(){}[0]
}

final enum class io.github.mobilebytelabs.worker.config/LogLevel : kotlin/Enum<io.github.mobilebytelabs.worker.config/LogLevel> { // io.github.mobilebytelabs.worker.config/LogLevel|null[0]
    enum entry DEBUG // io.github.mobilebytelabs.worker.config/LogLevel.DEBUG|null[0]
    enum entry ERROR // io.github.mobilebytelabs.worker.config/LogLevel.ERROR|null[0]
    enum entry INFO // io.github.mobilebytelabs.worker.config/LogLevel.INFO|null[0]
    enum entry SILENT // io.github.mobilebytelabs.worker.config/LogLevel.SILENT|null[0]
    enum entry VERBOSE // io.github.mobilebytelabs.worker.config/LogLevel.VERBOSE|null[0]
    enum entry WARN // io.github.mobilebytelabs.worker.config/LogLevel.WARN|null[0]

    final val entries // io.github.mobilebytelabs.worker.config/LogLevel.entries|#static{}entries[0]
        final fun <get-entries>(): kotlin.enums/EnumEntries<io.github.mobilebytelabs.worker.config/LogLevel> // io.github.mobilebytelabs.worker.config/LogLevel.entries.<get-entries>|<get-entries>#static(){}[0]

    final fun valueOf(kotlin/String): io.github.mobilebytelabs.worker.config/LogLevel // io.github.mobilebytelabs.worker.config/LogLevel.valueOf|valueOf#static(kotlin.String){}[0]
    final fun values(): kotlin/Array<io.github.mobilebytelabs.worker.config/LogLevel> // io.github.mobilebytelabs.worker.config/LogLevel.values|values#static(){}[0]
}

final enum class io.github.mobilebytelabs.worker/BackoffPolicy : kotlin/Enum<io.github.mobilebytelabs.worker/BackoffPolicy> { // io.github.mobilebytelabs.worker/BackoffPolicy|null[0]
    enum entry EXPONENTIAL // io.github.mobilebytelabs.worker/BackoffPolicy.EXPONENTIAL|null[0]
    enum entry LINEAR // io.github.mobilebytelabs.worker/BackoffPolicy.LINEAR|null[0]

    final val entries // io.github.mobilebytelabs.worker/BackoffPolicy.entries|#static{}entries[0]
        final fun <get-entries>(): kotlin.enums/EnumEntries<io.github.mobilebytelabs.worker/BackoffPolicy> // io.github.mobilebytelabs.worker/BackoffPolicy.entries.<get-entries>|<get-entries>#static(){}[0]

    final fun valueOf(kotlin/String): io.github.mobilebytelabs.worker/BackoffPolicy // io.github.mobilebytelabs.worker/BackoffPolicy.valueOf|valueOf#static(kotlin.String){}[0]
    final fun values(): kotlin/Array<io.github.mobilebytelabs.worker/BackoffPolicy> // io.github.mobilebytelabs.worker/BackoffPolicy.values|values#static(){}[0]
}

final enum class io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy : kotlin/Enum<io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy> { // io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy|null[0]
    enum entry KEEP // io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy.KEEP|null[0]
    enum entry REPLACE // io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy.REPLACE|null[0]
    enum entry UPDATE // io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy.UPDATE|null[0]

    final val entries // io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy.entries|#static{}entries[0]
        final fun <get-entries>(): kotlin.enums/EnumEntries<io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy> // io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy.entries.<get-entries>|<get-entries>#static(){}[0]
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
| (no open PRs labeled `cmp-worker-kmp` — refresh via `gh pr list --label cmp-worker-kmp` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-kmp@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-kmp@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-kmp.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-kmp.observability_opt_in=true` in consumer's `lib-integrate.properties`.
