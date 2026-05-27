package io.github.mobilebytelabs.worker.migrate

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Gradle plugin that scans v2.x worker-kmp consumer source and emits unified diffs
 * for migration to v3 API shape.
 *
 * Added in v3.0.0-alpha08 (Phase 12 of v3.0.0 epic).
 *
 * Tasks registered:
 * - `cmpWorkerMigrateCheck` — reports findings as Markdown; no changes
 * - `cmpWorkerMigrateApply` — applies HIGH-confidence diffs; emits MEDIUM/LOW to migrate-suggestions.md
 *
 * Current state (alpha08): SCAFFOLD ONLY. Tasks register + emit "no findings"
 * placeholder. Real Kotlin compiler frontend integration + V2PatternDetector +
 * UnifiedDiffGenerator + 5 integration fixtures land in v3.0.0-alpha08.X follow-ups.
 *
 * Consumer usage (when alpha08.X fully ships):
 * ```kotlin
 * plugins {
 *     id("io.github.mobilebytelabs.worker.migrate") version "3.0.0-alpha08" apply false
 * }
 * // Then:
 * //   ./gradlew cmpWorkerMigrateCheck      — report findings, no changes
 * //   ./gradlew cmpWorkerMigrateApply       — apply HIGH-confidence diffs
 * ```
 */
public class WorkerMigratePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("cmpWorkerMigrateCheck", CmpWorkerMigrateCheckTask::class.java) {
            it.group = "worker-kmp"
            it.description = "Scan source for v2 → v3 migration findings"
        }
        project.tasks.register("cmpWorkerMigrateApply", CmpWorkerMigrateApplyTask::class.java) {
            it.group = "worker-kmp"
            it.description = "Apply HIGH-confidence v2 → v3 migration diffs"
        }
    }
}

public abstract class CmpWorkerMigrateCheckTask @Inject constructor() : DefaultTask() {
    @TaskAction
    public fun check() {
        logger.lifecycle("=".repeat(60))
        logger.lifecycle("worker-kmp v2 → v3 migration findings")
        logger.lifecycle("=".repeat(60))
        logger.lifecycle("")
        logger.lifecycle("alpha08 scaffold — no findings reported.")
        logger.lifecycle("")
        logger.lifecycle("Real scanning (V2PatternDetector via Kotlin compiler frontend +")
        logger.lifecycle("confidence classification + unified-diff generation) lands in")
        logger.lifecycle("v3.0.0-alpha08.X per Phase 12 sub-plan Tier B-D.")
        logger.lifecycle("")
        logger.lifecycle("Until then, see MIGRATION_FROM_2_x.md for the manual migration guide.")
    }
}

public abstract class CmpWorkerMigrateApplyTask @Inject constructor() : DefaultTask() {
    @TaskAction
    public fun apply() {
        logger.lifecycle("alpha08 scaffold — no diffs to apply. See MIGRATION_FROM_2_x.md.")
    }
}
