---
module: cmp-worker-desktop-daemon
artifact: io.github.mobilebytelabs:cmp-worker-desktop-daemon
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.worker.desktop.daemon
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-worker-desktop-daemon — Development

> Single source of truth for development state of `cmp-worker-desktop-daemon` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-worker-desktop-daemon` | `com.mobilebytelabs.kmptoolkit.worker.desktop.daemon` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-worker-desktop-daemon) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-worker-desktop-daemon/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|-------|
| (no src/{platform}Main/ directories found) | — | — | — | — | 2026-05-30 | — |

Legend: ✅ real impl, 🟡 UnsupportedPlatform stub, ⛔ not declared, — N/A.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- BCV baseline excerpt (first 50 lines per file) -->
### cmp-worker-desktop-daemon.api
```
public final class io/github/mobilebytelabs/worker/daemon/DesktopBackgroundDaemonKt {
    public static final fun main ([Ljava/lang/String;)V
}

public abstract interface class io/github/mobilebytelabs/worker/daemon/DesktopBackgroundInstaller {
    public abstract fun install (Lio/github/mobilebytelabs/worker/config/DesktopBackgroundConfig;)Lio/github/mobilebytelabs/worker/daemon/InstallResult;
    public abstract fun isInstalled (Ljava/lang/String;)Z
    public abstract fun probe ()Lio/github/mobilebytelabs/worker/daemon/DesktopOsCapability;
    public abstract fun uninstall (Ljava/lang/String;)Lio/github/mobilebytelabs/worker/daemon/InstallResult;
}

public final class io/github/mobilebytelabs/worker/daemon/DesktopBackgroundInstallerKt {
    public static final fun createDesktopBackgroundInstaller ()Lio/github/mobilebytelabs/worker/daemon/DesktopBackgroundInstaller;
}

public final class io/github/mobilebytelabs/worker/daemon/DesktopOsCapability {
    public fun <init> (Lio/github/mobilebytelabs/worker/daemon/OsFamily;ZZZZLjava/lang/String;)V
    public final fun component1 ()Lio/github/mobilebytelabs/worker/daemon/OsFamily;
    public final fun component2 ()Z
    public final fun component3 ()Z
    public final fun component4 ()Z
    public final fun component5 ()Z
    public final fun component6 ()Ljava/lang/String;
    public final fun copy (Lio/github/mobilebytelabs/worker/daemon/OsFamily;ZZZZLjava/lang/String;)Lio/github/mobilebytelabs/worker/daemon/DesktopOsCapability;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/daemon/DesktopOsCapability;Lio/github/mobilebytelabs/worker/daemon/OsFamily;ZZZZLjava/lang/String;ILjava/lang/Object;)Lio/github/mobilebytelabs/worker/daemon/DesktopOsCapability;
    public fun equals (Ljava/lang/Object;)Z
    public final fun getHasCron ()Z
    public final fun getHasLaunchctl ()Z
    public final fun getHasSchtasks ()Z
    public final fun getHasSystemctlUser ()Z
    public final fun getNotes ()Ljava/lang/String;
    public final fun getOsFamily ()Lio/github/mobilebytelabs/worker/daemon/OsFamily;
    public fun hashCode ()I
    public fun toString ()Ljava/lang/String;
}

public abstract class io/github/mobilebytelabs/worker/daemon/InstallResult {
}

public final class io/github/mobilebytelabs/worker/daemon/InstallResult$Failure : io/github/mobilebytelabs/worker/daemon/InstallResult {
    public fun <init> (Ljava/lang/String;)V
    public final fun component1 ()Ljava/lang/String;
    public final fun copy (Ljava/lang/String;)Lio/github/mobilebytelabs/worker/daemon/InstallResult$Failure;
    public static synthetic fun copy$default (Lio/github/mobilebytelabs/worker/daemon/InstallResult$Failure;Ljava/lang/String;ILjava/lang/Object;)Lio/github/mobilebytelabs/worker/daemon/InstallResult$Failure;
    public fun equals (Ljava/lang/Object;)Z
    public final fun getReason ()Ljava/lang/String;
    public fun hashCode ()I
    public fun toString ()Ljava/lang/String;
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
| (no open PRs labeled `cmp-worker-desktop-daemon` — refresh via `gh pr list --label cmp-worker-desktop-daemon` then re-run scan) | — | — | — | — |

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
| T0 (Crashlytics attribution) | enabled | custom_key: `library:cmp-worker-desktop-daemon@UNKNOWN` (set on init by FirebaseCrashlyticsAttributionHook) |
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
custom_key_format: "library:cmp-worker-desktop-daemon@UNKNOWN"
event_schema: []  # populate when T2 enabled — see library-runtime-observability epic AC #12-13
consumer_opt_in: "lib-integrate.properties#cmp-worker-desktop-daemon.observability_opt_in"
```

**Consumer opt-in:** controlled via `cmp-worker-desktop-daemon.observability_opt_in=true` in consumer's `lib-integrate.properties`.
