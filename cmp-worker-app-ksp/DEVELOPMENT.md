---
module: cmp-worker-app-ksp
artifact: io.github.mobilebytelabs:cmp-worker-app-ksp
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.app.ksp
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-app-ksp — Development

> Single source of truth for development state of `cmp-worker-app-ksp` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-app-ksp` | `com.mobilebytelabs.kmptoolkit.worker.app.ksp` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-app-ksp) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-app-ksp/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-app-ksp.api
```
public final class io/github/mobilebytelabs/worker/app/ksp/CodegenModel {
    public static final field Companion Lio/github/mobilebytelabs/worker/app/ksp/CodegenModel$Companion;
    public fun <init> (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    public final fun component1 ()Ljava/lang/String;
    public final fun component2 ()Ljava/lang/String;
    public final fun component3 ()Ljava/lang/String;
    public final fun component4 ()Ljava/lang/String;
    public final fun component5 ()Ljava/util/List;
    public final fun component6 ()Ljava/lang/String;
    public final fun component7 ()Ljava/lang/String;
    public final fun component8 ()Ljava/lang/String;
    public final fun copy (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lio/github/mobilebytelabs/worker/app/ksp/CodegenModel;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/app/ksp/CodegenModel;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;)Lio/github/mobilebytelabs/worker/app/ksp/CodegenModel;
    public fun equals (Ljava/lang/Object;)Z
    public final fun getAndroidApplicationId ()Ljava/lang/String;
    public final fun getAndroidPermissions ()Ljava/util/List;
    public final fun getContentFnFqn ()Ljava/lang/String;
    public final fun getIosBundleId ()Ljava/lang/String;
    public final fun getKoinModulesFnFqn ()Ljava/lang/String;
    public final fun getPackageName ()Ljava/lang/String;
    public final fun getTitle ()Ljava/lang/String;
    public final fun getWebCanvasId ()Ljava/lang/String;
    public fun hashCode ()I
    public fun toString ()Ljava/lang/String;
}

public final synthetic class io/github/mobilebytelabs/worker/app/ksp/CodegenModel$$serializer : kotlinx/serialization/internal/GeneratedSerializer {
    public static final field INSTANCE Lio/github/mobilebytelabs/worker/app/ksp/CodegenModel$$serializer;
    public final fun childSerializers ()[Lkotlinx/serialization/KSerializer;
    public final fun deserialize (Lkotlinx/serialization/encoding/Decoder;)Lio/github/mobilebytelabs/worker/app/ksp/CodegenModel;
    public synthetic fun deserialize (Lkotlinx/serialization/encoding/Decoder;)Ljava/lang/Object;
    public final fun getDescriptor ()Lkotlinx/serialization/descriptors/SerialDescriptor;
    public final fun serialize (Lkotlinx/serialization/encoding/Encoder;Lio/github/mobilebytelabs/worker/app/ksp/CodegenModel;)V
    public synthetic fun serialize (Lkotlinx/serialization/encoding/Encoder;Ljava/lang/Object;)V
}

public final class io/github/mobilebytelabs/worker/app/ksp/CodegenModel$Companion {
    public final fun serializer ()Lkotlinx/serialization/KSerializer;
}

public final class io/github/mobilebytelabs/worker/app/ksp/WorkerKmpAppProcessor : com/google/devtools/ksp/processing/SymbolProcessor {
    public static final field MODEL_FILE_EXT Ljava/lang/String;
    public static final field MODEL_FILE_NAME Ljava/lang/String;
    public static final field MODEL_PACKAGE Ljava/lang/String;
    public static final field WORKER_KMP_APP_CONTENT_FQN Ljava/lang/String;
    public static final field WORKER_KMP_APP_FQN Ljava/lang/String;
    public fun <init> (Lcom/google/devtools/ksp/processing/CodeGenerator;Lcom/google/devtools/ksp/processing/KSPLogger;)V
    public fun process (Lcom/google/devtools/ksp/processing/Resolver;)Ljava/util/List;
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
| (no open PRs labeled `cmp-worker-app-ksp` — refresh via `gh pr list --label cmp-worker-app-ksp` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-app-ksp@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-app-ksp@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-app-ksp.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-app-ksp.observability_opt_in=true` in consumer's `lib-integrate.properties`.
