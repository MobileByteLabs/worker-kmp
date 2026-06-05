---
title: "Coverage"
description: "Kover-based test coverage setup + the worker-kmp 100%-coverage gate for cmp-worker-* modules."
---

# Code Coverage (Kover)

worker-kmp uses [kotlinx-kover](https://github.com/Kotlin/kotlinx-kover) for line-coverage
measurement + enforcement. Since v3.1.3, **8 testable commonMain modules carry a hard 100%
line-coverage floor**; CI fails PRs that regress below it.

## Quick start

```bash
# Generate aggregate HTML report
./gradlew koverHtmlReport
open build/reports/kover/html/index.html   # macOS

# Generate XML report (CI / codecov consumption)
./gradlew koverXmlReport

# Verify 100% line coverage per opted-in module (fails on regression)
./gradlew koverVerify
```

## What's measured

Kover instruments **JVM bytecode only**. KMP `commonMain` code compiles to JVM (via `jvmTest`)
so it IS measured. iOS, JS, and wasmJs platform actuals do NOT get coverage — see the exclusions
below.

## Modules in scope

| Module | Status |
|---|---|
| `cmp-worker-kmp` | 100% commonMain (gated by `koverVerify`) |
| `cmp-worker-store5` | 100% commonMain (gated by `koverVerify`) |
| `cmp-worker-storeflow` | 100% commonMain (gated by `koverVerify`) |
| `cmp-worker-koin` | 100% commonMain (gated by `koverVerify`) |
| `cmp-worker-sync` | 100% commonMain (gated by `koverVerify`) |
| `cmp-worker-test` | 100% commonMain (gated by `koverVerify`) |
| `cmp-worker-scheduler` | 100% commonMain (gated by `koverVerify`) |
| `cmp-worker-compose` | 100% commonMain (gated by `koverVerify`; `@Composable` bodies filter-excluded per exclusion patterns) |

## Modules NOT in scope (with rationale)

| Module | Why excluded |
|---|---|
| `cmp-worker-app-annotations` | Annotation-only module — no executable code to measure. |
| `cmp-worker-android`, `cmp-worker-ios`, `cmp-worker-desktop`, `cmp-worker-desktop-daemon`, `cmp-worker-web`, `cmp-worker-web-push` | Platform actuals; Kover only measures JVM bytecode. Tracked separately by the Tier-2 follow-up epic [`worker-kmp-platform-engine-tests`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-platform-engine-tests/GOAL.md), which adds Robolectric (Android) + Xcode-sim (iOS) + browser-test (Web) harnesses. |
| `cmp-worker-app-ksp`, `cmp-worker-app-plugin` | Build-tooling (KSP processor + Gradle plugin) — needs Gradle TestKit; deferred. |
| `cmp-worker-bench`, `cmp-worker-migrate` | Non-production tooling. |
| `samples/*` | Illustrative; not under coverage threshold. |

## Exclusion patterns

The root [`KoverConventionPlugin`](../../build-logic/convention/src/main/kotlin/KoverConventionPlugin.kt)
delegates to `configureKoverRootReports()` in
[`Kover.kt`](../../build-logic/convention/src/main/kotlin/io/github/mobilebytelabs/worker/convention/Kover.kt),
which excludes:

```kotlin
excludes {
    classes(
        "*.di.*",                       // Koin DI declarations
        "*.BuildConfig",                // Android-generated BuildConfig
        "*ComposableSingletons*",       // Compose generated lambda holders
        "*_*Factory*",                  // Generated factories
        "*\$ComposableLambda\$*",
        "*Preview*",                    // @Preview functions
        "*Test*",                       // Test helpers themselves
    )
    packages(
        "*.generated.*",
        "*.ksp.*",
        "*.android", "*.ios", "*.jvm", "*.js", "*.wasmJs",  // platform actuals
    )
    annotatedBy("androidx.compose.runtime.Composable")
}
```

The verify rule enforces:

```kotlin
verify {
    rule { minBound(100) }
}
```

This is `LINE` aggregation per the Kover default; per-module — each opted-in module
gates its own bytecode.

## Tier-2 platform-engine tests

Kover measures JVM bytecode line coverage only. The 6 platform-actual modules
(`cmp-worker-android` / `cmp-worker-ios` / `cmp-worker-desktop` /
`cmp-worker-desktop-daemon` / `cmp-worker-web` / `cmp-worker-web-push`)
fundamentally cannot be measured this way for non-JVM targets, so a
**structural** success criterion is used instead:

- Every public actual class has ≥ 1 test class.
- Every per-platform Gradle test task passes on its runner.

These tests run via the
[`.github/workflows/platform-engine-tests.yml`](../../.github/workflows/platform-engine-tests.yml)
matrix (6 jobs: 5 ubuntu-latest + 1 macos-latest for iOS sim). The workflow
is a required check on PRs to `development`.

Per-module test tasks:

| Module | Test task | CI runner |
|---|---|---|
| `cmp-worker-android` | `:cmp-worker-android:testAndroidHostTest` (Robolectric) | ubuntu-latest |
| `cmp-worker-ios` | `:cmp-worker-ios:iosSimulatorArm64Test` | macos-latest |
| `cmp-worker-desktop` | `:cmp-worker-desktop:jvmTest` | ubuntu-latest |
| `cmp-worker-desktop-daemon` | `:cmp-worker-desktop-daemon:test` | ubuntu-latest |
| `cmp-worker-web` | `:cmp-worker-web:jsNodeTest` + `wasmJsBrowserTest` | ubuntu-latest |
| `cmp-worker-web-push` | `:cmp-worker-web-push:jsBrowserTest` + `wasmJsBrowserTest` | ubuntu-latest |

Spec:
[`worker-kmp-platform-engine-tests`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-platform-engine-tests/GOAL.md).

## Adding a new module

1. Add `id("io.github.mobilebytelabs.kover")` to the new module's `build.gradle.kts` `plugins {}`
   block.
2. The convention plugin auto-registers it with the root aggregator via
   `rootProject.dependencies.add("kover", project)`.
3. `./gradlew :{new-module}:koverVerify` will start failing until 100% line coverage is reached.
4. If the new module is platform-heavy (mostly `androidMain` / `iosMain` / etc.), do NOT apply
   the plugin — add a row to the "Modules NOT in scope" table above and surface the gap in the
   Tier-2 follow-up epic.

## Escape hatches

For provably-uncoverable lines (e.g. `sealed when` synthetic `else` branches that the bytecode
verifier mis-counts as missed), two options:

1. **Per-class exclusion** — extend the `classes(...)` list in `Kover.kt` with the FQN.
   Document the reason in a code comment.
2. **`@ExcludeFromCoverage`** (future) — once the marker annotation lands, annotate the
   uncoverable member and reference it via the filter's `annotatedBy(...)`.

Every escape hatch must carry a code-comment rationale and a linked issue. The quarterly
maintenance pass prunes stale exclusions.

## CI workflow

The `Test Coverage / Kover 100% gate` job at
[`.github/workflows/test-coverage.yml`](../../.github/workflows/test-coverage.yml) runs on every
PR + push to `main` / `development`. It:

1. Runs `./gradlew koverHtmlReport koverXmlReport koverVerify`.
2. Uploads the aggregate HTML report as `kover-html-report` (14-day retention).
3. Posts the aggregate `%` to the PR Step Summary.

`koverVerify` exit ≠ 0 fails the PR. There is no soft-warn mode.

## Related

- Coverage epic plan: [`plan-layer/project-plans/mbs/worker-kmp/archive/2026-06/kover-100-coverage/`](../../../../plan-layer/project-plans/mbs/worker-kmp/archive/2026-06/kover-100-coverage/)
- Tier-2 platform-engine tests: [`worker-kmp-platform-engine-tests`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-platform-engine-tests/GOAL.md)
- Convention plugin pattern: [`convention-plugin.md`](convention-plugin.md)
