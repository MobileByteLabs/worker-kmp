# cmp-worker-migrate

> Gradle plugin that scans v2.x worker-kmp consumer source and emits unified
> diffs for migration to v3 API shape. Lands at v3.0.0-alpha08.

## Current state (alpha08 scaffold)

- ✓ Gradle plugin registration (`io.github.mobilebytelabs.worker.migrate`)
- ✓ Two tasks: `cmpWorkerMigrateCheck` + `cmpWorkerMigrateApply` (both log placeholders)
- ✓ Smoke tests (2/2 GREEN — ProjectBuilder.builder() + plugins.apply)
- 🕒 V2PatternDetector via Kotlin compiler frontend (kotlin-compiler-embeddable) — alpha08.X
- 🕒 UnifiedDiffGenerator with confidence classification (HIGH/MEDIUM/LOW) — alpha08.X
- 🕒 5 integration fixtures (v2-android-app, v2-multiplatform-app, v2-koin-already,
    v2-hilt-app, v2-custom-factory) — alpha08.X
- 🕒 Idempotency tests — alpha08.X
- 🕒 Gradle Plugin Portal publishing — alpha08.X
- 🕒 ≤10-min migration time verification on real ≥30-worker codebase — alpha08.X

## Coordinates

`io.github.mobilebytelabs:worker-migrate:3.0.0-alpha08` (Maven Central + Gradle Plugin Portal — Portal listing lands in alpha08.X).

## Consumer usage (when alpha08.X fully ships)

```kotlin
// settings.gradle.kts
plugins {
    id("io.github.mobilebytelabs.worker.migrate") version "3.0.0-alpha08" apply false
}

// Then from terminal:
./gradlew cmpWorkerMigrateCheck      // report findings
./gradlew cmpWorkerMigrateApply       // apply HIGH-confidence diffs
```

## See also

- Phase 12 sub-plan: `plan-layer/.../12-migration-toolkit.md`
- MIGRATION_FROM_2_x.md — manual migration guide (always works regardless of plugin state)
