package io.github.mobilebytelabs.worker.app.gradle.codegen

import io.github.mobilebytelabs.worker.app.gradle.CodegenModel
import io.github.mobilebytelabs.worker.app.gradle.TemplateEngine
import java.io.File

/**
 * Desktop codegen — emits `kotlin/{pkg}/generated/Generated_Main.kt` (the
 * `fun main()` calling `launchDesktopWorkerApp`).
 *
 * Spec: 05-desktop-web-codegen.md
 */
internal object DesktopLauncherGenerator {

    fun run(model: CodegenModel, outputDir: File) {
        val pkgPath = model.packageName.replace('.', '/')
        outputDir.resolve("kotlin/$pkgPath/generated/Generated_Main.kt").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("desktop-main.kt.template"), model))
        }
    }
}
