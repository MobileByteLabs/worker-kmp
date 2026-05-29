package io.github.mobilebytelabs.worker.app.gradle.codegen

import io.github.mobilebytelabs.worker.app.gradle.CodegenModel
import io.github.mobilebytelabs.worker.app.gradle.TemplateEngine
import java.io.File

/**
 * Android codegen — emits 3 files into `outputDir`:
 *   - `kotlin/{pkg}/generated/Generated_App.kt`
 *   - `kotlin/{pkg}/generated/Generated_MainActivity.kt`
 *   - `AndroidManifest.xml`     (merged with consumer's via AGP)
 *
 * Spec: 04-android-codegen.md
 */
internal object AndroidLauncherGenerator {

    fun run(model: CodegenModel, outputDir: File) {
        val pkgPath = model.packageName.replace('.', '/')

        outputDir.resolve("kotlin/$pkgPath/generated/Generated_App.kt").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("android-app.kt.template"), model))
        }
        outputDir.resolve("kotlin/$pkgPath/generated/Generated_MainActivity.kt").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("android-activity.kt.template"), model))
        }
        outputDir.resolve("AndroidManifest.xml").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("AndroidManifest.xml.template"), model))
        }
    }
}
