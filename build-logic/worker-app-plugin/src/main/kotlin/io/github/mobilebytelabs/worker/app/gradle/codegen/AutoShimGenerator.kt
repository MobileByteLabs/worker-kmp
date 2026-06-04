package io.github.mobilebytelabs.worker.app.gradle.codegen

import io.github.mobilebytelabs.worker.app.gradle.CodegenModel
import io.github.mobilebytelabs.worker.app.gradle.TemplateEngine
import java.io.File

/**
 * Renders `WorkerKmpAuto.kt` — the consumer-facing single-API entry point.
 *
 * **Common expect** is emitted by [runCommon] (called from `workerKmpAppCodegenAutoShim` task).
 * **Per-platform actuals** are emitted by [runPlatformActual] (called from each platform's
 * codegen task right after `WorkerInitGenerator.run`). This way the actual only exists when
 * the matching `installWorkerKmp{Platform}` function it dispatches to also exists — emitting
 * an unmatched actual to a source set whose Init wasn't generated produces compile errors
 * ("Expected WorkerKmpAuto has no actual declaration in module commonMain for {target}").
 *
 * Per AC-49 — emitted into the consumer's app module `build/generated/...`, NOT into any
 * published worker-kmp module.
 *
 * Per AC-21/D10 — no-arg `install()`.
 */
internal object AutoShimGenerator {

    /** Emits commonMain `WorkerKmpAuto.kt` with the `expect object WorkerKmpAuto { fun install() }`. */
    fun runCommon(model: CodegenModel, outputDir: File) {
        val pkgPath = model.packageName.replace('.', '/')
        val template = TemplateEngine.load("workerkmp-auto-common.kt.template")
        val rendered = TemplateEngine.render(template = template, model = model)
        outputDir
            .resolve("commonMain/kotlin/$pkgPath/generated/WorkerKmpAuto.kt")
            .apply { parentFile.mkdirs() }
            .writeText(rendered)
    }

    enum class Platform(val sourceSet: String, val templateName: String) {
        Android("androidMain", "workerkmp-auto-android.kt.template"),
        Ios("iosMain", "workerkmp-auto-ios.kt.template"),
        Desktop("desktopMain", "workerkmp-auto-desktop.kt.template"),
        Web("wasmJsMain", "workerkmp-auto-web.kt.template"),
    }

    /**
     * Emits the per-platform actual `WorkerKmpAuto.kt` for [platform]. Called from the matching
     * platform's codegen task right after `WorkerInitGenerator.run(model, platform, outputDir)`.
     */
    fun runPlatformActual(model: CodegenModel, platform: Platform, outputDir: File) {
        val pkgPath = model.packageName.replace('.', '/')
        val template = TemplateEngine.load(platform.templateName)
        val rendered = TemplateEngine.render(template = template, model = model)
        outputDir
            .resolve("${platform.sourceSet}/kotlin/$pkgPath/generated/WorkerKmpAuto.kt")
            .apply { parentFile.mkdirs() }
            .writeText(rendered)
    }
}
