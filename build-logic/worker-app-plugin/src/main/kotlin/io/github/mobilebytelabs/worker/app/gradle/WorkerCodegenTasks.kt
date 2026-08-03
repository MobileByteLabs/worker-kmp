package io.github.mobilebytelabs.worker.app.gradle

import io.github.mobilebytelabs.worker.app.gradle.codegen.AndroidLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.AutoShimGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.DesktopLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.IosLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.WebLauncherGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.WorkerInitGenerator
import io.github.mobilebytelabs.worker.app.gradle.codegen.XcodegenRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Configuration-cache-compatible codegen tasks for worker-kmp-app.
 *
 * These typed [DefaultTask] subclasses replace the old ad-hoc
 * `tasks.register(NAME){ notCompatibleWithConfigurationCache(...); doLast { …captures Project… } }`
 * shape. Every value the `@TaskAction` needs is captured at CONFIGURATION time as a lazy,
 * serializable Provider/Property/FileCollection — NO `Project` (or the `workerKmpApp`
 * extension) is referenced at execution time, so the consumer build REUSES the configuration
 * cache instead of discarding it on every `compile*` (these tasks are auto-wired into every
 * compile chain, so a single incompatible one previously poisoned the whole build's cache).
 *
 * Generated output is byte-identical to the previous implementation — the `@TaskAction`
 * bodies reproduce the original per-platform branching verbatim, only the *source* of each
 * value changed (task properties instead of `target`/`ext`).
 *
 * NOTE (intentional deviation): the model input is modelled as an `@InputFiles`
 * [ConfigurableFileCollection] of the canonical KSP output candidates rather than a single
 * `@InputFile RegularFileProperty`. `@InputFile` fails validation when the resolved file is
 * absent, but the "no @WorkerKmpApp/@WorkerKmpWorkers annotations in this module" path is a
 * *supported* no-op: KSP legitimately writes no `codegen-model.json`. `@InputFiles` tolerates
 * missing candidates (empty), preserving that no-op while staying CC-safe. The file is
 * produced by `kspCommonMainKotlinMetadata` (retained `dependsOn`), so it exists at execution
 * on every real build.
 */
internal abstract class AbstractWorkerCodegenTask : DefaultTask() {

    /**
     * Canonical KSP `codegen-model.json` output candidates (wired as lazy build-dir Providers
     * at config time). Optional — absent means "no annotations in this module" → no-op.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val codegenModel: ConfigurableFileCollection

    /** Consumer source-set dirs scanned by [PreexistingLauncherDetector] (resolved at config time). */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceSetDirs: ConfigurableFileCollection

    /**
     * `build/generated/worker-kmp-app` — root the generators write into. Deliberately NOT an
     * `@OutputDirectory`: every codegen task (per-platform + AutoShim + xcodegen) writes into this
     * SAME shared root (each into its own source-set sub-dir), so declaring it as a tracked output
     * makes Gradle flag a false "task X consumes task Y's output without a dependency" validation
     * error (Gradle 8+/9 strict). These tasks were never up-to-date-cacheable before this refactor
     * (ad-hoc `doLast`, no declared outputs) so `@Internal` restores that always-run behavior; it stays
     * configuration-cache compatible (the DirectoryProperty is captured + serialized at config time).
     * The compile→codegen ordering is carried by the plugin's `dependsOn` wiring, not by output tracking.
     */
    @get:Internal
    abstract val generatedRoot: DirectoryProperty

    /** The consumer project's Gradle path (e.g. `:sample`) — for lifecycle logging only. */
    @get:Input
    abstract val projectPath: Property<String>

    /** The consumer project dir — used only to render relative paths in detector warnings. */
    @get:Internal
    abstract val projectRootDir: DirectoryProperty

    /** First existing candidate model, or null when this module has no worker annotations. */
    protected fun loadModel(): CodegenModel? =
        codegenModel.files.firstOrNull { it.exists() }?.let { CodegenModelLoader.load(it) }

    protected fun logNoAnnotations(suffix: String = "skipping codegen") {
        logger.lifecycle(
            "worker-kmp-app: no @WorkerKmpApp/@WorkerKmpWorkers annotations in ${projectPath.get()} — $suffix",
        )
    }
}

/** Base for the 4 per-platform launcher codegen tasks (each honours its `*Generator` flag). */
internal abstract class AbstractWorkerPlatformCodegenTask : AbstractWorkerCodegenTask() {
    /** The matching `workerKmpApp.{platform}Generator` flag (default true). */
    @get:Input
    abstract val generatorEnabled: Property<Boolean>
}

internal abstract class WorkerCodegenAndroidTask : AbstractWorkerPlatformCodegenTask() {
    @TaskAction
    fun run() {
        val model = loadModel() ?: run {
            logNoAnnotations()
            return
        }
        val root = generatedRoot.get()
        if (!model.appGeneration) {
            WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Android, root.asFile)
            AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Android, root.asFile)
            logger.lifecycle("worker-kmp-app: Android worker registry (workers-only, no launcher) done")
            return
        }
        if (!generatorEnabled.get()) {
            logger.lifecycle("worker-kmp-app: androidGenerator disabled — skipping")
            return
        }
        if (PreexistingLauncherDetector.warnIfFound(
                srcDirs = sourceSetDirs.files.toList(),
                projectDir = projectRootDir.get().asFile,
                sourceSetName = "androidMain",
                filenamePatterns = listOf("Application\\.kt", "MainActivity\\.kt"),
                logger = logger,
            )
        ) {
            return
        }
        AndroidLauncherGenerator.run(model, root.dir("androidMain").asFile)
        WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Android, root.asFile)
        AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Android, root.asFile)
        logger.lifecycle("worker-kmp-app: Android codegen done")
    }
}

