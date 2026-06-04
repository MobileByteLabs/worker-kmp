package io.github.mobilebytelabs.worker.app.gradle

import io.github.mobilebytelabs.worker.app.gradle.codegen.AndroidLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.AutoShimGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.DesktopLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.IosLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.WebLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.WorkerInitGenerator
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
        // commonMain wires the codegen-emitted WorkerKmpAuto.kt (expect) per AC-49.
        wireKmpSourceSet("commonMain", generatedRoot.get().dir("commonMain/kotlin").asFile)
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
                // worker-kmp-single-api-completion sub-plan 04 — emit worker registry
                // Generated_WorkerKmpInit.kt + matching WorkerKmpAuto actual into androidMain.
                WorkerInitGenerator.run(
                    model = model,
                    platform = WorkerInitGenerator.Platform.Android,
                    outputDir = generatedRoot.get().asFile,
                )
                AutoShimGenerator.runPlatformActual(
                    model = model,
                    platform = AutoShimGenerator.Platform.Android,
                    outputDir = generatedRoot.get().asFile,
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
                // worker-kmp-single-api-completion sub-plan 04 — emit worker registry
                // Generated_WorkerKmpInit.kt + matching WorkerKmpAuto actual into desktopMain.
                WorkerInitGenerator.run(
                    model = model,
                    platform = WorkerInitGenerator.Platform.Desktop,
                    outputDir = generatedRoot.get().asFile,
                )
                AutoShimGenerator.runPlatformActual(
                    model = model,
                    platform = AutoShimGenerator.Platform.Desktop,
                    outputDir = generatedRoot.get().asFile,
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
                // worker-kmp-single-api-completion sub-plan 04 — emit worker registry
                // Generated_WorkerKmpInit.kt + matching WorkerKmpAuto actual into iosMain.
                WorkerInitGenerator.run(
                    model = model,
                    platform = WorkerInitGenerator.Platform.Ios,
                    outputDir = generatedRoot.get().asFile,
                )
                AutoShimGenerator.runPlatformActual(
                    model = model,
                    platform = AutoShimGenerator.Platform.Ios,
                    outputDir = generatedRoot.get().asFile,
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
                // worker-kmp-single-api-completion sub-plan 04 — emit worker registry
                // Generated_WorkerKmpInit.kt + matching WorkerKmpAuto actual into wasmJsMain.
                WorkerInitGenerator.run(
                    model = model,
                    platform = WorkerInitGenerator.Platform.Web,
                    outputDir = generatedRoot.get().asFile,
                )
                AutoShimGenerator.runPlatformActual(
                    model = model,
                    platform = AutoShimGenerator.Platform.Web,
                    outputDir = generatedRoot.get().asFile,
                )
                logger.lifecycle("worker-kmp-app: Web (wasmJs) codegen done")
            }
        }
        // worker-kmp-single-api-completion sub-plan 04 — emit the commonMain WorkerKmpAuto.kt
        // expect declaration + 4 platform actuals dispatching to `installWorkerKmp{Platform}`.
        // Per AC-49 — emitted into the consumer app module `build/generated/...`, NOT into
        // any published worker-kmp module. Per AC-21/D10 — no-arg `install()`.
        tasks.register(TASK_AUTO_SHIM) {
            group = TASK_GROUP
            description = "Codegens commonMain WorkerKmpAuto.kt (expect) + 4 platform actuals"
            dependsOn(kspTask)
            notCompatibleWithConfigurationCache(
                "worker-app codegen reads codegen-model.json + source-set dirs at execution time",
            )
            doLast {
                val model = target.requireModel()
                // commonMain expect only — the platform actuals are emitted by each
                // per-platform codegen task (Android/Desktop/iOS/Web) to ensure the actual
                // only exists when the matching installWorkerKmp{Platform} function exists.
                AutoShimGenerator.runCommon(model = model, outputDir = generatedRoot.get().asFile)
                logger.lifecycle("worker-kmp-app: WorkerKmpAuto.kt commonMain expect codegen done")
            }
        }
        // The platform-init generators piggyback on the per-platform codegen tasks above
        // (Android/Desktop/iOS/Web each call `WorkerInitGenerator.run` after the existing
        // `*LauncherGenerator.run`). AutoShimGenerator is its own task because it emits to
        // 5 source sets at once (commonMain expect + 4 actuals) and doesn't fit either of
        // the existing per-platform task contracts.
        // Auto-wire AutoShim into compile chain — emitted commonMain WorkerKmpAuto.kt
        // is referenced by consumer's per-platform code, so it must exist before
        // compileKotlinMetadata (which compiles commonMain).
        tasks.matching { it.name == "compileKotlinMetadata" || it.name.startsWith("compileCommon") }
            .configureEach { dependsOn(TASK_AUTO_SHIM) }
        // Also depend on per-platform compile tasks so the platform-specific actuals
        // (WorkerKmpAuto.android.kt etc.) exist before their compile.
        tasks.matching { it.name.startsWith("compileKotlinJvm") || it.name.startsWith("compileKotlinDesktop") }
            .configureEach { dependsOn(TASK_AUTO_SHIM) }
        tasks.matching { it.name.startsWith("compileKotlinIos") }
            .configureEach { dependsOn(TASK_AUTO_SHIM) }
        tasks.matching { it.name.startsWith("compileKotlinWasmJs") || it.name.startsWith("compileKotlinJs") }
            .configureEach { dependsOn(TASK_AUTO_SHIM) }
        tasks.matching {
            it.name.startsWith("compileKotlinAndroid") ||
                it.name.matches(Regex("compile[A-Z].*KotlinAndroid"))
        }
            .configureEach { dependsOn(TASK_AUTO_SHIM) }

        // Convenience aggregator — runs all 4 codegens + the auto-shim.
        tasks.register(TASK_ALL) {
            group = TASK_GROUP
            description = "Runs all enabled per-platform codegen tasks + WorkerKmpAuto shim"
            dependsOn(TASK_ANDROID, TASK_DESKTOP, TASK_IOS, TASK_WEB, TASK_AUTO_SHIM)
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
     * Adds [generatedDir] as an extra src dir on `[sourceSetName].kotlin` REACTIVELY —
     * registers a `matching { name == X }.configureEach { ... }` listener so the wiring
     * fires whenever the source set is created, including LATER in the build lifecycle
     * (e.g. after `applyDefaultHierarchyTemplate()` materializes `iosMain` between
     * `iosArm64()` + `iosSimulatorArm64()` targets, which happens AFTER our plugin's
     * apply phase). Without the reactive listener, eagerly looking up `iosMain` at
     * apply time silently returns null + the generated `iosMain/WorkerKmpAuto.kt`
     * actual never gets attached to its source set — surfacing as
     * `Expected WorkerKmpAuto has no actual declaration for Native` on iOS compile.
     *
     * Uses reflection so we don't need a hard compile-time dep on kotlin-gradle-plugin's
     * KotlinSourceSet type.
     */
    private fun Project.wireKmpSourceSet(sourceSetName: String, generatedDir: File) {
        val kotlinExt = extensions.findByName("kotlin") ?: return
        runCatching {
            @Suppress("UNCHECKED_CAST")
            val sourceSets = kotlinExt.javaClass.getMethod("getSourceSets")
                .invoke(kotlinExt) as? org.gradle.api.NamedDomainObjectCollection<Any>
                ?: return@runCatching
            val attachSrcDir: (Any) -> Unit = attach@{ ss ->
                runCatching {
                    val kotlin = ss.javaClass.getMethod("getKotlin").invoke(ss) ?: return@attach
                    kotlin.javaClass.getMethod("srcDir", Any::class.java).invoke(kotlin, generatedDir)
                }
            }
            // 1) Wire now if the source set already exists.
            sourceSets.findByName(sourceSetName)?.let(attachSrcDir)
            // 2) Reactive: fires LATER when the source set is materialized by
            // applyDefaultHierarchyTemplate() / iosArm64() / wasmJs() etc. (which run AFTER
            // our plugin apply). Without this, generated `iosMain/WorkerKmpAuto.kt` actuals
            // never attach to the source set → "Expected ... has no actual ... for Native".
            //
            // Use reflective Action.invoke to avoid statically referencing KotlinSourceSet's type
            // (we'd otherwise need a hard dep on kotlin-gradle-plugin).
            val addedAction = object : org.gradle.api.Action<Any> {
                override fun execute(added: Any) {
                    runCatching {
                        val name = added.javaClass.getMethod("getName").invoke(added) as? String
                        if (name == sourceSetName) attachSrcDir(added)
                    }
                }
            }
            sourceSets.javaClass.getMethod("whenObjectAdded", org.gradle.api.Action::class.java)
                .invoke(sourceSets, addedAction)
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
        const val TASK_AUTO_SHIM = "workerKmpAppCodegenAutoShim"

        @Suppress("unused")
        private fun unusedSourceSetImport(): SourceSet? = null
    }
}
