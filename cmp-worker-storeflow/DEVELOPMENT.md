---
module: cmp-worker-storeflow
artifact: io.github.mobilebytelabs:cmp-worker-storeflow
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.storeflow
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-storeflow — Development

> Single source of truth for development state of `cmp-worker-storeflow` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-storeflow` | `com.mobilebytelabs.kmptoolkit.worker.storeflow` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-storeflow) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-storeflow/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-storeflow.api
```
public final class io/github/mobilebytelabs/worker/storeflow/koin/WorkStoreFlowKoinModuleKt {
    public static final fun getWorkStoreFlowKoinModule ()Lorg/koin/core/module/Module;
}

public abstract class io/github/mobilebytelabs/worker/storeflow/paging/PrefetchPagingWorker : io/github/mobilebytelabs/worker/CoroutineWorker {
    public static final field Companion Lio/github/mobilebytelabs/worker/storeflow/paging/PrefetchPagingWorker$Companion;
    public static final field DEFAULT_PAGE_COUNT I
    public static final field KEY_PAGES_FETCHED Ljava/lang/String;
    public static final field KEY_PAGE_COUNT Ljava/lang/String;
    public static final field KEY_START_PAGE Ljava/lang/String;
    public fun <init> (Lio/github/mobilebytelabs/worker/WorkerContext;)V
    public fun doWork (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    protected abstract fun fetchPage (ILkotlin/coroutines/Continuation;)Ljava/lang/Object;
}

public final class io/github/mobilebytelabs/worker/storeflow/paging/PrefetchPagingWorker$Companion {
}

public final class io/github/mobilebytelabs/worker/storeflow/policy/FetchPolicy : java/lang/Enum {
    public static final field CACHE_ONLY Lio/github/mobilebytelabs/worker/storeflow/policy/FetchPolicy;
    public static final field CACHE_THEN_NETWORK Lio/github/mobilebytelabs/worker/storeflow/policy/FetchPolicy;
    public static final field NETWORK_ONLY Lio/github/mobilebytelabs/worker/storeflow/policy/FetchPolicy;
    public static fun getEntries ()Lkotlin/enums/EnumEntries;
    public static fun valueOf (Ljava/lang/String;)Lio/github/mobilebytelabs/worker/storeflow/policy/FetchPolicy;
    public static fun values ()[Lio/github/mobilebytelabs/worker/storeflow/policy/FetchPolicy;
}

public final class io/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler {
    public fun <init> (Lio/github/mobilebytelabs/worker/storeflow/submit/SubmitOutbox;Lkotlin/jvm/functions/Function2;)V
    public final fun draft (Ljava/lang/Object;)V
    public final fun getState ()Lio/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler$State;
    public final fun rehydrateFromOutbox (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public final fun reset ()V
    public final fun submit (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
}

public abstract class io/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler$State {
}

public final class io/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler$State$Drafting : io/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler$State {
    public fun <init> (Ljava/lang/Object;)V
    public final fun component1 ()Ljava/lang/Object;
    public final fun copy (Ljava/lang/Object;)Lio/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler$State$Drafting;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler$State$Drafting;Ljava/lang/Object;ILjava/lang/Object;)Lio/github/mobilebytelabs/worker/storeflow/submit/DraftSubmitHandler$State$Drafting;
    public fun equals (Ljava/lang/Object;)Z
    public final fun getPayload ()Ljava/lang/Object;
    public fun hashCode ()I
    public fun toString ()Ljava/lang/String;
}
```

### cmp-worker-storeflow.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64, js, wasmJs]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-storeflow>
final enum class io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy : kotlin/Enum<io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy> { // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy|null[0]
    enum entry CACHE_ONLY // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy.CACHE_ONLY|null[0]
    enum entry CACHE_THEN_NETWORK // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy.CACHE_THEN_NETWORK|null[0]
    enum entry NETWORK_ONLY // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy.NETWORK_ONLY|null[0]

    final val entries // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy.entries|#static{}entries[0]
        final fun <get-entries>(): kotlin.enums/EnumEntries<io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy> // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy.entries.<get-entries>|<get-entries>#static(){}[0]

    final fun valueOf(kotlin/String): io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy.valueOf|valueOf#static(kotlin.String){}[0]
    final fun values(): kotlin/Array<io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy> // io.github.mobilebytelabs.worker.storeflow.policy/FetchPolicy.values|values#static(){}[0]
}

final enum class io.github.mobilebytelabs.worker.storeflow.submit/OutboxState : kotlin/Enum<io.github.mobilebytelabs.worker.storeflow.submit/OutboxState> { // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState|null[0]
    enum entry FAILED // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.FAILED|null[0]
    enum entry PENDING // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.PENDING|null[0]
    enum entry RETRYING // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.RETRYING|null[0]
    enum entry SUBMITTED // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.SUBMITTED|null[0]

    final val entries // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.entries|#static{}entries[0]
        final fun <get-entries>(): kotlin.enums/EnumEntries<io.github.mobilebytelabs.worker.storeflow.submit/OutboxState> // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.entries.<get-entries>|<get-entries>#static(){}[0]

    final fun valueOf(kotlin/String): io.github.mobilebytelabs.worker.storeflow.submit/OutboxState // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.valueOf|valueOf#static(kotlin.String){}[0]
    final fun values(): kotlin/Array<io.github.mobilebytelabs.worker.storeflow.submit/OutboxState> // io.github.mobilebytelabs.worker.storeflow.submit/OutboxState.values|values#static(){}[0]
}

abstract interface <#A: kotlin/Any> io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox { // io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox|null[0]
    abstract suspend fun deleteSubmitted() // io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox.deleteSubmitted|deleteSubmitted(){}[0]
    abstract suspend fun enqueue(#A): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox.enqueue|enqueue(1:0){}[0]
    abstract suspend fun getAllPending(): kotlin.collections/List<io.github.mobilebytelabs.worker.storeflow.submit/OutboxEntry<#A>> // io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox.getAllPending|getAllPending(){}[0]
    abstract suspend fun markFailed(kotlin.uuid/Uuid, kotlin/String?) // io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox.markFailed|markFailed(kotlin.uuid.Uuid;kotlin.String?){}[0]
    abstract suspend fun markRetrying(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox.markRetrying|markRetrying(kotlin.uuid.Uuid){}[0]
    abstract suspend fun markSubmitted(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.storeflow.submit/SubmitOutbox.markSubmitted|markSubmitted(kotlin.uuid.Uuid){}[0]
}

abstract class io.github.mobilebytelabs.worker.storeflow.paging/PrefetchPagingWorker : io.github.mobilebytelabs.worker/CoroutineWorker { // io.github.mobilebytelabs.worker.storeflow.paging/PrefetchPagingWorker|null[0]
    constructor <init>(io.github.mobilebytelabs.worker/WorkerContext) // io.github.mobilebytelabs.worker.storeflow.paging/PrefetchPagingWorker.<init>|<init>(io.github.mobilebytelabs.worker.WorkerContext){}[0]

    abstract suspend fun fetchPage(kotlin/Int) // io.github.mobilebytelabs.worker.storeflow.paging/PrefetchPagingWorker.fetchPage|fetchPage(kotlin.Int){}[0]
    open suspend fun doWork(): io.github.mobilebytelabs.worker/WorkResult // io.github.mobilebytelabs.worker.storeflow.paging/PrefetchPagingWorker.doWork|doWork(){}[0]

    final object Companion { // io.github.mobilebytelabs.worker.storeflow.paging/PrefetchPagingWorker.Companion|null[0]
        final const val DEFAULT_PAGE_COUNT // io.github.mobilebytelabs.worker.storeflow.paging/PrefetchPagingWorker.Companion.DEFAULT_PAGE_COUNT|{}DEFAULT_PAGE_COUNT[0]
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
| (no open PRs labeled `cmp-worker-storeflow` — refresh via `gh pr list --label cmp-worker-storeflow` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-storeflow@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-storeflow@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-storeflow.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-storeflow.observability_opt_in=true` in consumer's `lib-integrate.properties`.