internal abstract class WorkerCodegenDesktopTask : AbstractWorkerPlatformCodegenTask() {
    /** `desktopMain` when the consumer declared `jvm("desktop")`, else `jvmMain` (resolved at config). */
    @get:Input
    abstract val desktopSourceSet: Property<String>

    @TaskAction
    fun run() {
        val model = loadModel() ?: run {
            logNoAnnotations()
            return
        }
        val root = generatedRoot.get()
        val sourceSet = desktopSourceSet.get()
        if (!model.appGeneration) {
            WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Desktop, root.asFile, sourceSet)
            AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Desktop, root.asFile, sourceSet)
            logger.lifecycle("worker-kmp-app: Desktop worker registry (workers-only) done (target=$sourceSet)")
            return
        }
        if (!generatorEnabled.get()) {
            logger.lifecycle("worker-kmp-app: desktopGenerator disabled — skipping")
            return
        }
        if (PreexistingLauncherDetector.warnIfFound(
                srcDirs = sourceSetDirs.files.toList(),
                projectDir = projectRootDir.get().asFile,
                sourceSetName = sourceSet,
                filenamePatterns = listOf("Main\\.kt"),
                logger = logger,
            )
        ) {
            return
        }
        DesktopLauncherGenerator.run(model, root.dir(sourceSet).asFile)
        WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Desktop, root.asFile, sourceSet)
        AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Desktop, root.asFile, sourceSet)
        logger.lifecycle("worker-kmp-app: Desktop codegen done (target=$sourceSet)")
    }
}

internal abstract class WorkerCodegenIosTask : AbstractWorkerPlatformCodegenTask() {
    /** The consumer's `iosApp/` dir — xcodegen spec + Swift wrappers land here. */
    @get:Internal
    abstract val iosAppDir: DirectoryProperty

    @TaskAction
    fun run() {
        val model = loadModel() ?: run {
            logNoAnnotations()
            return
        }
        val root = generatedRoot.get()
        if (!model.appGeneration) {
            WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Ios, root.asFile)
            AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Ios, root.asFile)
            logger.lifecycle("worker-kmp-app: iOS worker registry (workers-only, no launcher) done")
            return
        }
        if (!generatorEnabled.get()) {
            logger.lifecycle("worker-kmp-app: iosGenerator disabled — skipping")
            return
        }
        if (PreexistingLauncherDetector.warnIfFound(
                srcDirs = sourceSetDirs.files.toList(),
                projectDir = projectRootDir.get().asFile,
                sourceSetName = "iosMain",
                filenamePatterns = listOf("MainViewController\\.kt"),
                logger = logger,
            )
        ) {
            return
        }
        IosLauncherGenerator.run(
            model = model,
            kotlinOutputDir = root.dir("iosMain").asFile,
            iosAppDir = iosAppDir.get().asFile,
        )
        WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Ios, root.asFile)
        AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Ios, root.asFile)
        logger.lifecycle("worker-kmp-app: iOS codegen done")
    }
}

