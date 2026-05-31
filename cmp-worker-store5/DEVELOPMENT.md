---
module: cmp-worker-store5
artifact: io.github.mobilebytelabs:cmp-worker-store5
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.store5
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-store5 — Development

> Single source of truth for development state of `cmp-worker-store5` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-store5` | `com.mobilebytelabs.kmptoolkit.worker.store5` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-store5) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-store5/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-store5.api
```
public abstract class io/github/mobilebytelabs/worker/store5/MutableStoreSyncWorker : io/github/mobilebytelabs/worker/CoroutineWorker {
    public fun <init> (Lio/github/mobilebytelabs/worker/WorkerContext;Lorg/mobilenativefoundation/store/store5/MutableStore;Ljava/lang/Object;Ljava/lang/Object;)V
    public fun doWork (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    protected fun isRetryable (Ljava/lang/Throwable;)Z
    protected fun mapWriteResponseToWorkData (Lorg/mobilenativefoundation/store/store5/StoreWriteResponse$Success;)Lio/github/mobilebytelabs/worker/WorkData;
}

public abstract class io/github/mobilebytelabs/worker/store5/StoreBackedWorker : io/github/mobilebytelabs/worker/CoroutineWorker {
    public fun <init> (Lio/github/mobilebytelabs/worker/WorkerContext;Lorg/mobilenativefoundation/store/store5/Store;Ljava/lang/Object;)V
    public fun doWork (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    protected fun isRetryable (Ljava/lang/Throwable;)Z
    protected fun mapOutputToWorkData (Ljava/lang/Object;)Lio/github/mobilebytelabs/worker/WorkData;
}

public abstract class io/github/mobilebytelabs/worker/store5/StoreFreshnessWorker : io/github/mobilebytelabs/worker/CoroutineWorker {
    public static final field Companion Lio/github/mobilebytelabs/worker/store5/StoreFreshnessWorker$Companion;
    public static final field KEY_SKIPPED Ljava/lang/String;
    public static final field VALUE_FRESH Ljava/lang/String;
    public fun <init> (Lio/github/mobilebytelabs/worker/WorkerContext;Lorg/mobilenativefoundation/store/store5/Store;Ljava/lang/Object;Lorg/mobilenativefoundation/store/store5/Validator;)V
    public fun doWork (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    protected fun isRetryable (Ljava/lang/Throwable;)Z
}

public final class io/github/mobilebytelabs/worker/store5/StoreFreshnessWorker$Companion {
}

public final class io/github/mobilebytelabs/worker/store5/StoreRefreshScheduler {
    public static final field Companion Lio/github/mobilebytelabs/worker/store5/StoreRefreshScheduler$Companion;
    public static final field TAG_PREFIX Ljava/lang/String;
    public fun <init> (Lio/github/mobilebytelabs/worker/WorkManager;)V
    public final fun cancelRefresh (Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public final fun enqueueUnique (Ljava/lang/String;Lio/github/mobilebytelabs/worker/ExistingPeriodicWorkPolicy;Lio/github/mobilebytelabs/worker/PeriodicWorkRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public final fun observeRefreshes (Ljava/lang/String;)Lkotlinx/coroutines/flow/Flow;
}

public final class io/github/mobilebytelabs/worker/store5/StoreRefreshScheduler$Companion {
    public final fun refreshTag (Ljava/lang/String;)Ljava/lang/String;
}

public final class io/github/mobilebytelabs/worker/store5/koin/WorkStore5KoinModuleKt {
    public static final fun getWorkStore5KoinModule ()Lorg/koin/core/module/Module;
}
```

### cmp-worker-store5.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64, js, wasmJs]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-store5>
abstract class <#A: kotlin/Any, #B: kotlin/Any> io.github.mobilebytelabs.worker.store5/MutableStoreSyncWorker : io.github.mobilebytelabs.worker/CoroutineWorker { // io.github.mobilebytelabs.worker.store5/MutableStoreSyncWorker|null[0]
    constructor <init>(io.github.mobilebytelabs.worker/WorkerContext, org.mobilenativefoundation.store.store5/MutableStore<#A, #B>, #A, #B) // io.github.mobilebytelabs.worker.store5/MutableStoreSyncWorker.<init>|<init>(io.github.mobilebytelabs.worker.WorkerContext;org.mobilenativefoundation.store.store5.MutableStore<1:0,1:1>;1:0;1:1){}[0]

    open fun (kotlin/Throwable).isRetryable(): kotlin/Boolean // io.github.mobilebytelabs.worker.store5/MutableStoreSyncWorker.isRetryable|isRetryable@kotlin.Throwable(){}[0]
    open fun mapWriteResponseToWorkData(org.mobilenativefoundation.store.store5/StoreWriteResponse.Success): io.github.mobilebytelabs.worker/WorkData // io.github.mobilebytelabs.worker.store5/MutableStoreSyncWorker.mapWriteResponseToWorkData|mapWriteResponseToWorkData(org.mobilenativefoundation.store.store5.StoreWriteResponse.Success){}[0]
    open suspend fun doWork(): io.github.mobilebytelabs.worker/WorkResult // io.github.mobilebytelabs.worker.store5/MutableStoreSyncWorker.doWork|doWork(){}[0]
}

abstract class <#A: kotlin/Any, #B: kotlin/Any> io.github.mobilebytelabs.worker.store5/StoreBackedWorker : io.github.mobilebytelabs.worker/CoroutineWorker { // io.github.mobilebytelabs.worker.store5/StoreBackedWorker|null[0]
    constructor <init>(io.github.mobilebytelabs.worker/WorkerContext, org.mobilenativefoundation.store.store5/Store<#A, #B>, #A) // io.github.mobilebytelabs.worker.store5/StoreBackedWorker.<init>|<init>(io.github.mobilebytelabs.worker.WorkerContext;org.mobilenativefoundation.store.store5.Store<1:0,1:1>;1:0){}[0]

    open fun (kotlin/Throwable).isRetryable(): kotlin/Boolean // io.github.mobilebytelabs.worker.store5/StoreBackedWorker.isRetryable|isRetryable@kotlin.Throwable(){}[0]
    open fun mapOutputToWorkData(#B): io.github.mobilebytelabs.worker/WorkData // io.github.mobilebytelabs.worker.store5/StoreBackedWorker.mapOutputToWorkData|mapOutputToWorkData(1:1){}[0]
    open suspend fun doWork(): io.github.mobilebytelabs.worker/WorkResult // io.github.mobilebytelabs.worker.store5/StoreBackedWorker.doWork|doWork(){}[0]
}

abstract class <#A: kotlin/Any, #B: kotlin/Any> io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker : io.github.mobilebytelabs.worker/CoroutineWorker { // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker|null[0]
    constructor <init>(io.github.mobilebytelabs.worker/WorkerContext, org.mobilenativefoundation.store.store5/Store<#A, #B>, #A, org.mobilenativefoundation.store.store5/Validator<#B>) // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.<init>|<init>(io.github.mobilebytelabs.worker.WorkerContext;org.mobilenativefoundation.store.store5.Store<1:0,1:1>;1:0;org.mobilenativefoundation.store.store5.Validator<1:1>){}[0]

    open fun (kotlin/Throwable).isRetryable(): kotlin/Boolean // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.isRetryable|isRetryable@kotlin.Throwable(){}[0]
    open suspend fun doWork(): io.github.mobilebytelabs.worker/WorkResult // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.doWork|doWork(){}[0]

    final object Companion { // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.Companion|null[0]
        final const val KEY_SKIPPED // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.Companion.KEY_SKIPPED|{}KEY_SKIPPED[0]
            final fun <get-KEY_SKIPPED>(): kotlin/String // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.Companion.KEY_SKIPPED.<get-KEY_SKIPPED>|<get-KEY_SKIPPED>(){}[0]
        final const val VALUE_FRESH // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.Companion.VALUE_FRESH|{}VALUE_FRESH[0]
            final fun <get-VALUE_FRESH>(): kotlin/String // io.github.mobilebytelabs.worker.store5/StoreFreshnessWorker.Companion.VALUE_FRESH.<get-VALUE_FRESH>|<get-VALUE_FRESH>(){}[0]
    }
}

final class io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler { // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler|null[0]
    constructor <init>(io.github.mobilebytelabs.worker/WorkManager) // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.<init>|<init>(io.github.mobilebytelabs.worker.WorkManager){}[0]

    final fun observeRefreshes(kotlin/String): kotlinx.coroutines.flow/Flow<kotlin.collections/List<io.github.mobilebytelabs.worker/WorkInfo>> // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.observeRefreshes|observeRefreshes(kotlin.String){}[0]
    final suspend fun cancelRefresh(kotlin/String) // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.cancelRefresh|cancelRefresh(kotlin.String){}[0]
    final suspend fun enqueueUnique(kotlin/String, io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy, io.github.mobilebytelabs.worker/PeriodicWorkRequest): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.enqueueUnique|enqueueUnique(kotlin.String;io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy;io.github.mobilebytelabs.worker.PeriodicWorkRequest){}[0]
    final suspend inline fun <#A1: reified io.github.mobilebytelabs.worker/CoroutineWorker> schedulePeriodicRefresh(kotlin.time/Duration, kotlin/String, io.github.mobilebytelabs.worker/Constraints = ..., io.github.mobilebytelabs.worker/ExistingPeriodicWorkPolicy = ...): kotlin.uuid/Uuid // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.schedulePeriodicRefresh|schedulePeriodicRefresh(kotlin.time.Duration;kotlin.String;io.github.mobilebytelabs.worker.Constraints;io.github.mobilebytelabs.worker.ExistingPeriodicWorkPolicy){0§<io.github.mobilebytelabs.worker.CoroutineWorker>}[0]

    final object Companion { // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.Companion|null[0]
        final const val TAG_PREFIX // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.Companion.TAG_PREFIX|{}TAG_PREFIX[0]
            final fun <get-TAG_PREFIX>(): kotlin/String // io.github.mobilebytelabs.worker.store5/StoreRefreshScheduler.Companion.TAG_PREFIX.<get-TAG_PREFIX>|<get-TAG_PREFIX>(){}[0]
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
| (no open PRs labeled `cmp-worker-store5` — refresh via `gh pr list --label cmp-worker-store5` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-store5@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-store5@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-store5.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-store5.observability_opt_in=true` in consumer's `lib-integrate.properties`.
