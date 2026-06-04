---
title: "Coverage"
description: "Kover-based test coverage setup + the worker-kmp 100%-coverage gate for cmp-worker-* modules."
---

# Code Coverage (Kover)

worker-kmp uses [kotlinx-kover](https://github.com/Kotlin/kotlinx-kover) for line-coverage
measurement + enforcement. Since v3.1.3, **6 testable commonMain modules carry a hard 100%
line-coverage floor**; CI fails PRs that regress below it. Two additional modules
(`cmp-worker-scheduler`, `cmp-worker-compose`) are test-complete but cannot opt in yet — see
[Known limitation: AGP 9 + Kover 0.9.1](#known-limitation--agp-9--kover-091).

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

## Modules NOT in scope (with rationale)

| Module | Why excluded |
|---|---|
| `cmp-worker-scheduler` | Test suite complete (39 `@Test`s across 8 commonTest files run via `jvmTest`) but Kover plugin NOT applied. **Blocked on upstream Kover support for AGP 9's `android.kotlin.multiplatform.library` extension shape.** Tracked by follow-up plan [`kover-agp9-opt-in-followup`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/kover-agp9-opt-in-followup/PLAN.md). |
| `cmp-worker-compose` | Test suite complete (21 `@Test`s incl. `NonComposableSurfacesTest`) but Kover plugin NOT applied for the same AGP 9 reason. `@Composable` bodies would be filter-excluded anyway; the testable non-`@Composable` utilities (state-mapping, request builders) are covered. Tracked by the same follow-up plan. |
| `cmp-worker-app-annotations` | Annotation-only module — no executable code to measure. |
| `cmp-worker-android`, `cmp-worker-ios`, `cmp-worker-desktop`, `cmp-worker-desktop-daemon`, `cmp-worker-web`, `cmp-worker-web-push` | Platform actuals; Kover only measures JVM bytecode. Tracked separately by the Tier-2 follow-up epic [`worker-kmp-platform-engine-tests`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-platform-engine-tests/GOAL.md), which adds Robolectric (Android) + Xcode-sim (iOS) + browser-test (Web) harnesses. |
| `cmp-worker-app-ksp`, `cmp-worker-app-plugin` | Build-tooling (KSP processor + Gradle plugin) — needs Gradle TestKit; deferred. |
| `cmp-worker-bench`, `cmp-worker-migrate` | Non-production tooling. |
| `samples/*` | Illustrative; not under coverage threshold. |

## Known limitation — AGP 9 + Kover 0.9.1

Kover 0.9.1 cannot introspect the new `android.kotlin.multiplatform.library` Gradle plugin
extension shape that AGP 9+ uses for KMP-Android library modules. Applying
`id("io.github.mobilebytelabs.kover")` to `cmp-worker-scheduler` or `cmp-worker-compose`
fails at configuration time.

**Workaround in place:** the convention plugin is intentionally NOT applied to those two
modules. Their tests still run via `./gradlew :cmp-worker-scheduler:jvmTest` and
`./gradlew :cmp-worker-compose:jvmTest`; they just don't contribute to Kover aggregation.

**Revisit criteria:** when Kover ships a release that supports the AGP 9 KMP-Android
library extension (track [Kover #649](https://github.com/Kotlin/kotlinx-kover/issues/649)
or the equivalent), bump `kover` in `gradle/libs.versions.toml`, apply
`id("io.github.mobilebytelabs.kover")` to both modules' `plugins {}` blocks, and remove the
explanatory comment block. The follow-up plan [`kover-agp9-opt-in-followup`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/kover-agp9-opt-in-followup/PLAN.md)
carries the full checklist.

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

This is `LINE` aggregation per the Kover 0.9.1 default; per-module — each opted-in module
gates its own bytecode.

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

- Coverage epic plan: [`plan-layer/project-plans/mbs/worker-kmp/active/kover-100-coverage/`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/kover-100-coverage/)
- Tier-2 platform-engine tests: [`worker-kmp-platform-engine-tests`](../../../../plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-platform-engine-tests/GOAL.md)
- Convention plugin pattern: [`convention-plugin.md`](convention-plugin.md)
