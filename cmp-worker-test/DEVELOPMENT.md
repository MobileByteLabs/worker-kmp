---
module: cmp-worker-test
artifact: io.github.mobilebytelabs:cmp-worker-test
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.test
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-test — Development

> Single source of truth for development state of `cmp-worker-test` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-test` | `com.mobilebytelabs.kmptoolkit.worker.test` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-test) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-test/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-test.api
```
public final class io/github/mobilebytelabs/worker/test/TestWorkManager : io/github/mobilebytelabs/worker/WorkManager {
	public fun <init> ()V
	public fun cancelAllWorkByTag (Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
	public fun cancelWorkById (Lkotlin/uuid/Uuid;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
	public fun enqueue (Lio/github/mobilebytelabs/worker/OneTimeWorkRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
	public fun enqueueUniquePeriodicWork (Ljava/lang/String;Lio/github/mobilebytelabs/worker/ExistingPeriodicWorkPolicy;Lio/github/mobilebytelabs/worker/PeriodicWorkRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
	public final fun getEnqueuedRequests ()Ljava/util/List;
	public final fun getLastEnqueuedRequest ()Lio/github/mobilebytelabs/worker/WorkRequest;
	public final fun getUniqueWorkNames ()Ljava/util/List;
	public fun getWorkInfoById (Lkotlin/uuid/Uuid;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
	public fun getWorkInfosByTag (Ljava/lang/String;)Lkotlinx/coroutines/flow/Flow;
	public final fun hasWorkWithTag (Ljava/lang/String;)Z
	public final fun reset ()V
	public final fun simulateWorkFailure (Lkotlin/uuid/Uuid;)V
	public final fun simulateWorkProgress (Lkotlin/uuid/Uuid;Lio/github/mobilebytelabs/worker/WorkProgress;)V
	public final fun simulateWorkRetry (Lkotlin/uuid/Uuid;)V
	public final fun simulateWorkRunning (Lkotlin/uuid/Uuid;)V
	public final fun simulateWorkSuccess (Lkotlin/uuid/Uuid;Lio/github/mobilebytelabs/worker/WorkData;)V
	public static synthetic fun simulateWorkSuccess$default (Lio/github/mobilebytelabs/worker/test/TestWorkManager;Lkotlin/uuid/Uuid;Lio/github/mobilebytelabs/worker/WorkData;ILjava/lang/Object;)V
	public final fun workCountWithTag (Ljava/lang/String;)I
}
```

### cmp-worker-test.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64, js]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-test>
final class io.github.mobilebytelabs.worker.test/TestWorkManager : io.github.mobilebytelabs.worker/WorkManager { // io.github.mobilebytelabs.worker.test/TestWorkManager|null[0]
    constructor <init>() // io.github.mobilebytelabs.worker.test/TestWorkManager.<init>|<init>(){}[0]

    final val enqueuedRequests // io.github.mobilebytelabs.worker.test/TestWorkManager.enqueuedRequests|{}enqueuedRequests[0]
        final fun <get-enqueuedRequests>(): kotlin.collections/List<io.github.mobilebytelabs.worker/WorkRequest> // io.github.mobilebytelabs.worker.test/TestWorkManager.enqueuedRequests.<get-enqueuedRequests>|<get-enqueuedRequests>(){}[0]
    final val lastEnqueuedRequest // io.github.mobilebytelabs.worker.test/TestWorkManager.lastEnqueuedRequest|{}lastEnqueuedRequest[0]
        final fun <get-lastEnqueuedRequest>(): io.github.mobilebytelabs.worker/WorkRequest? // io.github.mobilebytelabs.worker.test/TestWorkManager.lastEnqueuedRequest.<get-lastEnqueuedRequest>|<get-lastEnqueuedRequest>(){}[0]
    final val uniqueWorkNames // io.github.mobilebytelabs.worker.test/TestWorkManager.uniqueWorkNames|{}uniqueWorkNames[0]
        final fun <get-uniqueWorkNames>(): kotlin.collections/List<kotlin/String> // io.github.mobilebytelabs.worker.test/TestWorkManager.uniqueWorkNames.<get-uniqueWorkNames>|<get-uniqueWorkNames>(){}[0]

    final fun getWorkInfosByTag(kotlin/String): kotlinx.coroutines.flow/Flow<kotlin.collections/List<io.github.mobilebytelabs.worker/WorkInfo>> // io.github.mobilebytelabs.worker.test/TestWorkManager.getWorkInfosByTag|getWorkInfosByTag(kotlin.String){}[0]
    final fun hasWorkWithTag(kotlin/String): kotlin/Boolean // io.github.mobilebytelabs.worker.test/TestWorkManager.hasWorkWithTag|hasWorkWithTag(kotlin.String){}[0]
    final fun reset() // io.github.mobilebytelabs.worker.test/TestWorkManager.reset|reset(){}[0]
    final fun simulateWorkFailure(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.test/TestWorkManager.simulateWorkFailure|simulateWorkFailure(kotlin.uuid.Uuid){}[0]
    final fun simulateWorkProgress(kotlin.uuid/Uuid, io.github.mobilebytelabs.worker/WorkProgress) // io.github.mobilebytelabs.worker.test/TestWorkManager.simulateWorkProgress|simulateWorkProgress(kotlin.uuid.Uuid;io.github.mobilebytelabs.worker.WorkProgress){}[0]
    final fun simulateWorkRetry(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.test/TestWorkManager.simulateWorkRetry|simulateWorkRetry(kotlin.uuid.Uuid){}[0]
    final fun simulateWorkRunning(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.test/TestWorkManager.simulateWorkRunning|simulateWorkRunning(kotlin.uuid.Uuid){}[0]
    final fun simulateWorkSuccess(kotlin.uuid/Uuid, io.github.mobilebytelabs.worker/WorkData = ...) // io.github.mobilebytelabs.worker.test/TestWorkManager.simulateWorkSuccess|simulateWorkSuccess(kotlin.uuid.Uuid;io.github.mobilebytelabs.worker.WorkData){}[0]
    final fun workCountWithTag(kotlin/String): kotlin/Int // io.github.mobilebytelabs.worker.test/TestWorkManager.workCountWithTag|workCountWithTag(kotlin.String){}[0]
    final suspend fun cancelAllWorkByTag(kotlin/String) // io.github.mobilebytelabs.worker.test/TestWorkManager.cancelAllWorkByTag|cancelAllWorkByTag(kotlin.String){}[0]
    final suspend fun cancelWorkById(kotlin.uuid/Uuid) // io.github.mobilebytelabs.worker.test/TestWorkManager.cancelWorkById|cancelWorkById(kotlin.uuid.Uuid){}[0]
    final suspend fun enqueue(io.github.mobilebytelabs.worker/OneTimeWorkRequest): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.test/TestWorkManager.enqueue|enqueue(io.github.mobilebytelabs.worker.OneTimeWorkRequest){}[0]
    final suspend fun enqueueUniquePeriodicWork(kotlin/String, io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy, io.github.mobilebytelabs.worker/PeriodicWorkRequest): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.test/TestWorkManager.enqueueUniquePeriodicWork|enqueueUniquePeriodicWork(kotlin.String;io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy;io.github.mobilebytelabs.worker.PeriodicWorkRequest){}[0]
    final suspend fun getWorkInfoById(kotlin.uuid/Uuid): io.github.mobilebytelabs.worker/WorkInfo? // io.github.mobilebytelabs.worker.test/TestWorkManager.getWorkInfoById|getWorkInfoById(kotlin.uuid.Uuid){}[0]
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
| (no open PRs labeled `cmp-worker-test` — refresh via `gh pr list --label cmp-worker-test` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-test@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-test@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-test.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-test.observability_opt_in=true` in consumer's `lib-integrate.properties`.
