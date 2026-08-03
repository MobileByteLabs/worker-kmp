package io.github.mobilebytelabs.worker.app.gradle

import kotlinx.serialization.json.Json
import org.gradle.api.Project
import java.io.File

/**
 * Locates + decodes the `codegen-model.json` emitted by the KSP processor.
 *
 * KSP writes resources to a per-source-set directory. For KMP commonMain the
 * canonical path is `build/generated/ksp/metadata/commonMain/resources/...`.
 * For JVM-only consumer projects (rare for worker-kmp-app — usually KMP) the
 * path is `build/generated/ksp/main/resources/...`. We search both.
 */
internal object CodegenModelLoader {

    private const val MODEL_RELATIVE = "META-INF/worker-kmp-app/codegen-model.json"
    private const val FALLBACK_RELATIVE = "codegen-model.json"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun load(project: Project): CodegenModel? {
        val file = locate(project) ?: return null
        return load(file)
    }

    /**
     * Configuration-cache-safe overload: decode a model from an already-resolved [modelFile]
     * (the input file resolved at CONFIG time + read at execution time by the typed codegen
     * tasks). Returns null when the file is absent or empty — i.e. the KSP processor wrote no
     * `codegen-model.json` because this module has no `@WorkerKmpApp`/`@WorkerKmpWorkers`
     * annotations (ProcessingMode.NONE), in which case the caller no-ops.
     */
    fun load(modelFile: File): CodegenModel? {
        if (!modelFile.exists()) return null
        return json.decodeFromString(CodegenModel.serializer(), modelFile.readText())
    }

    /** Canonical KSP output locations for `codegen-model.json`, tried in order at execution time. */
    val MODEL_CANDIDATE_PATHS: List<String> = listOf(
        "generated/ksp/metadata/commonMain/resources/$MODEL_RELATIVE",
        "generated/ksp/metadata/commonMain/resources/$FALLBACK_RELATIVE",
        "generated/ksp/main/resources/$MODEL_RELATIVE",
        "generated/ksp/main/resources/$FALLBACK_RELATIVE",
    )

    fun locate(project: Project): File? {
        val candidates = listOf(
            project.layout.buildDirectory.file(
                "generated/ksp/metadata/commonMain/resources/$MODEL_RELATIVE",
            ).get().asFile,
            project.layout.buildDirectory.file(
                "generated/ksp/metadata/commonMain/resources/$FALLBACK_RELATIVE",
            ).get().asFile,
            project.layout.buildDirectory.file("generated/ksp/main/resources/$MODEL_RELATIVE").get().asFile,
            project.layout.buildDirectory.file("generated/ksp/main/resources/$FALLBACK_RELATIVE").get().asFile,
        )
        candidates.firstOrNull { it.exists() }?.let { return it }
        // Walk fallback — KSP output paths vary across versions
        val kspRoot = project.layout.buildDirectory.dir("generated/ksp").get().asFile
        if (!kspRoot.exists()) return null
        return kspRoot.walkTopDown().firstOrNull { it.name == "codegen-model.json" }
    }
}
