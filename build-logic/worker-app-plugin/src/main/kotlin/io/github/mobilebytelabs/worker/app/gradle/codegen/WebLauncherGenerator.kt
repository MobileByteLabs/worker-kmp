package io.github.mobilebytelabs.worker.app.gradle.codegen

import io.github.mobilebytelabs.worker.app.gradle.CodegenModel
import io.github.mobilebytelabs.worker.app.gradle.TemplateEngine
import java.io.File

/**
 * Web (wasmJs) codegen — emits 2 files into `outputDir`:
 *   - `kotlin/{pkg}/generated/Generated_Main.kt`
 *   - `resources/index.html`
 *
 * Spec: 05-desktop-web-codegen.md
 */
internal object WebLauncherGenerator {

    fun run(model: CodegenModel, outputDir: File, wasmJsBundleName: String) {
        val pkgPath = model.packageName.replace('.', '/')
        val extras = mapOf("wasmJsBundleName" to wasmJsBundleName)

        outputDir.resolve("kotlin/$pkgPath/generated/Generated_Main.kt").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("web-main.kt.template"), model, extras))
        }
        outputDir.resolve("resources/index.html").apply {
            parentFile.mkdirs()
            writeText(TemplateEngine.render(TemplateEngine.load("index.html.template"), model, extras))
        }
    }
}
