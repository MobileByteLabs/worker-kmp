package io.github.mobilebytelabs.worker.app.gradle

import io.github.mobilebytelabs.worker.app.gradle.codegen.AndroidLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.DesktopLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.IosLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.WebLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.XcodegenRunner
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import java.io.File

/**
 * Gradle plugin entry for worker-kmp-app-plugin.
 *
 * Apply via:
 *   plugins { id("io.github.mobilebytelabs.worker-app") version "X" }
 *
 * Responsibilities (per worker-kmp-app-plugin epic Phases 03-06):
 *  - Validate KMP plugin is applied first (else this plugin has nothing to wire).
 *  - Apply KSP transitively + add the annotations + processor deps.
 *  - Register `workerKmpApp { … }` extension DSL.
 *  - Register 4 per-platform codegen tasks (one task per platform) + an
 *    xcodegen-generate task that materializes iosApp.xcodeproj.
 *  - Wire generated source dirs into the consumer's KMP source sets.
 */
public class WorkerKmpAppPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        check(plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            "io.github.mobilebytelabs.worker-app requires kotlin-multiplatform plugin to be applied first"
        }
        // Apply KSP (consumer doesn't have to)
        plugins.apply("com.google.devtools.ksp")

        // Add transitive deps: annotations into commonMain; processor into kspCommonMainMetadata.
        // In-monorepo detection — if the worker-kmp project tree includes the annotations
        // + ksp modules (i.e. the sample inside worker-kmp itself), use project() refs to
        // avoid the mavenLocal-publish prerequisite. External consumers fall back to
        // resolving the published Maven coordinates.
        val workerVersion = providers.gradleProperty("worker.version").orNull
            ?: project.version.toString()
        val annotationsProject = rootProject.findProject(":cmp-worker-app-annotations")
        val kspProject = rootProject.findProject(":cmp-worker-app-ksp")
        val annotationsDep: Any = annotationsProject
            ?: "io.github.mobilebytelabs:worker-app-annotations:$workerVersion"
        val kspDep: Any = kspProject
            ?: "io.github.mobilebytelabs:worker-app-ksp:$workerVersion"
        dependencies.add("commonMainImplementation", annotationsDep)
        // KSP processor — `kspCommonMainMetadata` runs on commonMain so annotations
        // are processed once regardless of consumer's target matrix.
        runCatching {
            dependencies.add("kspCommonMainMetadata", kspDep)
        }

        val ext = extensions.create(EXT_NAME, WorkerKmpAppExtension::class.java).apply {
            androidGenerator.convention(true)
            desktopGenerator.convention(true)
            iosGenerator.convention(true)
            webGenerator.convention(true)
            xcodegenPath.convention("xcodegen")
            wasmJsBundleName.convention(project.name)
        }

        // Generated dirs root — wired into source sets below so generated files
        // participate in compilation transparently.
        val generatedRoot = layout.buildDirectory.dir("generated/worker-kmp-app")

        // Wire generated source dirs into KMP source sets (those that exist).
        wireKmpSourceSet("androidMain", generatedRoot.get().dir("androidMain/kotlin").asFile)
        wireKmpSourceSet("desktopMain", generatedRoot.get().dir("desktopMain/kotlin").asFile)
        wireKmpSourceSet("jvmMain", generatedRoot.get().dir("jvmMain/kotlin").asFile)
        wireKmpSourceSet("iosMain", generatedRoot.get().dir("iosMain/kotlin").asFile)
        wireKmpSourceSet("wasmJsMain", generatedRoot.get().dir("wasmJsMain/kotlin").asFile)

        // ── Codegen tasks ──────────────────────────────────────────────────────
        // KSP processor must run before any codegen reads codegen-model.json.
        // The metadata variant runs on commonMain so annotations are processed once
        // regardless of the consumer's target matrix. Codegen tasks dependOn this
        // and each platform's `compile*` task in turn dependsOn the matching codegen.
        val kspTask = "kspCommonMainKotlinMetadata"

        tasks.register(TASK_ANDROID) {
            group = TASK_GROUP
            description = "Codegens Android Application + Activity + AndroidManifest.xml"
            dependsOn(kspTask)
            // Tasks read codegen-model.json + scan source-set dirs at execution time
            // (data isn't known until KSP runs + user's kotlin{} block resolves).
            // Both are fundamentally Project-backed lookups → opt out of config cache
            // for these tasks specifically. Rest of the build still caches.
            notCompatibleWithConfigurationCache(
                "worker-app codegen reads codegen-model.json + source-set dirs at execution time",
            )
            doLast {
                if (!ext.androidGenerator.get()) {
                    logger.lifecycle("worker-kmp-app: androidGenerator disabled — skipping")
                    return@doLast
                }
                if (PreexistingLauncherDetector.warnIfFound(
                        target,
                        "androidMain",
                        listOf("Application\\.kt", "MainActivity\\.kt"),
                    )
                ) {
                    return@doLast
                }
                val model = target.requireModel()
                AndroidLauncherGenerator.run(
                    model = model,
                    outputDir = generatedRoot.get().dir("androidMain").asFile,
                )
                logger.lifecycle("worker-kmp-app: Android codegen done")
            }
        }
        tasks.register(TASK_DESKTOP) {
            group = TASK_GROUP
            description = "Codegens jvmMain/desktopMain fun main()"
            dependsOn(kspTask)
            // Tasks read codegen-model.json + scan source-set dirs at execution time
            // (data isn't known until KSP runs + user's kotlin{} block resolves).
            // Both are fundamentally Project-backed lookups → opt out of config cache
            // for these tasks specifically. Rest of the build still caches.
            notCompatibleWithConfigurationCache(
                "worker-app codegen reads codegen-model.json + source-set dirs at execution time",
            )
            doLast {
                if (!ext.desktopGenerator.get()) {
                    logger.lifecycle("worker-kmp-app: desktopGenerator disabled — skipping")
                    return@doLast
                }
                val sourceSet = if (target.kotlinSourceSetExists("desktopMain")) "desktopMain" else "jvmMain"
                if (PreexistingLauncherDetector.warnIfFound(target, sourceSet, listOf("Main\\.kt"))) return@doLast
                val model = target.requireModel()
                DesktopLauncherGenerator.run(
                    model = model,
                    outputDir = generatedRoot.get().dir(sourceSet).asFile,
                )
                logger.lifecycle("worker-kmp-app: Desktop codegen done (target=$sourceSet)")
            }
        }
        tasks.register(TASK_IOS) {
            group = TASK_GROUP
            description = "Codegens iosMain MainViewController + iosApp xcodegen spec + Swift wrappers"
            dependsOn(kspTask)
            // Tasks read codegen-model.json + scan source-set dirs at execution time
            // (data isn't known until KSP runs + user's kotlin{} block resolves).
            // Both are fundamentally Project-backed lookups → opt out of config cache
            // for these tasks specifically. Rest of the build still caches.
            notCompatibleWithConfigurationCache(
                "worker-app codegen reads codegen-model.json + source-set dirs at execution time",
            )
            doLast {
                if (!ext.iosGenerator.get()) {
                    logger.lifecycle("worker-kmp-app: iosGenerator disabled — skipping")
                    return@doLast
                }
                if (PreexistingLauncherDetector.warnIfFound(
                        target,
                        "iosMain",
                        listOf("MainViewController\\.kt"),
                    )
                ) {
                    return@doLast
                }
                val model = target.requireModel()
                IosLauncherGenerator.run(
                    model = model,
                    kotlinOutputDir = generatedRoot.get().dir("iosMain").asFile,
                    iosAppDir = layout.projectDirectory.dir("iosApp").asFile,
                )
                logger.lifecycle("worker-kmp-app: iOS codegen done")
            }
        }
        tasks.register(TASK_WEB) {
            group = TASK_GROUP
            description = "Codegens wasmJsMain fun main() + resources/index.html"
            dependsOn(kspTask)
            // Tasks read codegen-model.json + scan source-set dirs at execution time
            // (data isn't known until KSP runs + user's kotlin{} block resolves).
            // Both are fundamentally Project-backed lookups → opt out of config cache
            // for these tasks specifically. Rest of the build still caches.
            notCompatibleWithConfigurationCache(
                "worker-app codegen reads codegen-model.json + source-set dirs at execution time",
            )
            doLast {
                if (!ext.webGenerator.get()) {
                    logger.lifecycle("worker-kmp-app: webGenerator disabled — skipping")
                    return@doLast
                }
                if (PreexistingLauncherDetector.warnIfFound(target, "wasmJsMain", listOf("Main\\.kt"))) return@doLast
                val model = target.requireModel()
                WebLauncherGenerator.run(
                    model = model,
                    outputDir = generatedRoot.get().dir("wasmJsMain").asFile,
                    wasmJsBundleName = ext.wasmJsBundleName.get(),
                )
                logger.lifecycle("worker-kmp-app: Web (wasmJs) codegen done")
            }
        }
        // Convenience aggregator — runs all 4 codegens.
        tasks.register(TASK_ALL) {
            group = TASK_GROUP
            description = "Runs all enabled per-platform codegen tasks"
            dependsOn(TASK_ANDROID, TASK_DESKTOP, TASK_IOS, TASK_WEB)
        }
        // Auto-wire compile tasks → codegen → KSP so the consumer never needs to
        // invoke codegen manually. Per-target matchers because the compile-task
        // name depends on which targets the consumer declared.
        tasks.matching { it.name.startsWith("compileKotlinJvm") || it.name.startsWith("compileKotlinDesktop") }
            .configureEach { dependsOn(TASK_DESKTOP) }
        tasks.matching { it.name.startsWith("compileKotlinIos") }
            .configureEach { dependsOn(TASK_IOS) }
        tasks.matching { it.name.startsWith("compileKotlinWasmJs") || it.name.startsWith("compileKotlinJs") }
            .configureEach { dependsOn(TASK_WEB) }
        tasks.matching {
            it.name.startsWith("compileKotlinAndroid") ||
                it.name.matches(Regex("compile[A-Z].*KotlinAndroid"))
        }
            .configureEach { dependsOn(TASK_ANDROID) }
        // AndroidManifest merge wiring is left to consumer-side AGP convention
        // (point manifest at build/generated/worker-kmp-app/androidMain/AndroidManifest.xml).

        // xcodegen materialization — depends on iOS codegen producing project.yml.
        tasks.register(TASK_XCODEGEN) {
            group = TASK_GROUP
            description = "Materializes iosApp.xcodeproj via xcodegen (depends on $TASK_IOS)"
            dependsOn(TASK_IOS)
            notCompatibleWithConfigurationCache(
                "xcodegen runner shells out to user PATH at execution time",
            )
            doLast {
                if (!ext.iosGenerator.get()) {
                    logger.lifecycle("worker-kmp-app: iosGenerator disabled — skipping xcodegen")
                    return@doLast
                }
                val iosAppDir = layout.projectDirectory.dir("iosApp").asFile
                if (!iosAppDir.exists()) {
                    logger.warn("worker-kmp-app: $iosAppDir not found; iOS codegen may have been skipped")
                    return@doLast
                }
                val exit = XcodegenRunner.run(iosAppDir, ext.xcodegenPath.get())
                check(exit == 0) { "worker-kmp-app: xcodegen exited with code $exit" }
            }
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun Project.requireModel(): CodegenModel = CodegenModelLoader.load(this)
        ?: error(
            "worker-kmp-app: codegen-model.json not found in build/generated/ksp/... — " +
                "ensure KSP processor ran (run :kspCommonMainMetadata first or invoke codegen as part of a build).",
        )

    /**
     * Adds [generatedDir] as an extra src dir on `[sourceSetName].kotlin` if the
     * source set exists. Uses reflection so we don't need a hard compile-time
     * dep on kotlin-gradle-plugin's KotlinSourceSet type.
     */
    private fun Project.wireKmpSourceSet(sourceSetName: String, generatedDir: File) {
        val kotlinExt = extensions.findByName("kotlin") ?: return
        runCatching {
            @Suppress("UNCHECKED_CAST")
            val sourceSets = kotlinExt.javaClass.getMethod("getSourceSets")
                .invoke(kotlinExt) as? NamedDomainObjectCollection<Any>
                ?: return@runCatching
            val sourceSet = sourceSets.findByName(sourceSetName) ?: return@runCatching
            val kotlin = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet) ?: return@runCatching
            kotlin.javaClass.getMethod("srcDir", Any::class.java).invoke(kotlin, generatedDir)
        }
    }

    private fun Project.kotlinSourceSetExists(name: String): Boolean {
        val kotlinExt = extensions.findByName("kotlin") ?: return false
        return runCatching {
            val sourceSets = kotlinExt.javaClass.getMethod("getSourceSets")
                .invoke(kotlinExt) as? NamedDomainObjectCollection<*>
            sourceSets?.findByName(name) != null
        }.getOrDefault(false)
    }

    private companion object {
        const val EXT_NAME = "workerKmpApp"
        const val TASK_GROUP = "worker-kmp-app"
        const val TASK_ANDROID = "workerKmpAppCodegenAndroid"
        const val TASK_DESKTOP = "workerKmpAppCodegenDesktop"
        const val TASK_IOS = "workerKmpAppCodegenIos"
        const val TASK_WEB = "workerKmpAppCodegenWeb"
        const val TASK_ALL = "workerKmpAppCodegenAll"
        const val TASK_XCODEGEN = "workerKmpAppXcodegenGenerate"

        @Suppress("unused")
        private fun unusedSourceSetImport(): SourceSet? = null
    }
}
