package io.github.mobilebytelabs.worker.app.gradle

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.register
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
 *
 * Configuration-cache: every codegen task is a typed [AbstractWorkerCodegenTask] with lazy,
 * serializable inputs captured at CONFIGURATION time (see [WorkerCodegenTasks]). No task
 * references `Project`/the extension at execution time, so consumer builds REUSE the
 * configuration cache — these tasks are auto-wired into every `compile*` chain, so a single
 * `notCompatibleWithConfigurationCache` opt-out previously poisoned the whole build's cache.
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
        //
        // Version resolution (GitHub issue #51, bug 1): prefer an explicit `worker.version`
        // gradle property override, else the plugin's OWN version baked into a classpath
        // resource at plugin-build time. We MUST NOT fall back to `project.version` — that
        // is the *consumer's* project version, which is Gradle's literal "unspecified" for a
        // normal app project, producing `worker-app-ksp:unspecified` (the reported failure).
        // Only reach for the coordinate version when the modules aren't in the local tree.
        val annotationsProject = rootProject.findProject(":cmp-worker-app-annotations")
        val kspProject = rootProject.findProject(":cmp-worker-app-ksp")
        // Resolve the coordinate version lazily — only external consumers (no in-tree
        // project refs) need it, and only then do we want to fail loudly if it can't
        // be determined. Precedence: `worker.version` override → the plugin's own baked-in
        // version. NEVER the consumer's `project.version` (issue #51, bug 1).
        val workerVersion: String by lazy {
            WorkerVersionResolver.resolve(
                propertyOverride = providers.gradleProperty("worker.version").orNull,
                embedded = WorkerVersionResolver.loadEmbedded(),
            )
        }
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
        val generatedRootDir = layout.buildDirectory.dir("generated/worker-kmp-app")

        // Wire generated source dirs into KMP source sets (those that exist).
        // commonMain wires the codegen-emitted WorkerKmpAuto.kt (expect) per AC-49.
        wireKmpSourceSet("commonMain", generatedRootDir.get().dir("commonMain/kotlin").asFile)
        wireKmpSourceSet("androidMain", generatedRootDir.get().dir("androidMain/kotlin").asFile)
        wireKmpSourceSet("desktopMain", generatedRootDir.get().dir("desktopMain/kotlin").asFile)
        wireKmpSourceSet("jvmMain", generatedRootDir.get().dir("jvmMain/kotlin").asFile)
        wireKmpSourceSet("iosMain", generatedRootDir.get().dir("iosMain/kotlin").asFile)
        wireKmpSourceSet("wasmJsMain", generatedRootDir.get().dir("wasmJsMain/kotlin").asFile)
        // The "Web" codegen serves BOTH wasmJs and the plain js(IR) target — a consumer
        // that declares js() gets the same WorkerKmpAuto actual + worker registry emitted
        // into jsMain. Wiring is a no-op when the consumer has no js target.
        wireKmpSourceSet("jsMain", generatedRootDir.get().dir("jsMain/kotlin").asFile)

        // ── Config-time captures for the typed codegen tasks (all serializable → CC-safe) ──
        // The consumer project path (":", ":sample") for lifecycle logging — captured here
        // because inside a task-config lambda `path` would resolve to the TASK's path.
        val consumerProjectPath = path
        val consumerProjectDir = layout.projectDirectory
        val iosAppDirProvider = layout.projectDirectory.dir("iosApp")
        // Lazy Providers for the canonical KSP `codegen-model.json` output candidates — NO
        // filesystem read at config time (that would churn the config-cache fingerprint);
        // resolved at execution AFTER kspCommonMainKotlinMetadata via the retained dependsOn.
        val modelCandidateFiles = CodegenModelLoader.MODEL_CANDIDATE_PATHS
            .map { layout.buildDirectory.file(it) }
        // Desktop source set is jvmMain unless the consumer declared jvm("desktop"); resolved
        // once at config time (was computed at execution via the Project before).
        val desktopSourceSetName = if (kotlinSourceSetExists("desktopMain")) "desktopMain" else "jvmMain"

        // ── Codegen tasks ──────────────────────────────────────────────────────
        // KSP processor must run before any codegen reads codegen-model.json.
        // The metadata variant runs on commonMain so annotations are processed once
        // regardless of the consumer's target matrix. Codegen tasks dependOn this
        // and each platform's `compile*` task in turn dependsOn the matching codegen.
        val kspTask = "kspCommonMainKotlinMetadata"

        tasks.register<WorkerCodegenAndroidTask>(TASK_ANDROID) {
            group = TASK_GROUP
            description = "Codegens Android Application + Activity + AndroidManifest.xml"
            dependsOn(kspTask)
            codegenModel.from(modelCandidateFiles)
            sourceSetDirs.from(target.findKotlinSrcDirs("androidMain"))
            generatedRoot.set(generatedRootDir)
            projectPath.set(consumerProjectPath)
            projectRootDir.set(consumerProjectDir)
            generatorEnabled.set(ext.androidGenerator.get())
        }
        tasks.register<WorkerCodegenDesktopTask>(TASK_DESKTOP) {
            group = TASK_GROUP
            description = "Codegens jvmMain/desktopMain fun main()"
            dependsOn(kspTask)
            codegenModel.from(modelCandidateFiles)
            sourceSetDirs.from(target.findKotlinSrcDirs(desktopSourceSetName))
            generatedRoot.set(generatedRootDir)
            projectPath.set(consumerProjectPath)
            projectRootDir.set(consumerProjectDir)
            generatorEnabled.set(ext.desktopGenerator.get())
            desktopSourceSet.set(desktopSourceSetName)
        }
        tasks.register<WorkerCodegenIosTask>(TASK_IOS) {
            group = TASK_GROUP
            description = "Codegens iosMain MainViewController + iosApp xcodegen spec + Swift wrappers"
            dependsOn(kspTask)
            codegenModel.from(modelCandidateFiles)
            sourceSetDirs.from(target.findKotlinSrcDirs("iosMain"))
            generatedRoot.set(generatedRootDir)
            projectPath.set(consumerProjectPath)
            projectRootDir.set(consumerProjectDir)
            generatorEnabled.set(ext.iosGenerator.get())
            iosAppDir.set(iosAppDirProvider)
        }
        tasks.register<WorkerCodegenWebTask>(TASK_WEB) {
            group = TASK_GROUP
            description = "Codegens wasmJsMain fun main() + resources/index.html"
            dependsOn(kspTask)
            codegenModel.from(modelCandidateFiles)
            sourceSetDirs.from(target.findKotlinSrcDirs("wasmJsMain"))
            generatedRoot.set(generatedRootDir)
            projectPath.set(consumerProjectPath)
            projectRootDir.set(consumerProjectDir)
            generatorEnabled.set(ext.webGenerator.get())
            wasmJsBundleName.set(ext.wasmJsBundleName.get())
        }
        // worker-kmp-single-api-completion sub-plan 04 — emit the commonMain WorkerKmpAuto.kt
        // expect declaration + 4 platform actuals dispatching to `installWorkerKmp{Platform}`.
        // Per AC-49 — emitted into the consumer app module `build/generated/...`, NOT into
        // any published worker-kmp module. Per AC-21/D10 — no-arg `install()`.
        tasks.register<WorkerCodegenAutoShimTask>(TASK_AUTO_SHIM) {
            group = TASK_GROUP
            description = "Codegens commonMain WorkerKmpAuto.kt (expect) + 4 platform actuals"
            dependsOn(kspTask)
            codegenModel.from(modelCandidateFiles)
            generatedRoot.set(generatedRootDir)
            projectPath.set(consumerProjectPath)
            projectRootDir.set(consumerProjectDir)
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
                it.name.matches(Regex("compile[A-Z].*KotlinAndroid")) ||
                // AGP9 `com.android.kotlin.multiplatform.library` names the android
                // compilation `compileAndroidMain` (+ test variants) — no "Kotlin" token —
                // so the two patterns above miss it and the android WorkerKmpAuto actual
                // never gets generated ("Expected WorkerKmpAuto has no actual ... for JVM").
                it.name.startsWith("compileAndroid")
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
                it.name.matches(Regex("compile[A-Z].*KotlinAndroid")) ||
                // AGP9 `com.android.kotlin.multiplatform.library` names the android
                // compilation `compileAndroidMain` (+ test variants) — no "Kotlin" token —
                // so the two patterns above miss it and the android WorkerKmpAuto actual
                // never gets generated ("Expected WorkerKmpAuto has no actual ... for JVM").
                it.name.startsWith("compileAndroid")
        }
            .configureEach { dependsOn(TASK_ANDROID) }
        // AndroidManifest merge wiring is left to consumer-side AGP convention
        // (point manifest at build/generated/worker-kmp-app/androidMain/AndroidManifest.xml).

        // xcodegen materialization — depends on iOS codegen producing project.yml.
        tasks.register<WorkerXcodegenTask>(TASK_XCODEGEN) {
            group = TASK_GROUP
            description = "Materializes iosApp.xcodeproj via xcodegen (depends on $TASK_IOS)"
            dependsOn(TASK_IOS)
            codegenModel.from(modelCandidateFiles)
            generatedRoot.set(generatedRootDir)
            projectPath.set(consumerProjectPath)
            projectRootDir.set(consumerProjectDir)
            iosGeneratorEnabled.set(ext.iosGenerator.get())
            xcodegenPath.set(ext.xcodegenPath.get())
            iosAppDir.set(iosAppDirProvider)
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

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
