---
module: cmp-worker-web-push
artifact: io.github.mobilebytelabs:cmp-worker-web-push
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.web.push
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-web-push — Development

> Single source of truth for development state of `cmp-worker-web-push` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-web-push` | `com.mobilebytelabs.kmptoolkit.worker.web.push` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-web-push) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-web-push/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| iosMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| jvmMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| jsMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |
| wasmJsMain | ✅ | ✅ real | 0 | 1 | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-web-push.api
```
public final class io/github/mobilebytelabs/worker/webpush/WebPushConfig {
    public fun <init> ()V
    public fun <init> (ZLjava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function1;ZZLjava/lang/String;I)V
    public synthetic fun <init> (ZLjava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function1;ZZLjava/lang/String;IILkotlin/jvm/internal/DefaultConstructorMarker;)V
    public final fun component1 ()Z
    public final fun component2 ()Ljava/lang/String;
    public final fun component3 ()Ljava/lang/String;
    public final fun component4 ()Lkotlin/jvm/functions/Function1;
    public final fun component5 ()Z
    public final fun component6 ()Z
    public final fun component7 ()Ljava/lang/String;
    public final fun component8 ()I
    public final fun copy (ZLjava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function1;ZZLjava/lang/String;I)Lio/github/mobilebytelabs/worker/webpush/WebPushConfig;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/webpush/WebPushConfig;ZLjava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function1;ZZLjava/lang/String;IILjava/lang/Object;)Lio/github/mobilebytelabs/worker/webpush/WebPushConfig;
    public fun equals (Ljava/lang/Object;)Z
    public final fun getEnabled ()Z
    public final fun getForegroundFallback ()Z
    public final fun getNotificationPermissionAutoRequest ()Z
    public final fun getServerEndpoint ()Ljava/lang/String;
    public final fun getServerEndpointAuthHeader ()Lkotlin/jvm/functions/Function1;
    public final fun getServiceWorkerScript ()Ljava/lang/String;
    public final fun getSubscriptionExpiryDays ()I
    public final fun getVapidPublicKey ()Ljava/lang/String;
    public fun hashCode ()I
    public fun toString ()Ljava/lang/String;
}

