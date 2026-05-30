---
module: cmp-worker-app-annotations
artifact: io.github.mobilebytelabs:cmp-worker-app-annotations
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.app.annotations
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-app-annotations — Development

> Single source of truth for development state of `cmp-worker-app-annotations` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-app-annotations` | `com.mobilebytelabs.kmptoolkit.worker.app.annotations` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-app-annotations) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-app-annotations/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-app-annotations.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64, js, wasmJs]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-app-annotations>
open annotation class io.github.mobilebytelabs.worker.app/WorkerKmpApp : kotlin/Annotation { // io.github.mobilebytelabs.worker.app/WorkerKmpApp|null[0]
    constructor <init>(kotlin/String, kotlin/String, kotlin/String = ..., kotlin/String = ..., kotlin/Array<kotlin/String> = ...) // io.github.mobilebytelabs.worker.app/WorkerKmpApp.<init>|<init>(kotlin.String;kotlin.String;kotlin.String;kotlin.String;kotlin.Array<kotlin.String>){}[0]

    final val androidApplicationId // io.github.mobilebytelabs.worker.app/WorkerKmpApp.androidApplicationId|{}androidApplicationId[0]
        final fun <get-androidApplicationId>(): kotlin/String // io.github.mobilebytelabs.worker.app/WorkerKmpApp.androidApplicationId.<get-androidApplicationId>|<get-androidApplicationId>(){}[0]
    final val androidPermissions // io.github.mobilebytelabs.worker.app/WorkerKmpApp.androidPermissions|{}androidPermissions[0]
        final fun <get-androidPermissions>(): kotlin/Array<kotlin/String> // io.github.mobilebytelabs.worker.app/WorkerKmpApp.androidPermissions.<get-androidPermissions>|<get-androidPermissions>(){}[0]
    final val iosBundleId // io.github.mobilebytelabs.worker.app/WorkerKmpApp.iosBundleId|{}iosBundleId[0]
        final fun <get-iosBundleId>(): kotlin/String // io.github.mobilebytelabs.worker.app/WorkerKmpApp.iosBundleId.<get-iosBundleId>|<get-iosBundleId>(){}[0]
    final val title // io.github.mobilebytelabs.worker.app/WorkerKmpApp.title|{}title[0]
        final fun <get-title>(): kotlin/String // io.github.mobilebytelabs.worker.app/WorkerKmpApp.title.<get-title>|<get-title>(){}[0]
    final val webCanvasId // io.github.mobilebytelabs.worker.app/WorkerKmpApp.webCanvasId|{}webCanvasId[0]
        final fun <get-webCanvasId>(): kotlin/String // io.github.mobilebytelabs.worker.app/WorkerKmpApp.webCanvasId.<get-webCanvasId>|<get-webCanvasId>(){}[0]
}

open annotation class io.github.mobilebytelabs.worker.app/WorkerKmpAppContent : kotlin/Annotation { // io.github.mobilebytelabs.worker.app/WorkerKmpAppContent|null[0]
    constructor <init>() // io.github.mobilebytelabs.worker.app/WorkerKmpAppContent.<init>|<init>(){}[0]
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
| (no open PRs labeled `cmp-worker-app-annotations` — refresh via `gh pr list --label cmp-worker-app-annotations` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-app-annotations@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-app-annotations@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-app-annotations.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-app-annotations.observability_opt_in=true` in consumer's `lib-integrate.properties`.
