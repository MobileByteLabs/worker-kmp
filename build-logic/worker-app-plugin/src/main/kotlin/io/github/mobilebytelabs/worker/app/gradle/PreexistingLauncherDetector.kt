package io.github.mobilebytelabs.worker.app.gradle

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Scans a consumer's source set for files matching known launcher patterns.
 * If any match, the corresponding codegen task skips with a warning so the
 * consumer's hand-authored launcher takes precedence — opt-out escape hatch
 * per `worker-kmp-app-plugin` D9.
 */
internal object PreexistingLauncherDetector {

    /**
     * @return true if any of [filenamePatterns] (regex) match files inside the
     *   consumer's source-set directory (recursive). Logs a warning when a
     *   match is found — codegen for this source set is then skipped.
     */
    fun warnIfFound(project: Project, sourceSetName: String, filenamePatterns: List<String>): Boolean = warnIfFound(
        srcDirs = project.findKotlinSrcDirs(sourceSetName),
        projectDir = project.projectDir,
        sourceSetName = sourceSetName,
        filenamePatterns = filenamePatterns,
        logger = project.logger,
    )

    /**
     * Configuration-cache-safe overload — the caller resolves [srcDirs] (via
     * [Project.findKotlinSrcDirs]) + [projectDir] + [logger] at CONFIGURATION time and passes
     * only serializable values in, so the scan runs at execution time without capturing
     * `Project`. Behavior is identical to the `Project`-based overload.
     *
     * @return true if any of [filenamePatterns] (regex) match files inside [srcDirs]
     *   (recursive). Logs a warning when a match is found — codegen for this source set is
     *   then skipped.
     */
    fun warnIfFound(
        srcDirs: List<File>,
        projectDir: File,
        sourceSetName: String,
        filenamePatterns: List<String>,
        logger: Logger,
    ): Boolean {
        if (srcDirs.isEmpty()) return false
        val regexes = filenamePatterns.map { Regex(it) }
        val matches = srcDirs.flatMap { dir ->
            if (!dir.exists()) {
                emptyList()
            } else {
                dir.walkTopDown().filter { f -> f.isFile && regexes.any { it.matches(f.name) } }.toList()
            }
        }
        if (matches.isNotEmpty()) {
            logger.warn(
                "worker-kmp-app: skipping ${'$'}sourceSetName codegen — pre-existing launcher file(s) detected: " +
                    matches.joinToString { it.relativeTo(projectDir).path },
            )
            return true
        }
        return false
    }
}

/**
 * Resolves the `kotlin.srcDirs` for [sourceSetName] via reflection (no hard dep on
 * kotlin-gradle-plugin-api's KotlinSourceSet type). Called at CONFIGURATION time by the
 * typed codegen tasks so the resulting `List<File>` can be wired as a lazy task input
 * (a serializable `List<File>` — configuration-cache safe).
 *
 * Top-level so the plugin can call it directly on the consumer `Project` while wiring tasks.
 */
internal fun Project.findKotlinSrcDirs(sourceSetName: String): List<File> {
    val kotlinExt = extensions.findByName("kotlin") ?: return emptyList()
    return runCatching {
        val sourceSets = kotlinExt.javaClass.getMethod(
            "getSourceSets",
        ).invoke(kotlinExt) as? org.gradle.api.NamedDomainObjectCollection<*>
        val sourceSet = sourceSets?.findByName(sourceSetName) ?: return emptyList()
        val kotlin = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet)
        @Suppress("UNCHECKED_CAST")
        (kotlin.javaClass.getMethod("getSrcDirs").invoke(kotlin) as? Set<File>)?.toList().orEmpty()
    }.getOrElse { emptyList() }
}
