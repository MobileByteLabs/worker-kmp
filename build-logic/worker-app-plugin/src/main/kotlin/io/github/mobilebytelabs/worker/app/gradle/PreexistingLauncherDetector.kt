package io.github.mobilebytelabs.worker.app.gradle

import org.gradle.api.Project
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
    fun warnIfFound(project: Project, sourceSetName: String, filenamePatterns: List<String>): Boolean {
        val srcDirs = project.findKotlinSrcDirs(sourceSetName)
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
            project.logger.warn(
                "worker-kmp-app: skipping ${'$'}sourceSetName codegen — pre-existing launcher file(s) detected: " +
                    matches.joinToString { it.relativeTo(project.projectDir).path },
            )
            return true
        }
        return false
    }

    private fun Project.findKotlinSrcDirs(sourceSetName: String): List<File> {
        // Use reflection to walk KotlinProjectExtension → sourceSets[name] → kotlin.srcDirs.
        // Avoids a hard compile-time dep on kotlin-gradle-plugin-api's KotlinSourceSet type.
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
}
