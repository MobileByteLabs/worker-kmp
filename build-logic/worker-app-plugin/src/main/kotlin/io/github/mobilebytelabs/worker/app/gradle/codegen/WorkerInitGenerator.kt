package io.github.mobilebytelabs.worker.app.gradle.codegen

import io.github.mobilebytelabs.worker.app.gradle.CodegenModel
import io.github.mobilebytelabs.worker.app.gradle.TemplateEngine
import io.github.mobilebytelabs.worker.app.gradle.WorkerDef
import java.io.File

/**
 * Renders the per-platform `Generated_WorkerKmpInit.kt` file from the matching
 * `workerkmp-init-{platform}.kt.template` resource.
 *
 * Closes audit gaps G1 (per-platform factory selection auto-generated) + G3/G4/G5
 * (codegen-emitted host-init wiring). Per D28 — output is deterministic across builds:
 * worker list pre-sorted by FQN by KSP processor; imports sorted alphabetically here.
 *
 * Per AC-25 — per-worker `@WorkerForPlatforms` filter is honored:
 * workers with non-empty `platformsFilter` are emitted only on listed platforms.
 *
 * Per D29/NG12/AC-60 — workers with default-valued primary-ctor params have the
 * defaults skipped from `koinDeps` by the KSP processor; the generated `register<…>`
 * block uses the worker's defaults via `Worker(context = ctx)` (no `getKoin().get<…>`).
 */
internal object WorkerInitGenerator {

    /** All 4 platform identifiers — must match `@WorkerForPlatforms` enum names. */
    enum class Platform(val templateSuffix: String, val outputSourceSet: String) {
        Android("android", "androidMain"),
        Ios("ios", "iosMain"),
        Desktop("desktop", "desktopMain"),
        Web("web", "wasmJsMain"),
    }

    /**
     * Renders `Generated_WorkerKmpInit.kt` into `{outputDir}/{outputSourceSet}/kotlin/{pkg}/generated/`.
     *
     * [sourceSetOverride] lets the caller supply a runtime-detected source set name (e.g.
     * `"jvmMain"` when the consumer declared `jvm { }` instead of `jvm("desktop") { }`).
     */
    fun run(model: CodegenModel, platform: Platform, outputDir: File, sourceSetOverride: String? = null) {
        val pkgPath = model.packageName.replace('.', '/')

        // Filter workers for this platform (per @WorkerForPlatforms / AC-25).
        val workersForPlatform = model.workers.filter { it.isOnPlatform(platform.name) }

        // Render imports — alphabetical FQN order for determinism (D28).
        val importLines = workersForPlatform
            .map { "import ${it.fqn}" }
            .distinct()
            .sorted()
            .joinToString(separator = "\n")

        // Render registrations — workers come pre-sorted by FQN from KSP processor.
        val registrationKey = "workerRegistrationsFor${platform.name}"
        val registrations = workersForPlatform.joinToString(separator = "\n") { worker ->
            renderRegistration(worker)
        }

        val template = TemplateEngine.load("workerkmp-init-${platform.templateSuffix}.kt.template")
        val rendered = TemplateEngine.render(
            template = template,
            model = model,
            extras = mapOf(
                "workerImports" to importLines,
                registrationKey to registrations,
            ),
        )

        val effectiveSourceSet = sourceSetOverride ?: platform.outputSourceSet
        outputDir
            .resolve("$effectiveSourceSet/kotlin/$pkgPath/generated/Generated_WorkerKmpInit.kt")
            .apply { parentFile.mkdirs() }
            .writeText(rendered)
    }

    /**
     * Emits one `register<WorkerFqn> { ctx -> WorkerFqn(context = ctx, dep1 = getKoin().get(...), …) }` line.
     *
     * Constructor params are emitted in source order (NOT alphabetical — constructor order
     * is meaningful). Default-valued params are SKIPPED (per D29 — KSP processor strips them
     * from `koinDeps`).
     */
    private fun renderRegistration(worker: WorkerDef): String {
        val argList = buildList {
            add("context = ctx")
            worker.koinDeps.forEach { dep ->
                if (dep.namedQualifier != null) {
                    add(
                        "${dep.paramName} = getKoin().get(qualifier = org.koin.core.qualifier.named(\"${dep.namedQualifier}\"))",
                    )
                } else {
                    add("${dep.paramName} = getKoin().get()")
                }
            }
        }.joinToString(separator = ",\n                ")

        return """        register<${worker.fqn}> { ctx ->
            ${worker.fqn}(
                $argList,
            )
        }"""
    }
}
