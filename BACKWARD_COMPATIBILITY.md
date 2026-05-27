# Backward Compatibility

> Deprecation policy + BC test gate for worker-kmp.

## Deprecation timeline

- **v2.x APIs are guaranteed working through v3.x.** Deprecation warnings appear in IDE / compile output; behavior unchanged.
- **v4.0.0 will remove v2.x deprecated APIs.** Migration toolkit (`cmp-worker-migrate` Gradle plugin, ships in v3.0.0-alpha08) auto-converts.

## What's protected

`cmp-worker-bc-test` runs the SAME test classes against:
- `v2BcTest` classpath: `worker-kmp:2.1.0` from Maven Central
- `v3BcTest` classpath: current in-development worker-kmp build

If test outputs diverge between the two classpaths, CI fails. This guarantees v2.x consumer code continues working against the v3.x library.

## Per-deprecation tests

Each `@Deprecated` symbol in v3+ MUST have a paired BC test in `cmp-worker-bc-test/src/test/`. The `:cmp-worker-bc-test:checkDeprecatedCoverage` Gradle task enforces this. The task is currently trivial (v2.1.0 baseline has no `@Deprecated` symbols); it gains teeth at v3.0.0-alpha00 (Phase 0) when `initializeWorkerXxx()` becomes deprecated.

## Running BC tests locally

```bash
./gradlew :cmp-worker-bc-test:bcTestAll       # runs both v2 + v3
bash scripts/bc-test-diff.sh                  # CI parity (with divergence diff)
```

## Filing a BC break

If you discover a BC regression: use [`.github/ISSUE_TEMPLATE/bc-break.md`](.github/ISSUE_TEMPLATE/bc-break.md). Auto-labels `bc-break` + `priority/critical`.

## Bypassing the gate

If a PR legitimately changes BC behavior (e.g. major version bump or explicit removal of a v2 deprecation), add label `skip-bc-test` to the PR. Use sparingly — every such bypass should be discussed in the PR description.

## Cross-references

- Phase 13 sub-plan: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/13-backward-compat-test-suite.md`
- Migration toolkit: ships v3.0.0-alpha08 (Phase 12). See `cmp-worker-migrate` Gradle plugin.
- Master plan: `plan-layer/project-plans/mbs/worker-kmp/active/worker-kmp-v3-foreground-storeflow/PLAN.md`