internal abstract class WorkerCodegenWebTask : AbstractWorkerPlatformCodegenTask() {
    /** wasmJs distribution bundle name (defaults to `project.name`). */
    @get:Input
    abstract val wasmJsBundleName: Property<String>

    @TaskAction
    fun run() {
        val model = loadModel() ?: run {
            logNoAnnotations()
            return
        }
        val root = generatedRoot.get()
        if (!model.appGeneration) {
            // Emit into BOTH web source sets so a js(IR) consumer also gets a JS actual.
            for (webSourceSet in WEB_SOURCE_SETS) {
                WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Web, root.asFile, webSourceSet)
                AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Web, root.asFile, webSourceSet)
            }
            logger.lifecycle("worker-kmp-app: Web worker registry (workers-only, no launcher) done")
            return
        }
        if (!generatorEnabled.get()) {
            logger.lifecycle("worker-kmp-app: webGenerator disabled — skipping")
            return
        }
        if (PreexistingLauncherDetector.warnIfFound(
                srcDirs = sourceSetDirs.files.toList(),
                projectDir = projectRootDir.get().asFile,
                sourceSetName = "wasmJsMain",
                filenamePatterns = listOf("Main\\.kt"),
                logger = logger,
            )
        ) {
            return
        }
        WebLauncherGenerator.run(model, root.dir("wasmJsMain").asFile, wasmJsBundleName.get())
        for (webSourceSet in WEB_SOURCE_SETS) {
            WorkerInitGenerator.run(model, WorkerInitGenerator.Platform.Web, root.asFile, webSourceSet)
            AutoShimGenerator.runPlatformActual(model, AutoShimGenerator.Platform.Web, root.asFile, webSourceSet)
        }
        logger.lifecycle("worker-kmp-app: Web (wasmJs + js) codegen done")
    }

    private companion object {
        val WEB_SOURCE_SETS = listOf("wasmJsMain", "jsMain")
    }
}

/** Emits the commonMain `WorkerKmpAuto.kt` expect (the 4 actuals come from the per-platform tasks). */
internal abstract class WorkerCodegenAutoShimTask : AbstractWorkerCodegenTask() {
    @TaskAction
    fun run() {
        val model = loadModel() ?: run {
            logNoAnnotations("skipping WorkerKmpAuto shim codegen")
            return
        }
        AutoShimGenerator.runCommon(model = model, outputDir = generatedRoot.get().asFile)
        logger.lifecycle("worker-kmp-app: WorkerKmpAuto.kt commonMain expect codegen done")
    }
}

/** Materializes `iosApp.xcodeproj` via xcodegen — shells out to PATH (CC-safe: no Project capture). */
internal abstract class WorkerXcodegenTask : AbstractWorkerCodegenTask() {
    @get:Input
    abstract val iosGeneratorEnabled: Property<Boolean>

    @get:Input
    abstract val xcodegenPath: Property<String>

    @get:Internal
    abstract val iosAppDir: DirectoryProperty

    @TaskAction
    fun run() {
        val model = loadModel() ?: run {
            logNoAnnotations()
            return
        }
        if (!model.appGeneration) {
            logger.lifecycle("worker-kmp-app: workers-only mode — skipping xcodegen (no generated iosApp)")
            return
        }
        if (!iosGeneratorEnabled.get()) {
            logger.lifecycle("worker-kmp-app: iosGenerator disabled — skipping xcodegen")
            return
        }
        val dir = iosAppDir.get().asFile
        if (!dir.exists()) {
            logger.warn("worker-kmp-app: $dir not found; iOS codegen may have been skipped")
            return
        }
        val exit = XcodegenRunner.run(dir, xcodegenPath.get())
        check(exit == 0) { "worker-kmp-app: xcodegen exited with code $exit" }
    }
}