public abstract interface class io/github/mobilebytelabs/worker/webpush/WebPushSubscriber {
    public abstract fun currentSubscription (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public abstract fun ensureSubscribed (Lio/github/mobilebytelabs/worker/webpush/WebPushConfig;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
    public abstract fun getPushSupported ()Z
    public abstract fun getRequiresPwaInstall ()Z
    public abstract fun unsubscribe (Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
}

public final class io/github/mobilebytelabs/worker/webpush/WebPushSubscriber_jvmKt {
    public static final fun createWebPushSubscriber ()Lio/github/mobilebytelabs/worker/webpush/WebPushSubscriber;
}

public final class io/github/mobilebytelabs/worker/webpush/WebPushSubscription {
    public fun <init> (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    public final fun component1 ()Ljava/lang/String;
    public final fun component2 ()Ljava/lang/String;
    public final fun component3 ()Ljava/lang/String;
    public final fun copy (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lio/github/mobilebytelabs/worker/webpush/WebPushSubscription;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/webpush/WebPushSubscription;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;)Lio/github/mobilebytelabs/worker/webpush/WebPushSubscription;
    public fun equals (Ljava/lang/Object;)Z
    public final fun getAuth ()Ljava/lang/String;
    public final fun getEndpoint ()Ljava/lang/String;
    public final fun getP256dh ()Ljava/lang/String;
```

### cmp-worker-web-push.klib.api
```
// Klib ABI Dump
// Targets: [iosArm64, iosSimulatorArm64, js, wasmJs]
// Rendering settings:
// - Signature version: 2
// - Show manifest properties: true
// - Show declarations: true

// Library unique name: <io.github.mobilebytelabs:cmp-worker-web-push>
abstract interface io.github.mobilebytelabs.worker.webpush/WebPushSubscriber { // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber|null[0]
    abstract val pushSupported // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber.pushSupported|{}pushSupported[0]
        abstract fun <get-pushSupported>(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber.pushSupported.<get-pushSupported>|<get-pushSupported>(){}[0]
    abstract val requiresPwaInstall // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber.requiresPwaInstall|{}requiresPwaInstall[0]
        abstract fun <get-requiresPwaInstall>(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber.requiresPwaInstall.<get-requiresPwaInstall>|<get-requiresPwaInstall>(){}[0]

    abstract suspend fun currentSubscription(): io.github.mobilebytelabs.worker.webpush/WebPushSubscription? // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber.currentSubscription|currentSubscription(){}[0]
    abstract suspend fun ensureSubscribed(io.github.mobilebytelabs.worker.webpush/WebPushConfig): io.github.mobilebytelabs.worker.webpush/WebPushSubscription? // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber.ensureSubscribed|ensureSubscribed(io.github.mobilebytelabs.worker.webpush.WebPushConfig){}[0]
    abstract suspend fun unsubscribe(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushSubscriber.unsubscribe|unsubscribe(){}[0]
}

final class io.github.mobilebytelabs.worker.webpush/WebPushConfig { // io.github.mobilebytelabs.worker.webpush/WebPushConfig|null[0]
    constructor <init>(kotlin/Boolean = ..., kotlin/String = ..., kotlin/String = ..., kotlin.coroutines/SuspendFunction0<kotlin/String>? = ..., kotlin/Boolean = ..., kotlin/Boolean = ..., kotlin/String = ..., kotlin/Int = ...) // io.github.mobilebytelabs.worker.webpush/WebPushConfig.<init>|<init>(kotlin.Boolean;kotlin.String;kotlin.String;kotlin.coroutines.SuspendFunction0<kotlin.String>?;kotlin.Boolean;kotlin.Boolean;kotlin.String;kotlin.Int){}[0]

    final val enabled // io.github.mobilebytelabs.worker.webpush/WebPushConfig.enabled|{}enabled[0]
        final fun <get-enabled>(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushConfig.enabled.<get-enabled>|<get-enabled>(){}[0]
    final val foregroundFallback // io.github.mobilebytelabs.worker.webpush/WebPushConfig.foregroundFallback|{}foregroundFallback[0]
        final fun <get-foregroundFallback>(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushConfig.foregroundFallback.<get-foregroundFallback>|<get-foregroundFallback>(){}[0]
    final val notificationPermissionAutoRequest // io.github.mobilebytelabs.worker.webpush/WebPushConfig.notificationPermissionAutoRequest|{}notificationPermissionAutoRequest[0]
        final fun <get-notificationPermissionAutoRequest>(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushConfig.notificationPermissionAutoRequest.<get-notificationPermissionAutoRequest>|<get-notificationPermissionAutoRequest>(){}[0]
    final val serverEndpoint // io.github.mobilebytelabs.worker.webpush/WebPushConfig.serverEndpoint|{}serverEndpoint[0]
        final fun <get-serverEndpoint>(): kotlin/String // io.github.mobilebytelabs.worker.webpush/WebPushConfig.serverEndpoint.<get-serverEndpoint>|<get-serverEndpoint>(){}[0]
    final val serverEndpointAuthHeader // io.github.mobilebytelabs.worker.webpush/WebPushConfig.serverEndpointAuthHeader|{}serverEndpointAuthHeader[0]
        final fun <get-serverEndpointAuthHeader>(): kotlin.coroutines/SuspendFunction0<kotlin/String>? // io.github.mobilebytelabs.worker.webpush/WebPushConfig.serverEndpointAuthHeader.<get-serverEndpointAuthHeader>|<get-serverEndpointAuthHeader>(){}[0]
    final val serviceWorkerScript // io.github.mobilebytelabs.worker.webpush/WebPushConfig.serviceWorkerScript|{}serviceWorkerScript[0]
        final fun <get-serviceWorkerScript>(): kotlin/String // io.github.mobilebytelabs.worker.webpush/WebPushConfig.serviceWorkerScript.<get-serviceWorkerScript>|<get-serviceWorkerScript>(){}[0]
    final val subscriptionExpiryDays // io.github.mobilebytelabs.worker.webpush/WebPushConfig.subscriptionExpiryDays|{}subscriptionExpiryDays[0]
        final fun <get-subscriptionExpiryDays>(): kotlin/Int // io.github.mobilebytelabs.worker.webpush/WebPushConfig.subscriptionExpiryDays.<get-subscriptionExpiryDays>|<get-subscriptionExpiryDays>(){}[0]
    final val vapidPublicKey // io.github.mobilebytelabs.worker.webpush/WebPushConfig.vapidPublicKey|{}vapidPublicKey[0]
        final fun <get-vapidPublicKey>(): kotlin/String // io.github.mobilebytelabs.worker.webpush/WebPushConfig.vapidPublicKey.<get-vapidPublicKey>|<get-vapidPublicKey>(){}[0]

    final fun component1(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component1|component1(){}[0]
    final fun component2(): kotlin/String // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component2|component2(){}[0]
    final fun component3(): kotlin/String // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component3|component3(){}[0]
    final fun component4(): kotlin.coroutines/SuspendFunction0<kotlin/String>? // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component4|component4(){}[0]
    final fun component5(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component5|component5(){}[0]
    final fun component6(): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component6|component6(){}[0]
    final fun component7(): kotlin/String // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component7|component7(){}[0]
    final fun component8(): kotlin/Int // io.github.mobilebytelabs.worker.webpush/WebPushConfig.component8|component8(){}[0]
    final fun copy(kotlin/Boolean = ..., kotlin/String = ..., kotlin/String = ..., kotlin.coroutines/SuspendFunction0<kotlin/String>? = ..., kotlin/Boolean = ..., kotlin/Boolean = ..., kotlin/String = ..., kotlin/Int = ...): io.github.mobilebytelabs.worker.webpush/WebPushConfig // io.github.mobilebytelabs.worker.webpush/WebPushConfig.copy|copy(kotlin.Boolean;kotlin.String;kotlin.String;kotlin.coroutines.SuspendFunction0<kotlin.String>?;kotlin.Boolean;kotlin.Boolean;kotlin.String;kotlin.Int){}[0]
    final fun equals(kotlin/Any?): kotlin/Boolean // io.github.mobilebytelabs.worker.webpush/WebPushConfig.equals|equals(kotlin.Any?){}[0]
    final fun hashCode(): kotlin/Int // io.github.mobilebytelabs.worker.webpush/WebPushConfig.hashCode|hashCode(){}[0]
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
| (no open PRs labeled `cmp-worker-web-push` — refresh via `gh pr list --label cmp-worker-web-push` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-web-push@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-web-push@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-web-push.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-web-push.observability_opt_in=true` in consumer's `lib-integrate.properties`.
