---
module: cmp-worker-compose
artifact: io.github.mobilebytelabs:cmp-worker-compose
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.compose
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-compose — Development

> Single source of truth for development state of `cmp-worker-compose` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-compose` | `com.mobilebytelabs.kmptoolkit.worker.compose` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-compose) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-compose/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| androidMain | ✅ | ✅ real | 0 | 0 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-compose.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64, js, wasmJs]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-compose>
final enum class io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi : kotlin/Enum<io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi> { // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi|null[0]
    enum entry DRAFTING // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.DRAFTING|null[0]
    enum entry FAILED // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.FAILED|null[0]
    enum entry IDLE // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.IDLE|null[0]
    enum entry SUBMITTED // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.SUBMITTED|null[0]
    enum entry SUBMITTING // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.SUBMITTING|null[0]

    final val entries // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.entries|#static{}entries[0]
        final fun <get-entries>(): kotlin.enums/EnumEntries<io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi> // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.entries.<get-entries>|<get-entries>#static(){}[0]

    final fun valueOf(kotlin/String): io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.valueOf|valueOf#static(kotlin.String){}[0]
    final fun values(): kotlin/Array<io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi> // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi.values|values#static(){}[0]
}

final class io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel { // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel|null[0]
    constructor <init>(io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi, kotlin/String? = ..., kotlin/Function0<kotlin/Unit>? = ..., kotlin/Function0<kotlin/Unit>? = ...) // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.<init>|<init>(io.github.mobilebytelabs.worker.compose.storeflow.SubmitStateUi;kotlin.String?;kotlin.Function0<kotlin.Unit>?;kotlin.Function0<kotlin.Unit>?){}[0]

    final val message // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.message|{}message[0]
        final fun <get-message>(): kotlin/String? // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.message.<get-message>|<get-message>(){}[0]
    final val onCancel // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.onCancel|{}onCancel[0]
        final fun <get-onCancel>(): kotlin/Function0<kotlin/Unit>? // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.onCancel.<get-onCancel>|<get-onCancel>(){}[0]
    final val onRetry // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.onRetry|{}onRetry[0]
        final fun <get-onRetry>(): kotlin/Function0<kotlin/Unit>? // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.onRetry.<get-onRetry>|<get-onRetry>(){}[0]
    final val state // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.state|{}state[0]
        final fun <get-state>(): io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.state.<get-state>|<get-state>(){}[0]

    final fun component1(): io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.component1|component1(){}[0]
    final fun component2(): kotlin/String? // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.component2|component2(){}[0]
    final fun component3(): kotlin/Function0<kotlin/Unit>? // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.component3|component3(){}[0]
    final fun component4(): kotlin/Function0<kotlin/Unit>? // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.component4|component4(){}[0]
    final fun copy(io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUi = ..., kotlin/String? = ..., kotlin/Function0<kotlin/Unit>? = ..., kotlin/Function0<kotlin/Unit>? = ...): io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.copy|copy(io.github.mobilebytelabs.worker.compose.storeflow.SubmitStateUi;kotlin.String?;kotlin.Function0<kotlin.Unit>?;kotlin.Function0<kotlin.Unit>?){}[0]
    final fun equals(kotlin/Any?): kotlin/Boolean // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.equals|equals(kotlin.Any?){}[0]
    final fun hashCode(): kotlin/Int // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.hashCode|hashCode(){}[0]
    final fun toString(): kotlin/String // io.github.mobilebytelabs.worker.compose.storeflow/SubmitStateUiModel.toString|toString(){}[0]
}

final val io.github.mobilebytelabs.worker.compose.storeflow/io_github_mobilebytelabs_worker_compose_storeflow_SubmitStateUiModel$stableprop // io.github.mobilebytelabs.worker.compose.storeflow/io_github_mobilebytelabs_worker_compose_storeflow_SubmitStateUiModel$stableprop|#static{}io_github_mobilebytelabs_worker_compose_storeflow_SubmitStateUiModel$stableprop[0]
final val io.github.mobilebytelabs.worker.compose/LocalWorkManager // io.github.mobilebytelabs.worker.compose/LocalWorkManager|{}LocalWorkManager[0]
    final fun <get-LocalWorkManager>(): androidx.compose.runtime/ProvidableCompositionLocal<io.github.mobilebytelabs.worker/WorkManager> // io.github.mobilebytelabs.worker.compose/LocalWorkManager.<get-LocalWorkManager>|<get-LocalWorkManager>(){}[0]

final fun (io.github.mobilebytelabs.worker/WorkManager).io.github.mobilebytelabs.worker.compose/collectWorkInfoByIdAsState(kotlin.uuid/Uuid, io.github.mobilebytelabs.worker/WorkInfo?, androidx.compose.runtime/Composer?, kotlin/Int, kotlin/Int): androidx.compose.runtime/State<io.github.mobilebytelabs.worker/WorkInfo?> // io.github.mobilebytelabs.worker.compose/collectWorkInfoByIdAsState|collectWorkInfoByIdAsState@io.github.mobilebytelabs.worker.WorkManager(kotlin.uuid.Uuid;io.github.mobilebytelabs.worker.WorkInfo?;androidx.compose.runtime.Composer?;kotlin.Int;kotlin.Int){}[0]
final fun (io.github.mobilebytelabs.worker/WorkManager).io.github.mobilebytelabs.worker.compose/collectWorkInfosByTagAsState(kotlin/String, kotlin.collections/List<io.github.mobilebytelabs.worker/WorkInfo>?, androidx.compose.runtime/Composer?, kotlin/Int, kotlin/Int): androidx.compose.runtime/State<kotlin.collections/List<io.github.mobilebytelabs.worker/WorkInfo>> // io.github.mobilebytelabs.worker.compose/collectWorkInfosByTagAsState|collectWorkInfosByTagAsState@io.github.mobilebytelabs.worker.WorkManager(kotlin.String;kotlin.collections.List<io.github.mobilebytelabs.worker.WorkInfo>?;androidx.compose.runtime.Composer?;kotlin.Int;kotlin.Int){}[0]
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
| (no open PRs labeled `cmp-worker-compose` — refresh via `gh pr list --label cmp-worker-compose` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-compose@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-compose@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-compose.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-compose.observability_opt_in=true` in consumer's `lib-integrate.properties`.
